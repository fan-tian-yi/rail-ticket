package com.tianyi.railticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianyi.railticket.common.Check;
import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.common.PriceCalculator;
import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.common.exception.BizException;
import com.tianyi.railticket.common.exception.ErrorCode;
import com.tianyi.railticket.dto.OrderCreateDTO;
import com.tianyi.railticket.dto.OrderPayDTO;
import com.tianyi.railticket.dto.OrderRefundDTO;
import com.tianyi.railticket.entity.OrderDO;
import com.tianyi.railticket.entity.PassengerDO;
import com.tianyi.railticket.entity.StockDeductionLogDO;
import com.tianyi.railticket.entity.TrainDO;
import com.tianyi.railticket.entity.model.SegRange;
import com.tianyi.railticket.entity.model.SegmentInfo;
import com.tianyi.railticket.mapper.OrderMapper;
import com.tianyi.railticket.mapper.PassengerMapper;
import com.tianyi.railticket.mapper.StockDeductionLogMapper;
import com.tianyi.railticket.mapper.TrainMapper;
import com.tianyi.railticket.mapper.TrainStationMapper;
import com.tianyi.railticket.vo.OrderCreateVO;
import com.tianyi.railticket.vo.OrderPayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** 下单服务：先扣 Redis 再落库，落库失败手动回补（Redis 不参与 MySQL 事务） */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /** MVP 固定测试用户，Step4 接 Sa-Token 后改从会话取 */
    private static final Long CURRENT_USER_ID = 3001L;

    private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    /** 流水里的 seg 列表类型：下单写入、退票/超时关单/对账读出回补，提成常量避免每处都写 TypeReference */
    private static final TypeReference<List<Integer>> SEG_LIST_TYPE = new TypeReference<>() {};

    private final InventoryService inventoryService;
    private final PassengerMapper passengerMapper;
    private final TrainStationMapper trainStationMapper;
    private final TrainMapper trainMapper;
    private final OrderMapper orderMapper;
    private final StockDeductionLogMapper stockDeductionLogMapper;
    private final ObjectMapper objectMapper;

    /** 下单主流程：校验 → 解析区间 → 算价 → 扣库存 → 落库 */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateDTO dto) {
        Long userId = CURRENT_USER_ID;

        // ① 业务校验：乘车日期 / 预售期 / 车次可售 / 乘客归属
        //    参数非空、ID 正数、席别合法已由 OrderCreateDTO 的注解在 Controller 入口拦掉
        Check.trainDate(dto.getTrainDate());
        checkTrainOnline(dto.getTrainId());
        SeatType seatType = SeatType.ofOrThrow(dto.getSeatType());
        checkPassenger(dto.getPassengerId(), userId);

        // ② 区间解析：一次 SQL 同时拿到 seg 区间、区间里程、时刻
        SegmentInfo seg = resolveSegment(dto.getTrainId(), dto.getFromStationId(), dto.getToStationId());

        // ③ 算价：按区间里程算二等座基准价（递远递减），再乘席别系数
        BigDecimal amount = seatType.calc(PriceCalculator.basePrice(seg.getDistance()));

        // ④ 扣库存：Lua 两段式保证原子性，失败时不会部分扣减
        boolean deducted = inventoryService.deduct(dto.getTrainId(),
                dto.getTrainDate(), seatType.getCode(),
                seg.getFromSeq(), seg.getToSeq());
        if (!deducted) {
            throw new BizException(ErrorCode.NO_TICKET);
        }

        // ⑤ 落库：失败必须回补 Redis 后再抛出，否则事务回滚了但库存没还
        String orderNo = generateOrderNo(dto.getTrainDate());
        LocalDateTime expireTime = LocalDateTime.now(Const.ZONE)
                .plusMinutes(Const.PAY_TIMEOUT_MINUTES);
        List<Integer> segments = segList(seg.getFromSeq(), seg.getToSeq());

        try {
            orderMapper.insert(buildOrder(dto, userId, seatType, amount, orderNo, expireTime));

            StockDeductionLogDO logDO = new StockDeductionLogDO();
            logDO.setOrderNo(orderNo);
            logDO.setTrainId(dto.getTrainId());
            logDO.setTrainDate(dto.getTrainDate());
            logDO.setSeatType(seatType.getCode());
            logDO.setSegments(objectMapper.writeValueAsString(segments));
            logDO.setDelta(-1);
            stockDeductionLogMapper.insert(logDO);
        } catch (Exception e) {
            log.error("订单落库失败，已回补库存 | orderNo={} trainId={} trainDate={}",
                    orderNo, dto.getTrainId(), dto.getTrainDate(), e);
            inventoryService.restore(dto.getTrainId(), dto.getTrainDate(),
                    seatType.getCode(), seg.getFromSeq(), seg.getToSeq());
            // 必须继续抛出，否则 @Transactional 不回滚，会留下脏订单
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }

        log.info("下单成功 | orderNo={} userId={} trainId={} trainDate={} segs={} amount={}",
                orderNo, userId, dto.getTrainId(), dto.getTrainDate(), segments, amount);

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderNo(orderNo);
        vo.setAmount(amount);
        vo.setExpireTime(expireTime);
        return vo;
    }

    /** 退票：CAS 抢订单所有权 → 从扣减流水还原区间 → 回补库存 → 记退票流水 */
    @Transactional(rollbackFor = Exception.class)
    public void refund(OrderRefundDTO dto) {
        Long userId = CURRENT_USER_ID;
        String orderNo = dto.getOrderNo();

        // ① 查订单 + 归属校验：不是本人的单查出来就是 null，统一报"不存在"以免泄露订单号是否有效
        OrderDO order = orderMapper.selectOne(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, userId));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // ② 定目标状态：待支付→用户取消，已支付→已退票，其余状态一律拒绝
        int target;
        if (order.getStatus() == Const.STATUS_WAIT_PAY) {
            target = Const.STATUS_USER_CANCEL;
        } else if (order.getStatus() == Const.STATUS_PAID) {
            target = Const.STATUS_REFUNDED;
        } else {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // ③ CAS 抢所有权：rows=0 说明并发下已被别人退掉，此时绝不能回补，否则库存凭空多一份
        int rows = orderMapper.casStatus(orderNo, order.getStatus(), target,
                LocalDateTime.now(Const.ZONE));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // ④ 回补库存：区间取自扣减流水，不重新解析车站 ID
        SegRange range = resolveDeductedRange(orderNo);
        inventoryService.restore(order.getTrainId(), order.getTrainDate(),
                order.getSeatType(), range.getFromSeq(), range.getToSeq());

        // ⑤ 记退票流水（delta=+1），对账任务靠它比对 Redis 库存与 DB 订单
        StockDeductionLogDO refundLog = new StockDeductionLogDO();
        refundLog.setOrderNo(orderNo);
        refundLog.setTrainId(order.getTrainId());
        refundLog.setTrainDate(order.getTrainDate());
        refundLog.setSeatType(order.getSeatType());
        refundLog.setSegments(segJson(range.getFromSeq(), range.getToSeq()));
        refundLog.setDelta(1);
        stockDeductionLogMapper.insert(refundLog);

        log.info("退票成功 | orderNo={} userId={} status={}→{} trainId={} trainDate={} segs=[{}, {})",
                orderNo, userId, order.getStatus(), target, order.getTrainId(), order.getTrainDate(),
                range.getFromSeq(), range.getToSeq());
    }

    /** 支付订单：CAS 抢订单所有权，仅待支付且未超时的单能付成功 */
    public OrderPayVO pay(OrderPayDTO dto) {
        Long userId = CURRENT_USER_ID;
        String orderNo = dto.getOrderNo();

        // ① 查订单 + 归属校验：条件里带上 userId，非本人的单查出来就是 null，统一报"不存在"以免泄露订单号是否有效
        OrderDO order = orderMapper.selectOne(new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, userId));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // ② 先按状态快速拒绝：重复支付、已取消、已退票的单都不该走到 CAS
        if (order.getStatus() == Const.STATUS_TIMEOUT_CANCEL) {
            throw new BizException(ErrorCode.ORDER_EXPIRED);
        }
        if (order.getStatus() != Const.STATUS_WAIT_PAY) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // ③ CAS 支付：status=0 防重复支付，expire_time > now 防超时支付
        LocalDateTime payTime = LocalDateTime.now(Const.ZONE);
        int rows = orderMapper.casPay(orderNo, payTime);
        if (rows == 0) {
            // ④ rows=0 有两种可能：状态已被改（退票/关单/并发支付），或仍待支付但已过有效期。
            if (order.getExpireTime() != null && order.getExpireTime().isAfter(payTime)) {
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
            }
            throw new BizException(ErrorCode.ORDER_EXPIRED);
        }

        log.info("支付成功 | orderNo={} userId={} amount={}", orderNo, userId, order.getAmount());

        OrderPayVO vo = new OrderPayVO();
        vo.setOrderNo(orderNo);
        vo.setStatus(Const.STATUS_PAID);
        vo.setAmount(order.getAmount());
        vo.setPayTime(payTime);
        return vo;
    }

    /** 扫描超时未支付订单：逐条 CAS 抢占后回补库存，返回成功关闭的笔数 */
    public int closeTimeoutOrders(int limit) {
        List<OrderDO> timeoutOrders = orderMapper.selectTimeoutOrders(LocalDateTime.now(Const.ZONE), limit);
        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        int closed = 0;
        for (OrderDO order : timeoutOrders) {
            try {
                if (closeOne(order)) {
                    closed++;
                }
            } catch (Exception e) {
                // 单条失败不中断整批；它的 status 仍是 0，下个周期还会被扫到
                log.error("超时关单失败 | orderNo={}", order.getOrderNo(), e);
            }
        }
        return closed;
    }

    /* ==================== 私有校验 ==================== */

    /** 车次可售：存在且已上架（软删除由 @TableLogic 在 selectById 自动过滤） */
    private void checkTrainOnline(Long trainId) {
        TrainDO train = trainMapper.selectById(trainId);
        if (train == null || !Integer.valueOf(Const.TRAIN_STATUS_ONLINE).equals(train.getStatus())) {
            throw new BizException(ErrorCode.TRAIN_NOT_FOUND);
        }
    }

    /** 乘客归属：存在且属于当前用户（防越权） */
    private void checkPassenger(Long passengerId, Long userId) {
        PassengerDO passenger = passengerMapper.selectOne(new LambdaQueryWrapper<PassengerDO>()
                .eq(PassengerDO::getId, passengerId)
                .eq(PassengerDO::getUserId, userId));
        if (passenger == null) {
            throw new BizException(ErrorCode.PASSENGER_NOT_MATCH);
        }
    }

    /** 车站 ID → seg 区间，校验经停与方向 */
    private SegmentInfo resolveSegment(Long trainId, Long fromStationId, Long toStationId) {
        SegmentInfo seg = trainStationMapper.selectSegment(trainId, fromStationId, toStationId);
        if (seg == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_PASS_STATION);
        }
        Check.segment(seg.getFromSeq(), seg.getToSeq());
        return seg;
    }

    /* ==================== 私有工具 ==================== */

    /** 关闭单张超时订单：CAS 抢占 → 回补库存 → 记流水；抢不到返回 false（已被支付或已退票） */
    private boolean closeOne(OrderDO order) {
        String orderNo = order.getOrderNo();

        // CAS：仍是待支付才能关。rows=0 说明用户恰好付款或退票，此时绝不能回补，否则库存凭空多一份
        int rows = orderMapper.casStatus(orderNo, Const.STATUS_WAIT_PAY,
                Const.STATUS_TIMEOUT_CANCEL, LocalDateTime.now(Const.ZONE));
        if (rows == 0) {
            return false;
        }

        SegRange range = resolveDeductedRange(orderNo);
        inventoryService.restore(order.getTrainId(), order.getTrainDate(),
                order.getSeatType(), range.getFromSeq(), range.getToSeq());

        StockDeductionLogDO closeLog = new StockDeductionLogDO();
        closeLog.setOrderNo(orderNo);
        closeLog.setTrainId(order.getTrainId());
        closeLog.setTrainDate(order.getTrainDate());
        closeLog.setSeatType(order.getSeatType());
        closeLog.setSegments(segJson(range.getFromSeq(), range.getToSeq()));
        closeLog.setDelta(1);
        stockDeductionLogMapper.insert(closeLog);

        log.info("超时关单 | orderNo={} trainId={} trainDate={} segs=[{}, {})",
                orderNo, order.getTrainId(), order.getTrainDate(),
                range.getFromSeq(), range.getToSeq());
        return true;
    }

    /** 从扣减流水还原要回补的区间（扣多少还多少；toSeq 左闭右开故 +1）—— 退票与超时关单共用 */
    private SegRange resolveDeductedRange(String orderNo) {
        StockDeductionLogDO deductLog = stockDeductionLogMapper.selectOne(
                new LambdaQueryWrapper<StockDeductionLogDO>()
                        .eq(StockDeductionLogDO::getOrderNo, orderNo)
                        .eq(StockDeductionLogDO::getDelta, -1)
                        .last("LIMIT 1"));
        if (deductLog == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        List<Integer> segs;
        try {
            segs = objectMapper.readValue(deductLog.getSegments(), SEG_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        if (segs.isEmpty()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        return new SegRange(segs.getFirst(), segs.getLast() + 1);
    }

    /** seg 区间 → 流水里的 JSON 数组（与下单写入的格式保持一致） */
    private String segJson(int fromSeq, int toSeq) {
        try {
            return objectMapper.writeValueAsString(segList(fromSeq, toSeq));
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /** 组装订单（票价/席别/日期在下单时冻结成快照） */
    private OrderDO buildOrder(OrderCreateDTO dto, Long userId, SeatType seatType,
                               BigDecimal amount, String orderNo, LocalDateTime expireTime) {
        OrderDO order = new OrderDO();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPassengerId(dto.getPassengerId());
        order.setTrainId(dto.getTrainId());
        order.setTrainDate(dto.getTrainDate());
        order.setFromStationId(dto.getFromStationId());
        order.setToStationId(dto.getToStationId());
        order.setSeatType(seatType.getCode());
        order.setAmount(amount);
        order.setStatus(Const.STATUS_WAIT_PAY);
        order.setExpireTime(expireTime);
        return order;
    }

    /** 订单号：RT + 乘车日期 + 雪花 ID（不可枚举，防 IDOR） */
    private String generateOrderNo(LocalDate trainDate) {
        return "RT" + trainDate.format(ORDER_NO_DATE_FMT) + IdWorker.getId();
    }

    /** 覆盖的 seg 列表，左闭右开 [fromSeq, toSeq) */
    private List<Integer> segList(int fromSeq, int toSeq) {
        List<Integer> list = new ArrayList<>(Math.max(toSeq - fromSeq, 0));
        for (int s = fromSeq; s < toSeq; s++) {
            list.add(s);
        }
        return list;
    }
}
