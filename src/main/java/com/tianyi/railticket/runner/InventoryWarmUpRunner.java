package com.tianyi.railticket.runner;

import com.tianyi.railticket.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class InventoryWarmUpRunner implements ApplicationRunner {
    private final InventoryService inventoryService;

    @Value("${rail.inventory.warm-up-days}")
    private int days;

    @Override
    public void run(ApplicationArguments args){
        try{
            int n = inventoryService.warmUp(days);
            log.info("库存预热完成，新建 {} 个 key，天数={}", n, days);
        } catch (Exception e){
            log.error("库存预热失败，服务继续启动（下单将返回无票）", e);
        }
    }
}
