package com.tianyi.railticket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class TrainRouteDO {
    private Long trainId;
    private LocalTime depart;
    private LocalTime arrive;
    private BigDecimal price;
}
