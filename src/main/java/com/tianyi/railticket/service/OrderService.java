package com.tianyi.railticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianyi.railticket.common.BizException;
import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.common.ErrorCode;
import com.tianyi.railticket.common.Check;
import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.dto.OrderCreateDTO;
import com.tianyi.railticket.entity.OrderDO;
import com.tianyi.railticket.entity.PassengerDO;
import com.tianyi.railticket.entity.StockDeductionLogDO;
import com.tianyi.railticket.entity.model.SegmentInfo;
import com.tianyi.railticket.mapper.OrderMapper;
import com.tianyi.railticket.mapper.PassengerMapper;
import com.tianyi.railticket.mapper.StockDeductionLogMapper;
import com.tianyi.railticket.mapper.TrainStationMapper;
import com.tianyi.railticket.vo.OrderCreateVO;
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

    private final InventoryService inventoryService;
    private final PassengerMapper passengerMapper;
    private final TrainStationMapper trainStationMapper;
    private final OrderMapper orderMapper;
    private final StockDeductionLogMapper stockDeductionLogMapper;
    private final ObjectMapper objectMapper;

    /** 下单主流程：校验 → 解析区间 → 算价 → 扣库存 → 落库 */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateDTO dto) {
        Long userId = CURRENT_USER_ID;

        // ① 业务校验：乘车日期 / 预售期 / 乘客归属
        //    参数非空、ID 正数、席别合法已由 OrderCreateDTO 的注解在 Controller 入口拦掉
        Check.trainDate(dto.getTrainDate());
        SeatType seatType = SeatType.ofOrThrow(dto.getSeatType());
        checkPassenger(dto.getPassengerId(), userId);

        // ② 区间解析：一次 SQL 同时拿到 seg 区间、票价、时刻
        SegmentInfo seg = resolveSegment(dto.getTrainId(), dto.getFromStationId(), dto.getToStationId());

        // ③ 算价：二等座基准价 × 席别系数
        BigDecimal amount = seatType.calc(seg.getPrice());

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

    /* ==================== 私有校验 ==================== */

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
