package com.tianyi.railticket.common;

import java.time.ZoneId;

public final class Const {

    private Const() {
    }

    /** 业务时区（中国标准时间） */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 预售期天数：与库存预热天数保持一致，超出则 Redis 里没有 key（会假性显示无票） */
    public static final int PRESALE_DAYS = 7;

    /** 支付超时分钟数：下单后超过该时间未支付则自动关单并回补库存 */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    // ===== 订单状态（t_order.status）=====
    public static final int STATUS_WAIT_PAY = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_TIMEOUT_CANCEL = 3;
    public static final int STATUS_USER_CANCEL = 4;
    public static final int STATUS_REFUNDED = 5;
}
