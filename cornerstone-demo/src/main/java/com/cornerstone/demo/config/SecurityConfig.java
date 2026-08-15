package com.cornerstone.demo.config;

import com.cornerstone.common.security.RsaKeyUtils;
import com.cornerstone.common.security.UserContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 资源服务器安全配置：
 *
 * <ul>
 *   <li>所有请求校验 JWT（RS256，公钥来自 {@code cornerstone.security.public-key}）；
 *   <li>公开端点白名单放行，其余需认证；
 *   <li>从 JWT 权限声明解析 authority，支撑方法级 {@code @PreAuthorize("hasAuthority(...)")}。
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** 公开端点白名单：公告查询、Springdoc 文档与 Actuator 健康检查无需登录 */
    private static final String[] PUBLIC_PATHS = {
        "/demo/announcement/page/**",
        "/demo/announcement/*",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/**"
    };

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter())))
                .addFilterAfter(userContextFilter, AuthorizationFilter.class);
        return http.build();
    }

    /** 从 JWT 权限声明解析 authority 列表，供 {@code hasAuthority} 使用 */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        // 权限声明默认取自 scope/scp，此处明确关闭前缀，保持与授权码签发一致
        converter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }

    /** 校验 JWT 用 RSA 公钥：解析 PEM（与 gateway/auth/system 共用同一公钥，解析逻辑统一走 common 工具） */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${cornerstone.security.public-key}") String publicKeyPem) {
        return NimbusJwtDecoder.withPublicKey(RsaKeyUtils.parsePublicKey(publicKeyPem)).build();
    }
}
