package com.cornerstone.system;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.service.SysOperLogService;
import com.cornerstone.system.service.SysUserService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 资源服务器 + 权限注解 MockMvc 测试。 覆盖：无 token 401、带权限 200、无权限 403。 服务层用 @MockBean 隔离，避免依赖 MySQL/Redis。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SysUserService userService;

    /**
     * @OperLog 切面会写操作日志，mock 掉避免未建表（本测试类不依赖 MySQL/H2 表）
     */
    @MockBean private SysOperLogService operLogService;

    @Test
    void withoutToken_shouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/system/user/page")).andExpect(status().isUnauthorized());
    }

    @Test
    void withPermission_shouldReturnOk() throws Exception {
        // 模拟服务层返回空分页，验证整条 认证→授权→接口 链路
        Page<SysUser> page = new Page<>(1, 10);
        whenUserPage(page);

        String token = signToken(List.of("system:user:list"));
        mockMvc.perform(get("/system/user/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void withoutPermission_shouldBeForbidden() throws Exception {
        whenUserPage(new Page<>(1, 10));

        String token = signToken(List.of("system:role:list"));
        mockMvc.perform(get("/system/user/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void withOtherPermission_shouldBeForbidden() throws Exception {
        // 持有一项与接口无关的权限 → 403
        whenUserPage(new Page<>(1, 10));
        String token = signToken(List.of());
        mockMvc.perform(get("/system/user/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_shouldBindPasswordToService() throws Exception {
        // 回归：password 字段曾用 @JsonIgnore（同时阻止反序列化），
        // 前端填写的初始密码永远收不到 → 服务层落默认 123456。现为 WRITE_ONLY，必须透传到 service。
        org.mockito.Mockito.when(userService.add(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        String token = signToken(List.of("system:user:add"));
        mockMvc.perform(
                        post("/system/user")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"newbie\",\"password\":\"init-pass-9\",\"status\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        org.mockito.Mockito.verify(userService).add(captor.capture());
        // 核心断言：明文密码到达服务层（而非 null）
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPassword())
                .isEqualTo("init-pass-9");
    }

    @Test
    void userPage_shouldNotExposePasswordHash() throws Exception {
        // 序列化侧仍是 WRITE_ONLY：分页响应绝不包含密码哈希
        SysUser row = new SysUser();
        row.setId(1L);
        row.setUsername("admin");
        row.setPassword("$2a$10$should-not-leak");
        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(row));
        page.setTotal(1);
        whenUserPage(page);

        String token = signToken(List.of("system:user:list"));
        mockMvc.perform(get("/system/user/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist());
    }

    private void whenUserPage(Page<SysUser> page) {
        org.mockito.Mockito.when(
                        userService.page(
                                anyLong(),
                                anyLong(),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(page);
    }

    /** 用测试私钥签发 RS256 JWT，权限放入 scope 声明（空格分隔，与 auth 约定一致） */
    private String signToken(List<String> authorities) throws Exception {
        PrivateKey privateKey = readPrivateKey();
        RSASSASigner signer = new RSASSASigner(privateKey);
        long now = System.currentTimeMillis();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .subject("1")
                        .issuer("cornerstone-auth")
                        .issueTime(new Date(now))
                        .expirationTime(new Date(now + 3600_000L))
                        .claim("scope", String.join(" ", authorities))
                        .build();
        SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signed.sign(signer);
        return signed.serialize();
    }

    private PrivateKey readPrivateKey() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/keys/test-private.pem")) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 =
                    pem.replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replaceAll("\\s+", "");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64));
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }
}
