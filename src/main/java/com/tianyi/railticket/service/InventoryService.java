package com.tianyi.railticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianyi.railticket.common.BizException;
import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.common.ErrorCode;
import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.entity.TrainDO;
import com.tianyi.railticket.entity.TrainStationDO;
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
    private static final int STATUS_ONLINE = 1;
    private static final int DEFAULT_WARM_UP_DAYS = 7;

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
        return warmUp(DEFAULT_WARM_UP_DAYS);
    }

    /** 预热未来 N 天所有上架车次的库存 */
    public int warmUp(int days) {
        LocalDate today = LocalDate.now(Const.ZONE);
        int created = 0;

        // 查出所有上架的车次
        List<TrainDO> trains = trainMapper.selectList(new LambdaQueryWrapper<TrainDO>()
                .eq(TrainDO::getStatus, STATUS_ONLINE));
        if (trains.isEmpty()) {
            log.warn("没有上架的车次（status={}），预热跳过", STATUS_ONLINE);
        }

        for (TrainDO train : trains) {
            // 把 seat_config JSON 转成对象
            SeatConfig cfg;
            try {
                cfg = objectMapper.readValue(train.getSeatConfig(), SeatConfig.class);
            } catch (Exception e) {
                log.error("车次 {} 座位配置解析失败，跳过。原始内容：{}", train.getId(), train.getSeatConfig(), e);
                continue;
            }

            int segCount = querySegCount(train.getId());
            if (segCount <= 0) {
                log.warn("车次 {} 经停不足 2 站，跳过", train.getId());
                continue;
            }

            for (int d = 0; d < days; d++) {
                LocalDate date = today.plusDays(d);
                for (SeatType type : SeatType.values()) {
                    int capacity = type.capacityOf(cfg);
                    for (int seg = 1; seg <= segCount; seg++) {
                        String key = segKey(train.getId(), date, type.getCode(), seg);
                        Boolean ok = stringRedisTemplate.opsForValue()
                                .setIfAbsent(key, String.valueOf(capacity), days + 2L, TimeUnit.DAYS);
                        if (Boolean.TRUE.equals(ok)) {
                            created++;
                        }
                    }
                }
            }
        }
        log.info("库存预热完成，新建 key {} 个", created);
        return created;
    }

    /** 总区间数 = 最大 seq - 1 */
    private int querySegCount(Long trainId) {
        List<TrainStationDO> list = trainStationMapper.selectList(
                new LambdaQueryWrapper<TrainStationDO>()
                        .eq(TrainStationDO::getTrainId, trainId)
                        .orderByDesc(TrainStationDO::getSeq));
        if (list.isEmpty()) {
            return 0;
        }
        return list.get(0).getSeq() - 1;
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

    /** 回补区间库存（每 seg +1） */
    public void restore(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        for (String key : segKeys(trainId, date, seatType, fromSeq, toSeq)) {
            stringRedisTemplate.opsForValue().increment(key, 1);
        }
    }

    /** 查区间最小余票（任一 key 缺失按 0 处理） */
    public int getAvailable(Long trainId, LocalDate date, Integer seatType, int fromSeq, int toSeq) {
        List<String> keys = segKeys(trainId, date, seatType, fromSeq, toSeq);
        if (keys.isEmpty()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (String key : keys) {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                log.warn("库存 key 缺失，按无票处理：{}", key);
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
        if (fromSeq == null || toSeq == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_PASS_STATION);
        }
        if (fromSeq >= toSeq) {
            throw new BizException(ErrorCode.INVALID_DIRECTION);
        }
        return new SegRange(fromSeq, toSeq);
    }
}
