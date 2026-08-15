package com.cornerstone.auth.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置：为所有 Feign 调用自动附加服务间内部令牌头（X-Internal-Token）。
 *
 * <p>auth 经 Feign 调 system 的 /system/auth/** 内部接口时携带共享令牌，system 的 {@code InternalTokenFilter}
 * 校验之——解决服务间调用的匿名问题。
 */
@Configuration
public class FeignInternalTokenConfig {

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor(
            @Value("${cornerstone.internal-token:}") String internalToken) {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
