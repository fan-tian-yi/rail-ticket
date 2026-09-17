package com.tianyi.railticket.runner;

import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 超时未支付订单扫描器：每分钟一次，关单并回补库存 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutRunner {

    /** 单次处理上限：积压时不至于一次拉爆数据库，剩下的下个周期继续 */
    private static final int BATCH_LIMIT = 200;

    private final OrderService orderService;

    /** 每分钟第 0 秒扫一次；关单靠 CAS 抢所有权，故多实例同时跑也安全 */
    @Scheduled(cron = "0 * * * * ?", zone = Const.ZONE_ID)
    public void scanTimeoutOrders() {
        try {
            int closed = orderService.closeTimeoutOrders(BATCH_LIMIT);
            // 绝大多数周期是 0 笔，不打日志，免得每分钟一行噪音淹掉有用信息
            if (closed > 0) {
                log.info("超时关单完成 | closed={}", closed);
            }
        } catch (Exception e) {
            // 兜底：整批异常也不让调度线程挂掉，下个周期自然重试
            log.error("超时关单任务异常，等待下个周期重试", e);
        }
    }
}
