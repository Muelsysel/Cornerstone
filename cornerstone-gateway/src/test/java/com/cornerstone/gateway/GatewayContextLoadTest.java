package com.cornerstone.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/** 网关上下文加载测试：验证 WebFlux 环境下 common 自动配置与 JWT 解码器装配正常。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class GatewayContextLoadTest {

    @Autowired private ReactiveJwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        assertThat(jwtDecoder).isNotNull();
    }
}
