package com.cornerstone.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/** 认证中心集成测试：凭证模式拿令牌、JWKS 存在、令牌签名可用配置公钥校验通过。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthEndToEndTest {

    private static final String CLIENT_ID = "cornerstone-client";
    private static final String CLIENT_SECRET = "cornerstone-secret";

    @LocalServerPort private int port;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JwtDecoder jwtDecoder;

    /** client_credentials 换 token：断言 200 且 access_token 非空 */
    @Test
    void obtainTokenViaClientCredentials() throws Exception {
        ResponseEntity<JsonNode> resp = postToken();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().get("access_token").asText()).isNotBlank();
        assertThat(resp.getBody().get("token_type").asText()).isEqualTo("Bearer");
    }

    /** JWKS 端点存在且含 RSA 公钥 */
    @Test
    void jwksContainsRsaKey() throws Exception {
        String url = "http://localhost:" + port + "/oauth2/jwks";
        ResponseEntity<JsonNode> resp =
                restTemplate.exchange(url, HttpMethod.GET, null, JsonNode.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        JsonNode keys = resp.getBody().get("keys");
        assertThat(keys).isNotEmpty();
        // JWKS 必须为 RSA 公钥：kty=RSA 且含模数 n
        assertThat(keys.get(0).get("kty").asText()).isEqualTo("RSA");
        assertThat(keys.get(0).get("n").asText()).isNotBlank();
    }

    /** 用配置公钥（JwtDecoder bean）校验 access_token 签名有效，并携带 client_id、scope 声明 */
    @Test
    void tokenSignatureVerifiableWithConfiguredPublicKey() throws Exception {
        String token = obtainAccessToken();
        var jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(CLIENT_ID);
        assertThat(jwt.getClaimAsString("client_id")).isEqualTo(CLIENT_ID);
        assertThat(jwt.<Iterable<String>>getClaim("scope")).contains("read");
    }

    private String obtainAccessToken() throws Exception {
        ResponseEntity<JsonNode> resp = postToken();
        return resp.getBody().get("access_token").asText();
    }

    private ResponseEntity<JsonNode> postToken() {
        String url = "http://localhost:" + port + "/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(CLIENT_ID, CLIENT_SECRET);
        String body = "grant_type=client_credentials&scope=read write";
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
    }
}
