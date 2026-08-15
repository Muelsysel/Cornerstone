package com.cornerstone.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/** 网关上下文加载测试：验证 WebFlux 环境下 common 自动配置与 JWT 解码器装配正常。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class GatewayContextLoadTest {

    @Autowired private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    @Qualifier("redisRateLimiter")
    private RedisRateLimiter redisRateLimiter;

    @Autowired
    @Qualifier("loginRateLimiter")
    private RedisRateLimiter loginRateLimiter;

    @Test
    void contextLoads() {
        assertThat(jwtDecoder).isNotNull();
    }

    @Test
    void rateLimiterParamsMatchDoc() throws Exception {
        // 限流数值是防爆破契约（文档记录）：默认 10/s + 突发 20，登录更严 5/s + 突发 10
        RedisRateLimiter.Config def = defaultConfig(redisRateLimiter);
        RedisRateLimiter.Config login = defaultConfig(loginRateLimiter);
        assertThat(def.getReplenishRate()).isEqualTo(10);
        assertThat(def.getBurstCapacity()).isEqualTo(20);
        assertThat(login.getReplenishRate()).isEqualTo(5);
        assertThat(login.getBurstCapacity()).isEqualTo(10);
    }

    /** 反射读取 RedisRateLimiter 私有 defaultConfig（构造参数即文档契约的限流数值） */
    private RedisRateLimiter.Config defaultConfig(RedisRateLimiter limiter) throws Exception {
        java.lang.reflect.Field field =
                RedisRateLimiter.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        return (RedisRateLimiter.Config) field.get(limiter);
    }
}
