package com.cornerstone.system;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysConfig;
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.service.SysConfigService;
import com.cornerstone.system.service.SysDictService;
import com.cornerstone.system.service.SysLoginLogService;
import com.cornerstone.system.service.SysOperLogService;
import com.cornerstone.system.service.SysUserService;
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
    @MockBean private SysUserService userService;

    /** 拥有系统管理全部权限的 token */
    private static final List<String> ALL =
            List.of(
                    "system:user:list",
                    "system:dict:list",
                    "system:config:list",
                    "system:log:list");

    /** 分页参数契约：前端传 pageNum/pageSize，controller 必须原样透传（回归：曾误用 current/size 导致翻页失效） */
    @Test
    void userPage_shouldPassPaginationParamsToService() throws Exception {
        when(userService.page(2L, 5L, null, null, null)).thenReturn(new Page<SysUser>(2, 5));

        mockMvc.perform(
                        get("/system/user/page")
                                .param("pageNum", "2")
                                .param("pageSize", "5")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).page(2L, 5L, null, null, null);
    }

    @Test
    void dictTypePage_shouldPassPaginationParamsToService() throws Exception {
        org.mockito.Mockito.when(
                        dictService.pageType(
                                eq(3L), eq(20L), anyString(), anyString(), anyString()))
                .thenReturn(new Page<SysDictType>(3, 20));
        mockMvc.perform(
                        get("/system/dict/type/page")
                                .param("pageNum", "3")
                                .param("pageSize", "20")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(dictService).pageType(3L, 20L, null, null, null);
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
    void configPage_shouldPassPaginationParamsToService() throws Exception {
        org.mockito.Mockito.when(
                        configService.page(eq(2L), eq(50L), anyString(), anyString(), anyString()))
                .thenReturn(new Page<SysConfig>(2, 50));
        mockMvc.perform(
                        get("/system/config/page")
                                .param("pageNum", "2")
                                .param("pageSize", "50")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(configService).page(2L, 50L, null, null, null);
    }

    @Test
    void operLogPage_shouldPassPaginationParamsToService() throws Exception {
        org.mockito.Mockito.when(
                        operLogService.page(
                                eq(4L),
                                eq(10L),
                                anyString(),
                                anyString(),
                                anyString(),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(new Page<SysOperLog>(4, 10));
        mockMvc.perform(
                        get("/system/operlog/page")
                                .param("pageNum", "4")
                                .param("pageSize", "10")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(operLogService).page(4L, 10L, null, null, null, null, null);
    }

    @Test
    void loginLogPage_shouldPassPaginationParamsToService() throws Exception {
        org.mockito.Mockito.when(
                        loginLogService.page(
                                eq(5L),
                                eq(15L),
                                anyString(),
                                anyString(),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(new Page<SysLoginLog>(5, 15));
        mockMvc.perform(
                        get("/system/loginlog/page")
                                .param("pageNum", "5")
                                .param("pageSize", "15")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(loginLogService).page(5L, 15L, null, null, null, null);
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

    @Test
    void entityIdJsonContractUsesFrontendFieldNames() throws Exception {
        // 回归：主键 JSON 字段名必须对齐前端（operId/infoId/configId/dictId/dictCode），
        // 曾因缺 @JsonProperty 映射，前端删除日志/字典时取到 null id → 请求失败
        com.cornerstone.system.domain.entity.SysOperLog log =
                new com.cornerstone.system.domain.entity.SysOperLog();
        log.setId(7L);
        org.mockito.Mockito.when(
                        operLogService.page(
                                eq(1L),
                                eq(10L),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class),
                                org.mockito.ArgumentMatchers.nullable(String.class)))
                .thenReturn(pageWith(log));
        mockMvc.perform(
                        get("/system/operlog/page")
                                .param("pageNum", "1")
                                .param("pageSize", "10")
                                .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].operId").value(7));
    }

    private <T> Page<T> pageWith(T record) {
        Page<T> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(record));
        page.setTotal(1);
        return page;
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
