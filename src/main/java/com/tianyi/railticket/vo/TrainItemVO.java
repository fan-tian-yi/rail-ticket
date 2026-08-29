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
    private BigDecimal price;
    private String seatConfig;
    private Integer available;
}
