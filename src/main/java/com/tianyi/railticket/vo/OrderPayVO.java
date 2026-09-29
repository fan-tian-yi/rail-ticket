package com.tianyi.railticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付结果：状态与支付时间以服务端为准，前端不自己编 */
@Data
public class OrderPayVO {
    private String orderNo;
    private Integer status;
    private BigDecimal amount;
    private LocalDateTime payTime;
}
