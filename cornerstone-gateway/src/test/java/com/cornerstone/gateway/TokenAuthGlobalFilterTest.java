package com.cornerstone.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornerstone.common.security.UserContext;
import com.cornerstone.gateway.filter.TokenAuthGlobalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/** 网关令牌校验过滤器单元测试： 白名单放行、无令牌 401、有效令牌放行并透传上下文头。 */
class TokenAuthGlobalFilterTest {

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    private static JwtEncoder jwtEncoder;
    private static ReactiveJwtDecoder jwtDecoder;

    @BeforeAll
    static void setupKeys() throws Exception {
        privateKey = readPrivateKey(new ClassPathResource("test-private.pem"));
        publicKey = readPublicKey(new ClassPathResource("test-public.pem"));
        RSAKey rsaKey =
                new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("test-key").build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        jwtEncoder = new NimbusJwtEncoder(jwkSource);
        jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
    }

    private static TokenAuthGlobalFilter filter() {
        return new TokenAuthGlobalFilter(jwtDecoder, new ObjectMapper());
    }

    /** 白名单路径无令牌放行 */
    @Test
    void whitelistPathPassesWithoutToken() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/auth/oauth2/token"));
        boolean[] continued = {false};
        GatewayFilterChain chain =
                ex -> {
                    continued[0] = true;
                    return reactor.core.publisher.Mono.empty();
                };
        filter().filter(exchange, chain).block();
        assertThat(continued[0]).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    /** 非白名单路径无令牌返回 401 + Result JSON */
    @Test
    void protectedPathWithoutTokenReturns401() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/system/user/1"));
        filter().filter(exchange, (ex) -> reactor.core.publisher.Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull().contains("\"code\":401");
        assertThat(body).contains("未认证或令牌无效");
    }

    /** 有效令牌放行，且透传上下文头正确 */
    @Test
    void validTokenPassesAndSetsPassthroughHeaders() {
        String token =
                issueToken(
                        Map.of(
                                "client_id",
                                "cornerstone-client",
                                "scope",
                                List.of("read", "write")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/system/user/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        ServerWebExchangeMutator mutator = new ServerWebExchangeMutator();
        filter().filter(exchange, mutator).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        HttpHeaders forwarded = mutator.getMutatedHeaders();
        // client_credentials 令牌：sub 为 client_id（非数字）→ 透传为用户名，不写 User-Id
        assertThat(forwarded.getFirst(UserContext.HEADER_USER_ID)).isNull();
        assertThat(forwarded.getFirst(UserContext.HEADER_USERNAME)).isEqualTo("cornerstone-client");
        // scope：测试令牌 scope 为 read,write
        assertThat(forwarded.getFirst(UserContext.HEADER_ROLES)).isEqualTo("read,write");
    }

    /** 无效令牌返回 401 */
    @Test
    void invalidTokenReturns401() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/system/user/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here"));
        filter().filter(exchange, (ex) -> reactor.core.publisher.Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 白名单路径也必须剥除客户端伪造的透传头（防身份伪造） */
    @Test
    void whitelistPathStripsForgedPassthroughHeaders() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/demo/announcement/page")
                                .header(UserContext.HEADER_USER_ID, "999")
                                .header(UserContext.HEADER_ROLES, "admin"));
        ServerWebExchangeMutator mutator = new ServerWebExchangeMutator();
        filter().filter(exchange, mutator).block();

        assertThat(mutator.getMutatedHeaders().getFirst(UserContext.HEADER_USER_ID)).isNull();
        assertThat(mutator.getMutatedHeaders().getFirst(UserContext.HEADER_ROLES)).isNull();
    }

    /** 白名单路径携带有效令牌时同样重建透传头（公告管理等受保护操作依赖用户身份/审计） */
    @Test
    void whitelistPathWithValidTokenBuildsPassthroughHeaders() {
        String token = issueToken(Map.of("username", "admin", "scope", List.of("read")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/demo/announcement/page")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        ServerWebExchangeMutator mutator = new ServerWebExchangeMutator();
        filter().filter(exchange, mutator).block();

        HttpHeaders forwarded = mutator.getMutatedHeaders();
        // sub 为 client_id（非数字）→ Username 取 sub；JWT username 声明作为追加值
        assertThat(forwarded.getFirst(UserContext.HEADER_USERNAME)).isEqualTo("cornerstone-client");
        assertThat(forwarded.getFirst(UserContext.HEADER_ROLES)).isEqualTo("read");
    }

    /** 有效令牌 + 外部伪造透传头：伪造值被剥除，透传头只来自 JWT 声明 */
    @Test
    void validTokenOverridesForgedPassthroughHeaders() {
        String token = issueToken(Map.of("scope", List.of("read")));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/system/user/1")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .header(UserContext.HEADER_USER_ID, "999")
                                .header(UserContext.HEADER_ROLES, "admin"));
        ServerWebExchangeMutator mutator = new ServerWebExchangeMutator();
        filter().filter(exchange, mutator).block();

        HttpHeaders forwarded = mutator.getMutatedHeaders();
        // 伪造的 User-Id 被剥除（JWT sub 非数字 → 不写 User-Id）；Roles 来自 JWT scope
        assertThat(forwarded.getFirst(UserContext.HEADER_USER_ID)).isNull();
        assertThat(forwarded.getFirst(UserContext.HEADER_ROLES)).isEqualTo("read");
        assertThat(forwarded.getFirst(UserContext.HEADER_USERNAME)).isEqualTo("cornerstone-client");
    }

    /** 透传头捕获器：记录过滤链实际转发时携带的头 */
    static class ServerWebExchangeMutator implements GatewayFilterChain {
        private HttpHeaders mutatedHeaders;

        @Override
        public reactor.core.publisher.Mono<Void> filter(
                org.springframework.web.server.ServerWebExchange exchange) {
            this.mutatedHeaders = exchange.getRequest().getHeaders();
            return reactor.core.publisher.Mono.empty();
        }

        HttpHeaders getMutatedHeaders() {
            return mutatedHeaders;
        }
    }

    private String issueToken(Map<String, ?> claims) {
        JwtClaimsSet.Builder b =
                JwtClaimsSet.builder()
                        .subject("cornerstone-client")
                        .issuer("http://localhost:8081")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .id(UUID.randomUUID().toString());
        claims.forEach(b::claim);
        return jwtEncoder
                .encode(
                        org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                                b.build()))
                .getTokenValue();
    }

    private static RSAPrivateKey readPrivateKey(org.springframework.core.io.Resource res)
            throws Exception {
        try (InputStream in = res.getInputStream()) {
            byte[] der = Base64.getDecoder().decode(decodePem(new String(in.readAllBytes())));
            return (RSAPrivateKey)
                    KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        }
    }

    private static RSAPublicKey readPublicKey(org.springframework.core.io.Resource res)
            throws Exception {
        try (InputStream in = res.getInputStream()) {
            byte[] der = Base64.getDecoder().decode(decodePem(new String(in.readAllBytes())));
            return (RSAPublicKey)
                    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        }
    }

    private static String decodePem(String pem) {
        return pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    }
}
