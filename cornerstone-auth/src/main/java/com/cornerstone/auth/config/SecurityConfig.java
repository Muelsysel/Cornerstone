package com.cornerstone.auth.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 认证中心安全配置： 1. OAuth2 授权服务器过滤链（@Order(1)，仅匹配 /oauth2/** 端点）； 2. 用户登录链（@Order(2)）：/login 放行，其余需认证；
 * 3. JwtEncoder 用 JWK 源签发 RS256 令牌； 4. 令牌自定义：注入 client_id 与 scope 额外声明（网关据此透传上下文头）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 授权服务器安全链：令牌/授权/JWKS 端点走默认契约。 用 securityMatcher 把链限定到 /oauth2/**，避免拦截 /login 用户登录端点。 */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        // client_credentials 的令牌端点由客户端凭证认证，不受浏览器 CSRF 威胁；v2 授权码流程启用时按 OAuth2 规范细化
        http.securityMatcher("/oauth2/**").csrf(csrf -> csrf.disable());
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.build();
    }

    /** 应用默认链：/login 放行（用户名密码登录换 JWT），/error、/actuator/**、OpenAPI 文档放行，其余路径要求认证。 */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/login",
                                                "/error",
                                                "/actuator/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());
        return http.build();
    }

    /** JwtEncoder：用 JWK 源签发 RS256 JWT */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 密码编码器：DelegatingPasswordEncoder。
     *
     * <p>{noop} 前缀供 SAS 客户端密钥（cornerstone-secret）；无前缀哈希（system 种子数据的纯 BCrypt {@code $2a$...}）经
     * defaultPasswordEncoderForMatches 用 BCrypt 匹配（Spring Security 6.2 默认对无前缀 哈希抛 UnmappedId
     * 异常，故显式设置）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return delegating;
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
