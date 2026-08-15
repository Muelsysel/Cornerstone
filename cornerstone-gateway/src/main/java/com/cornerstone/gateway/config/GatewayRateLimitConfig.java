package com.cornerstone.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置：基于 Redis 的令牌桶（RequestRateLimiter）。
 *
 * <p>v1 预留的基础限流落地：按客户端 IP 限流，默认 10 请求/秒、突发 20（可按路由覆盖）。 反臃肿：仅提供一个默认限流器与 IP KeyResolver，各路由按需启用。
 */
@Configuration
public class GatewayRateLimitConfig {

    /** 默认限流器：每秒补充 10 个令牌，桶容量 20 */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /** 登录接口限流器：每秒 5 个令牌、桶 10（比默认更严，缓解账号密码定向爆破） */
    @Bean
    public RedisRateLimiter loginRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }

    /** 按客户端 IP 限流（IPv4/IPv6 原文） */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip =
                    exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown";
            return Mono.just(ip);
        };
    }
}
