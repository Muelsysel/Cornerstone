package com.cornerstone.gateway.config;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
            String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            RSAPublicKey publicKey =
                    (RSAPublicKey)
                            KeyFactory.getInstance("RSA")
                                    .generatePublic(
                                            new X509EncodedKeySpec(
                                                    Base64.getDecoder().decode(base64)));
            return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            throw new IllegalStateException("加载网关 JWT 公钥失败", e);
        }
    }
}
