package com.tianyi.railticket.runner;

import com.tianyi.railticket.common.Const;
import com.tianyi.railticket.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class InventoryWarmUpRunner implements ApplicationRunner {
    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args){
        try{
            int n = inventoryService.warmUp();
            log.info("启动预热完成，新增 {} 个 key（已存在的跳过）", n);
        } catch (Exception e){
            log.error("库存预热失败，服务继续启动（下单将返回无票）", e);
        }
    }

    /** 每日补预热：覆盖「服务长跑后新日期无库存」的假售罄 */
    @Scheduled(cron = "0 0 0 * * ?", zone = Const.ZONE_ID)
    public void scheduledWarmUp(){
        try {
            int n = inventoryService.warmUp();
            log.info("定时补预热完成，新增 {} 个 key（已存在的跳过）", n);
        } catch (Exception e) {
            log.error("定时预热失败，等待下个周期重试", e);
        }
    }
}
