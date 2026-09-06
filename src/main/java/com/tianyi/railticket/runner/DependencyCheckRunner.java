package com.tianyi.railticket.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

@Slf4j
@Component
@Order(1) // 在预热之前
@RequiredArgsConstructor
public class DependencyCheckRunner implements ApplicationRunner {
    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args){
        checkMysql();
        checkRedis();
    }

    private void checkMysql(){
        long start = System.currentTimeMillis();
        try(Connection conn = dataSource.getConnection()){
            if(!conn.isValid(2)){ // 2秒没响应就当无效
                throw new IllegalStateException("MySQL连接无效");
            }
            log.info("MySQL自检通过 | 版本={} | 库={} | 耗时={}ms",
                    conn.getMetaData().getDatabaseProductVersion(),
                    conn.getCatalog(),
                    System.currentTimeMillis() - start);
        } catch(Exception e){
            throw new IllegalStateException("MySQL 连接失败，应用终止启动", e);
        }
    }

    private void checkRedis() {
        try {
            String pong = stringRedisTemplate.execute(
                    (RedisCallback<String>) RedisConnection::ping);
            Properties info = stringRedisTemplate.execute(
                    (RedisCallback<Properties>) conn -> conn.serverCommands().info("server"));
            Long keyCount = stringRedisTemplate.execute(
                    (RedisCallback<Long>) RedisConnection::dbSize);

            log.info("Redis 自检通过 | ping={} | 版本={} | 当前 key 数={}",
                    pong,
                    info == null ? "unknown" : info.getProperty("redis_version"),
                    keyCount);
        } catch (Exception e) {
            // Redis 是缓存层：挂了只是库存不可用，站站查询还能服务
            log.error("Redis 连接失败，余票查询与下单将不可用", e);
        }
    }
}
