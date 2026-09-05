package com.tianyi.railticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreateVO {
    private String orderNo;
    private BigDecimal amount;
    private LocalDateTime expireTime;
}
