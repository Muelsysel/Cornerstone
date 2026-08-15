package com.cornerstone.gateway.config;

import com.cornerstone.common.security.RsaKeyUtils;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/** 网关 JWT 解码器：用与认证中心相同的 RSA 公钥校验令牌。 */
@Configuration
public class JwtDecoderConfig {

    /** 从 PEM 构造 RS256 的 ReactiveJwtDecoder */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${cornerstone.gateway.jwt.public-key}") Resource publicKeyResource) {
        try (InputStream in = publicKeyResource.getInputStream()) {
            String pem = new String(in.readAllBytes());
            RSAPublicKey publicKey = RsaKeyUtils.parsePublicKey(pem);
            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            throw new IllegalStateException("加载网关 JWT 公钥失败", e);
        }
    }
}
