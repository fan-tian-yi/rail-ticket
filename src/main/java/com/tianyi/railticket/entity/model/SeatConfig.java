package com.tianyi.railticket.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** 席别容量配置：对应 t_train.seat_config 的 JSON 列；放宽未知字段，避免上游加字段导致该车次整条跳过 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatConfig {
    private Integer first;    // 一等座容量
    private Integer second;   // 二等座容量
    private Integer business; // 商务座容量
}
