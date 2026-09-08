package com.tianyi.railticket;

import com.tianyi.railticket.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

@SpringBootTest
@Slf4j
public class InventoryWarmUpTest {
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    // 执行WarmUp方法，预热所有列车未来7天座位数量
    void testWarmUp() {
        deleteWarmUp();
        // 预热
        int created = inventoryService.warmUp();
        log.info("预热，新建 key = {}", created);
    }

    @Test
    // 删除Redis中预热的列车数据
    void deleteWarmUp() {
        Set<String> old = stringRedisTemplate.keys("rt:stock:*");
        if(old != null && !old.isEmpty()){
            stringRedisTemplate.delete(old);
            log.info("清理旧 key {} 个", old.size());
        }
    }
}
