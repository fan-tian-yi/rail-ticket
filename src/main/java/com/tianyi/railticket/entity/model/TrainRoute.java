package com.tianyi.railticket.entity.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class TrainRoute {
    private Long trainId;
    private LocalTime depart;
    private LocalTime arrive;
    private BigDecimal price;
}
