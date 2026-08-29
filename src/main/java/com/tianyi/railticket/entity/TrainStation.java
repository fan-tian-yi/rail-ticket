package com.tianyi.railticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@TableName("t_train_station")
public class TrainStation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private Long stationId;
    private Integer seq;
    private LocalTime arriveTime;
    private LocalTime departTime;
    private BigDecimal priceCum;
}
