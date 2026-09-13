package com.tianyi.railticket.entity.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class TrainRoute {
    private Long trainId;
    private LocalTime depart;
    private LocalTime arrive;

    /** 本次行程里程，票价由 basePrice(里程) × 席别倍率 现算 */
    private BigDecimal distance;
}
