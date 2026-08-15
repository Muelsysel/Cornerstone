package com.cornerstone.demo.config;

import com.cornerstone.common.security.RsaKeyUtils;
import com.cornerstone.common.security.UserContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    /** 公开读端点：公告分页/详情仅 GET 放行（写操作需认证 + 方法级权限，双保险） */
    private static final String[] PUBLIC_GET_PATHS = {
        "/demo/announcement/page/**", "/demo/announcement/*"
    };

    /** 其他公开路径：Springdoc 文档与 Actuator 健康检查无需登录 */
    private static final String[] PUBLIC_OTHER_PATHS = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**"
    };

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // CORS 白名单与网关一致：直连服务时合法预检也放行
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                                        .permitAll()
                                        .requestMatchers(PUBLIC_OTHER_PATHS)
                                        .permitAll()
                                        // CORS 预检放行（直连服务/跳过网关时预检无凭据头）
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
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

    /** CORS 白名单（与网关 globalcors 一致）：仅本地开发/演示来源。 生产经 nginx 同源反代不触发 CORS。 */
    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config =
                new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(
                java.util.List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:8088",
                        "http://127.0.0.1:8088"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
