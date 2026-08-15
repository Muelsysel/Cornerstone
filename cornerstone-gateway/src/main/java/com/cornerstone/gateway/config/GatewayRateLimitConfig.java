package com.cornerstone.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置：基于 Redis 的令牌桶（RequestRateLimiter）。
 *
 * <p>按客户端 IP 限流：默认 10 请求/秒、突发 20；登录接口独立更严（5/s、突发 10，防账号爆破）。
 *
 * <p>注意：必须用「模板 + 脚本 + ConfigurationService」构造 {@link RedisRateLimiter}—— 曾误用 {@code new
 * RedisRateLimiter(10, 20)}（无脚本/模板），导致限流器空转（请求全放行、Redis 无 request_rate_limiter key）。脚本 bean {@code
 * redisRequestRateLimiterScript} 由 {@code GatewayRedisAutoConfiguration} 提供。
 */
@Configuration
public class GatewayRateLimitConfig {

    /**
     * 默认限流器：速率按路由 id 预置（redisRateLimiter 服务 auth/system/demo → 10/s + 突发 20）。
     *
     * <p>@Primary：RequestRateLimiterGatewayFilterFactory 按类型注入单个 RateLimiter，
     * 须有默认主选（路由内再用 @Qualifier 显式指定各限流器）。
     */
    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter(
            ReactiveStringRedisTemplate template,
            RedisScript<java.util.List<Long>> script,
            ConfigurationService configurationService) {
        RedisRateLimiter limiter = new RedisRateLimiter(template, script, configurationService);
        // RedisRateLimiter 按路由 id 从 getConfig() 加载速率（StatefulConfigurable 契约）
        // default 兜底：路由 id 未显式预置时也生效（防漏配导致限流静默失效）
        limiter.getConfig().put("default", config(10, 20));
        for (String route : new String[] {"auth", "system", "demo"}) {
            limiter.getConfig().put(route, config(10, 20));
        }
        return limiter;
    }

    /** 登录接口限流器：auth-login 路由 → 5/s + 突发 10（更严防账号爆破）。 */
    @Bean
    public RedisRateLimiter loginRateLimiter(
            ReactiveStringRedisTemplate template,
            RedisScript<java.util.List<Long>> script,
            ConfigurationService configurationService) {
        RedisRateLimiter limiter = new RedisRateLimiter(template, script, configurationService);
        limiter.getConfig().put("auth-login", config(5, 10));
        return limiter;
    }

    private RedisRateLimiter.Config config(int replenishRate, int burstCapacity) {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config();
        config.setReplenishRate(replenishRate);
        config.setBurstCapacity(burstCapacity);
        return config;
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
