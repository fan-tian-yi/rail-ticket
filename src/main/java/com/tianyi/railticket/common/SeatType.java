package com.tianyi.railticket.common;

import com.tianyi.railticket.entity.model.SeatConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeatType {
    SECOND(1, "二等座"),
    FIRST(2, "一等座"),
    BUSINESS(3, "商务座");

    private final Integer code;
    private final String desc;

    public static SeatType of(Integer code){
        for(SeatType t : values())
            if (t.code.equals(code))
                return t;
        return null;
    }

    public int capacityOf(SeatConfig cfg){
        return switch(this){
            case SECOND   -> cfg.getSecond();
            case FIRST    -> cfg.getFirst();
            case BUSINESS -> cfg.getBusiness();
        };
    }
}
