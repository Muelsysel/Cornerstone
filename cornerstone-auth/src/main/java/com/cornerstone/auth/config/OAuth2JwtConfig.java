package com.cornerstone.auth.config;

import com.cornerstone.common.security.RsaKeyUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/** 认证中心令牌与客户端配置： 从 PEM 资源加载 RSA 密钥对、注册记忆客户端、构建 JwtDecoder。 */
@Configuration
public class OAuth2JwtConfig {

    private final Resource privateKeyResource;
    private final Resource publicKeyResource;

    public OAuth2JwtConfig(
            @Value("${cornerstone.auth.rsa.private-key}") Resource privateKeyResource,
            @Value("${cornerstone.auth.rsa.public-key}") Resource publicKeyResource) {
        this.privateKeyResource = privateKeyResource;
        this.publicKeyResource = publicKeyResource;
    }

    /** 签发与校验共用的 RSA 密钥对（从 PEM 资源加载） */
    @Bean
    public RsaKeyPair rsaKeyPair() {
        return RsaKeyPair.load(privateKeyResource, publicKeyResource);
    }

    /** 记忆客户端仓库：cornerstone-client，client_credentials + read/write scope */
    @Bean
    public RegisteredClientRepository registeredClientRepository(
            @Value("${cornerstone.auth.client.client-id}") String clientId,
            @Value("${cornerstone.auth.client.client-secret}") String clientSecret) {
        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(clientId)
                        .clientSecret("{noop}" + clientSecret)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("read")
                        .scope("write")
                        .tokenSettings(TokenSettings.builder().build())
                        .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    /** JWK 源，供 /oauth2/jwks 端点与 JwtEncoder 使用 */
    @Bean
    public JWKSource<SecurityContext> jwkSource(RsaKeyPair keyPair) {
        RSAKey rsaKey =
                new RSAKey.Builder(keyPair.publicKey())
                        .privateKey(keyPair.privateKey())
                        .keyID(keyPair.keyId())
                        .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /** JwtDecoder：用配置的公钥校验签名（网关用同一公钥） */
    @Bean
    public JwtDecoder jwtDecoder(RsaKeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey(keyPair.publicKey()).build();
    }

    /** RSA 密钥对载体。提供 keyId，便于 JWT kid 与 JWKS 对应。 */
    public record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey, String keyId) {

        /** 从私钥/公钥两个 PEM 资源加载密钥对，keyId 由公钥指纹派生 */
        static RsaKeyPair load(Resource privRes, Resource pubRes) {
            try (InputStream privIn = privRes.getInputStream();
                    InputStream pubIn = pubRes.getInputStream()) {
                RSAPrivateKey priv = RsaKeyUtils.parsePrivateKey(new String(privIn.readAllBytes()));
                RSAPublicKey pub = RsaKeyUtils.parsePublicKey(new String(pubIn.readAllBytes()));
                String kid = UUID.nameUUIDFromBytes(pub.getEncoded()).toString();
                return new RsaKeyPair(priv, pub, kid);
            } catch (Exception e) {
                throw new IllegalStateException("加载 RSA 密钥失败", e);
            }
        }
    }
}
