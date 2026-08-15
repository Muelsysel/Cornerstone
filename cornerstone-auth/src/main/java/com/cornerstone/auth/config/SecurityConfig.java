package com.cornerstone.auth.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 认证中心安全配置： 1. 默认 OAuth2 授权服务器过滤链（/oauth2/** 端点）； 2. JwtEncoder 用 JWK 源签发 RS256 令牌； 3. 令牌自定义：注入
 * client_id 与 scope 额外声明（网关据此透传上下文头）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 授权服务器安全链：令牌/授权/JWKS 端点走默认契约 */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        // client_credentials 的令牌端点由客户端凭证认证，不受浏览器 CSRF 威胁；v2 授权码流程启用时按 OAuth2 规范细化
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }

    /** JwtEncoder：用 JWK 源签发 RS256 JWT */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /** 令牌自定义：client_credentials 下把 client 主体与 scope 注入为声明。 网关从这些声明透传用户上下文头。 */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (context.getTokenType() != OAuth2TokenType.ACCESS_TOKEN) {
                return;
            }
            // client_id 为授权主体
            context.getClaims().claim("client_id", context.getPrincipal().getName());
            // scope：OAuth2ClientAuthenticationToken 的已授权范围
            if (context.getAuthorizedScopes() != null && !context.getAuthorizedScopes().isEmpty()) {
                context.getClaims().claim("scope", context.getAuthorizedScopes());
            }
        };
    }
}
