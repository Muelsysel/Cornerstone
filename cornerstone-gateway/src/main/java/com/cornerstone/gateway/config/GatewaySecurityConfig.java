package com.cornerstone.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关安全链：全部放行 + 禁用 CSRF。
 *
 * <p>网关的认证由 {@code TokenAuthGlobalFilter}（手动 JWT 校验 + 白名单）负责，不使用 Spring Security 的 resource server
 * 过滤器链。本 bean 同时阻止 Boot 的 {@code ReactiveOAuth2ResourceServerAutoConfiguration} 因检测到 {@code
 * ReactiveJwtDecoder} 而自动创建"全部请求需认证 + CSRF 开启"的默认链（否则会拦截所有经网关的请求）。
 */
@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
