package com.tianyi.railticket.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册 Sa-Token 拦截器：默认全部接口要求登录，白名单放行游客可访问的 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录接口本身（否则要求登录才能登录 → 死锁）
                        "/api/auth/login",
                        // 游客可访问：查车次、查余票（对标 12306 不登录也能搜票）
                        "/api/trains",
                        "/api/inventory/available",
                        // 静态资源与 API 文档
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        // 必须放行：Spring Boot 的错误转发页
                        "/error"
                );
    }
}
