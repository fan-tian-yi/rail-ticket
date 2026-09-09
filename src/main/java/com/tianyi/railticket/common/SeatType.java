package com.tianyi.railticket.common;

import com.tianyi.railticket.common.validation.CodeEnum;
import com.tianyi.railticket.entity.model.SeatConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public enum SeatType implements CodeEnum {
    SECOND(1, "二等座", new BigDecimal("1.00")),
    FIRST(2, "一等座", new BigDecimal("1.69")),
    BUSINESS(3, "商务座", new BigDecimal("3.16"));

    private final Integer code;
    private final String desc;
    private final BigDecimal rate;

    /** 按 code 找席别，找不到返回 null */
    public static SeatType of(Integer code) {
        for (SeatType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }

    /** 按 code 找席别，找不到抛 40007 */
    public static SeatType ofOrThrow(Integer code) {
        SeatType seatType = of(code);
        if (seatType == null) {
            throw new BizException(ErrorCode.SEAT_TYPE_INVALID);
        }
        return seatType;
    }

    /** 基准价 × 本席别倍率（保留 2 位，四舍五入） */
    public BigDecimal calc(BigDecimal basePrice) {
        return basePrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** 该席别在车次配置里的定员数 */
    public int capacityOf(SeatConfig cfg) {
        return switch (this) {
            case SECOND -> cfg.getSecond();
            case FIRST -> cfg.getFirst();
            case BUSINESS -> cfg.getBusiness();
        };
    }
}
