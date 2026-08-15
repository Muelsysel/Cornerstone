package com.cornerstone.system;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysConfig;
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysConfigService;
import com.cornerstone.system.service.SysDictService;
import com.cornerstone.system.service.SysLoginLogService;
import com.cornerstone.system.service.SysOperLogService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 扩展模块（字典/参数/登录日志/操作日志）查询接口 MockMvc 测试。 服务层 @MockBean 隔离，不依赖 MySQL/Redis。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemExtensionTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SysDictService dictService;
    @MockBean private SysConfigService configService;
    @MockBean private SysOperLogService operLogService;
    @MockBean private SysLoginLogService loginLogService;

    /** 拥有系统管理全部权限的 token */
    private static final List<String> ALL =
            List.of("system:dict:list", "system:config:list", "system:log:list");

    @Test
    void dictTypePage_shouldReturnOk() throws Exception {
        org.mockito.Mockito.when(
                        dictService.pageType(
                                anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new Page<SysDictType>(1, 10));
        mockMvc.perform(get("/system/dict/type/page").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void dictDataByType_shouldReturnData() throws Exception {
        SysDictData row = new SysDictData();
        row.setDictType("sys_normal_disable");
        row.setDictLabel("启用");
        row.setDictValue("0");
        org.mockito.Mockito.when(dictService.listData(eq("sys_normal_disable")))
                .thenReturn(List.of(row));
        mockMvc.perform(
                        get("/system/dict/data/type/sys_normal_disable")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dictLabel").value("启用"));
    }

    @Test
    void configPage_shouldReturnOk() throws Exception {
        org.mockito.Mockito.when(
                        configService.page(
                                anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new Page<SysConfig>(1, 10));
        mockMvc.perform(get("/system/config/page").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void operLogPage_shouldReturnOk() throws Exception {
        org.mockito.Mockito.when(
                        operLogService.page(
                                anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new Page<SysOperLog>(1, 10));
        mockMvc.perform(get("/system/operlog/page").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void loginLogPage_shouldReturnOk() throws Exception {
        org.mockito.Mockito.when(
                        loginLogService.page(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new Page<SysLoginLog>(1, 10));
        mockMvc.perform(get("/system/loginlog/page").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private String bearer() {
        try {
            return "Bearer " + signToken(ALL);
        } catch (Exception e) {
            throw new IllegalStateException("生成测试 JWT 失败", e);
        }
    }

    private String signToken(List<String> authorities) throws Exception {
        PrivateKey privateKey = readPrivateKey();
        com.nimbusds.jose.crypto.RSASSASigner signer =
                new com.nimbusds.jose.crypto.RSASSASigner(privateKey);
        long now = System.currentTimeMillis();
        com.nimbusds.jwt.JWTClaimsSet claims =
                new com.nimbusds.jwt.JWTClaimsSet.Builder()
                        .subject("1")
                        .issuer("cornerstone-auth")
                        .issueTime(new Date(now))
                        .expirationTime(new Date(now + 3600_000L))
                        .claim("scope", String.join(" ", authorities))
                        .build();
        com.nimbusds.jwt.SignedJWT signed =
                new com.nimbusds.jwt.SignedJWT(
                        new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256),
                        claims);
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
