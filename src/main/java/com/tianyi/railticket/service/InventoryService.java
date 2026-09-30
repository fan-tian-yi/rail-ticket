package com.tianyi.railticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianyi.railticket.common.exception.BizException;
import com.tianyi.railticket.common.Check;
import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.common.exception.ErrorCode;
import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.entity.TrainDO;
import com.tianyi.railticket.entity.model.SeatConfig;
import com.tianyi.railticket.entity.model.SegRange;
import com.tianyi.railticket.mapper.TrainMapper;
import com.tianyi.railticket.mapper.TrainStationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String KEY_PREFIX = "rt:stock";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate stringRedisTemplate;
    private final TrainMapper trainMapper;
    private final TrainStationMapper trainStationMapper;
    private final ObjectMapper objectMapper;

    /** 拼单个 seg 库存 key */
    private String segKey(Long trainId, LocalDate date, Integer seatType, int seg) {
        return KEY_PREFIX + ":" + trainId + ":" + date.format(DATE_FMT) + ":" + seatType + ":seg:" + seg;
    }

    /** 拼区间内所有 seg 的 key */
    public List<String> segKeys(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        List<String> keys = new ArrayList<>();
        for (int seg = fromSeq; seg < toSeq; seg++) {
            keys.add(segKey(trainId, date, seatType, seg));
        }
        return keys;
    }

    /** 启动预热（默认 7 天） */
    public int warmUp() {
        return warmUp(Const.PRESALE_DAYS);
    }

    /** 预热未来 N 天所有上架车次的库存 */
    public int warmUp(int days) {
        LocalDate today = LocalDate.now(Const.ZONE);
        int created = 0;

        // 1. 查出所有上架车次（下架车次不预热，不占 Redis）
        List<TrainDO> trains = trainMapper.selectList(new LambdaQueryWrapper<TrainDO>()
                .eq(TrainDO::getStatus, Const.TRAIN_STATUS_ONLINE));
        if (trains.isEmpty()) {
            LocalDate last = today.plusDays(days - 1);
            log.warn("t_train 无上架车次，跳过预热 | 窗口={} ~ {} | 影响：全部车次库存 key 未生成，查票将返回无票",
                    today, last);
            return 0;
        }

        // 2. 逐车次预热
        for (TrainDO train : trains) {
            // 2.1 解析 seat_config JSON（脏数据跳过该车次，不影响其他车次）
            SeatConfig cfg;
            try {
                cfg = objectMapper.readValue(train.getSeatConfig(), SeatConfig.class);
            } catch (Exception e) {
                log.error("车次 {} 座位配置解析失败，跳过该车次（本次无票可售）| seatConfig={}",
                        train.getTrainNo(), train.getSeatConfig(), e);
                continue;
            }

            // 2.2 算区间数：站数不足 2 则没有可卖区间，跳过
            int segCount = querySegCount(train.getId());
            if (segCount <= 0) {
                log.warn("车次 {} 经停不足 2 站，无法划区间，跳过该车次（本次无票可售）| segCount={}",
                        train.getTrainNo(), segCount);
                continue;
            }

            // 2.3 逐日 × 逐席别 × 逐区间建 key：d=0 是今天，窗口 [今天, 今天+days-1]
            for (int d = 0; d < days; d++) {
                LocalDate date = today.plusDays(d);
                for (SeatType type : SeatType.values()) {
                    int capacity = type.capacityOf(cfg);
                    for (int seg = 1; seg <= segCount; seg++) {
                        String key = segKey(train.getId(), date, type.getCode(), seg);
                        // setIfAbsent：已存在则跳过，不覆盖已扣减的库存；created 只统计真新建的
                        Boolean ok = stringRedisTemplate.opsForValue()
                                .setIfAbsent(key, String.valueOf(capacity), days + 2L, TimeUnit.DAYS);
                        if (Boolean.TRUE.equals(ok)) {
                            created++;
                        }
                    }
                }
            }
        }
        return created;
    }

    /** 总区间数 = 最大 seq - 1（少于 2 站时为 0）；取 MAX 而非 COUNT，断号时上界仍覆盖全部 seg 编号 */
    private int querySegCount(Long trainId) {
        Integer maxSeq = trainStationMapper.selectMaxSeq(trainId);
        return maxSeq == null ? 0 : maxSeq - 1;
    }

    /** 车次可售：存在且已上架（软删除由 @TableLogic 在 selectById 自动过滤） */
    private void checkTrainOnline(Long trainId) {
        TrainDO train = trainMapper.selectById(trainId);
        if (train == null || !Integer.valueOf(Const.TRAIN_STATUS_ONLINE).equals(train.getStatus())) {
            throw new BizException(ErrorCode.TRAIN_NOT_FOUND);
        }
    }

    /** 扣减 Lua：先检查全部 seg 够不够，够才逐段 DECRBY，任一不足返回 0 */
    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
                local n = tonumber(redis.call('GET', KEYS[i]) or '0')
                if n < tonumber(ARGV[1]) then
                    return 0
                end
            end
            for i = 1, #KEYS do
                redis.call('DECRBY', KEYS[i], ARGV[1])
            end
            return 1
            """, Long.class);

    /** Lua 原子扣减，成功返回 true */
    public boolean deduct(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        List<String> keys = segKeys(trainId, date, seatType, fromSeq, toSeq);
        if (keys.isEmpty()) {
            return false;
        }
        Long result = stringRedisTemplate.execute(DEDUCT_SCRIPT, keys, "1");
        return Long.valueOf(1L).equals(result);
    }

    /** 回补 Lua：逐段 INCRBY（无条件 +1，没有检查步骤，故不需要两段式） */
    private static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
                redis.call('INCRBY', KEYS[i], ARGV[1])
            end
            return 1
            """, Long.class);

    /** 回补区间库存（每 seg +1）：一条 Lua 完成，1 次往返且不会出现部分回补 */
    public void restore(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        List<String> keys = segKeys(trainId, date, seatType, fromSeq, toSeq);
        if (keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.execute(RESTORE_SCRIPT, keys, "1");
    }

    /** 查区间最小余票（任一 key 缺失按 0 处理） */
    public int getAvailable(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        Check.trainDate(date);
        checkTrainOnline(trainId);

        List<String> keys = segKeys(trainId, date, seatType, fromSeq, toSeq);
        if (keys.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (String key : keys) {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("库存 key 缺失，按无票处理 | key={}", key);
                return 0;
            }
            min = Math.min(min, Integer.parseInt(value));
        }
        return min;
    }

    /** 车站 ID → seg 区间（余票查询用） */
    public SegRange resolveRange(Long trainId, Long fromStationId, Long toStationId) {
        Integer fromSeq = trainStationMapper.selectSeq(trainId, fromStationId);
        Integer toSeq = trainStationMapper.selectSeq(trainId, toStationId);
        Check.segment(fromSeq, toSeq);
        return new SegRange(fromSeq, toSeq);
    }
}
