package com.tianyi.railticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisConnectTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisConnection() {
        stringRedisTemplate.opsForValue().set("test", "success");
        String value = stringRedisTemplate.opsForValue().get("test");
        System.out.println(value);
    }

    @Test
    // 删除Redis中当前库所有数据
    public void testClearRedis(){
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
        System.out.println("已删除当前库");
    }
}
