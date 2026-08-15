package com.cornerstone.demo;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StreamUtils;

/**
 * 测试辅助：用测试密钥对签发 RS256 JWT。权限经 {@code scope} 声明携带， 与 {@code SecurityConfig} 的 {@code
 * JwtGrantedAuthoritiesConverter} 解析一致。
 */
public final class TestJwtIssuer {

    private static final RSAKey RSA_KEY = buildRsaKey();

    private TestJwtIssuer() {}

    /** 签发带指定权限 scope 的 JWT */
    public static String tokenWithScope(String scope) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(RSA_KEY)));
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("http://localhost:8080")
                        .subject("demo-test-user")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .claim("scope", scope)
                        .claim("roles", List.of("admin"))
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(() -> "RS256").keyId("demo-test-key").build(),
                                claims))
                .getTokenValue();
    }

    /** 无权 token：scope 为空，用于验证 403 */
    public static String tokenWithoutPermission() {
        return tokenWithScope("");
    }

    private static RSAKey buildRsaKey() {
        try {
            RSAPrivateKey privateKey = loadPrivateKey();
            RSAPublicKey publicKey = loadPublicKey();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID("demo-test-key")
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("加载测试密钥对失败", e);
        }
    }

    private static RSAPrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = readPem("test-private-key.pem");
        return (RSAPrivateKey)
                KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static RSAPublicKey loadPublicKey() throws Exception {
        byte[] keyBytes = readPem("test-public-key.pem");
        return (RSAPublicKey)
                KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private static byte[] readPem(String resource) throws Exception {
        String pem =
                StreamUtils.copyToString(
                                new ClassPathResource(resource).getInputStream(),
                                StandardCharsets.UTF_8)
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }
}
