package com.tianyi.railticket.common;

import java.time.ZoneId;

public final class Const {

    private Const() {
    }

    /** 业务时区 ID：注解属性（如 @Scheduled.zone）只能吃编译期常量，故保留 String 形态 */
    public static final String ZONE_ID = "Asia/Shanghai";

    /** 业务时区：供 LocalDate.now() / LocalDateTime.now() 等运行时 API 使用 */
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    /** 预售期天数：与库存预热天数保持一致，超出则 Redis 里没有 key（会假性显示无票） */
    public static final int PRESALE_DAYS = 7;

    /** 支付超时分钟数：用户可见的支付窗口（软时间线），写进 t_order.expire_time，支付时用它判超时 */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    /** 关单宽限分钟数：到期后再等这么久才关单回补（硬时间线），用来吸收支付回调延迟；只放宽关单，不放宽支付 */
    public static final int CLOSE_GRACE_MINUTES = 5;

    /** 停止售票窗口：距发车不足该分钟数即停售（对标 12306；含已发车）。查询过滤与下单校验共用 */
    public static final int STOP_SELL_BEFORE_MINUTES = 30;

    // ===== 车次状态（t_train.status）=====
    /** 只有上架车次才预热库存、才可查可下单 */
    public static final int TRAIN_STATUS_DRAFT = 0;
    public static final int TRAIN_STATUS_ONLINE = 1;
    public static final int TRAIN_STATUS_OFFLINE = 2;

    // ===== 订单状态（t_order.status）=====
    public static final int STATUS_WAIT_PAY = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_TIMEOUT_CANCEL = 3;
    public static final int STATUS_USER_CANCEL = 4;
    public static final int STATUS_REFUNDED = 5;
}
