package com.tianyi.railticket.entity.model;

import lombok.Data;

@Data
public class SeatConfig {
    private Integer first; //一等座容量
    private Integer second;  //二等座容量
    private Integer business; //商务座容量
}
