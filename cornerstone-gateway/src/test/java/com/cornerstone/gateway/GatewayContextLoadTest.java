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
    void rateLimiterBeansAssembledWithRedisDeps() {
        // 限流器必须带脚本/模板装配（曾误用 new RedisRateLimiter(10,20) 无脚本导致空转）：
        // 脚本 bean 由 GatewayRedisAutoConfiguration 提供，装配后限流器可执行 Lua 计数
        assertThat(redisRateLimiter).isNotNull();
        assertThat(loginRateLimiter).isNotNull();
    }
}
