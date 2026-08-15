package com.cornerstone.system.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 资源服务器安全配置。 - 双保险：服务端同样校验网关透传的 JWT（公钥与 auth/gateway 对齐）。 - 权限来源：JWT 的 scope（→ SCOPE_*）与
 * authorities 声明（原样映射，配合 @PreAuthorize）。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    /** SPKI PEM 公钥内容（application.yml cornerstone.security.public-key） */
    @Value("${cornerstone.security.public-key}")
    private String publicKeyPem;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, InternalTokenFilter internalTokenFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                // /system/auth/** 为服务间内部接口（认证中心登录契约），网关白名单不含 /system/** 已隔离外部；
                                // 另有 InternalTokenFilter 校验共享内部令牌；/actuator/** 与 OpenAPI 文档放行
                                auth.requestMatchers(
                                                "/system/auth/**",
                                                "/actuator/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.decoder(jwtDecoder())
                                                        .jwtAuthenticationConverter(
                                                                customConverter())))
                .build();
    }

    /**
     * 权限映射：从 JWT 权限声明（默认 scope/scp）解析 authority。关闭 SCOPE_ 前缀， 与 auth 签发约定一致，使
     * {@code @PreAuthorize("hasAuthority('system:user:list')")} 生效。
     */
    @Bean
    public JwtAuthenticationConverter customConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(parseRsaPublicKey()).build();
    }

    private RSAPublicKey parseRsaPublicKey() {
        try {
            String pem =
                    publicKeyPem
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("解析 RSA 公钥失败", e);
        }
    }
}
