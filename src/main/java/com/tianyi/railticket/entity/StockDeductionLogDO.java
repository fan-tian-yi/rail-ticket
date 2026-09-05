package com.tianyi.railticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_stock_deduction_log")
public class StockDeductionLogDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long trainId;
    private LocalDate trainDate;
    private Integer seatType;
    private String segments;
    private Integer delta;
    private LocalDateTime createTime;
}
