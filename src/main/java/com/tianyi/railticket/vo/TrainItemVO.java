package com.tianyi.railticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TrainItemVO {
    private Long trainId;
    private String trainNo;
    private Integer trainType;
    private String fromStationName;
    private String toStationName;
    private LocalDateTime departTime;
    private LocalDateTime arriveTime;

    /** 二等座票价（列表主显） */
    private BigDecimal secondPrice;

    /** 一等座票价 */
    private BigDecimal firstPrice;

    /** 商务座票价 */
    private BigDecimal businessPrice;

    private String seatConfig;
    private Integer available;
}
