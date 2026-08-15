package com.cornerstone.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.Result;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 用户登录端点测试：正确密码 200 换 JWT、错误密码/用户不存在 401。 AuthUserClient 以 @MockBean 隔离，不依赖 MySQL/Redis/Nacos。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    /** admin123 的 BCrypt 哈希（与 system V2__seed.sql 一致） */
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S";

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtDecoder jwtDecoder;

    @MockBean private AuthUserClient authUserClient;

    @Test
    void validCredentials_shouldReturnTokenWithPermissions() throws Exception {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(adminUser()));

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\"admin\",\"password\":\"admin123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                        .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                        .andExpect(jsonPath("$.data.userId").value(1))
                        .andExpect(jsonPath("$.data.username").value("admin"))
                        .andReturn();

        String json = result.getResponse().getContentAsString();
        String token = extractToken(json);
        // 用配置公钥验签，且 scope 携带权限、roles 携带角色
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.<Iterable<String>>getClaim("scope")).contains("system:user:list");
        assertThat(jwt.<Iterable<String>>getClaim("roles")).contains("admin");
    }

    @Test
    void wrongPassword_shouldBeUnauthorized() throws Exception {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(adminUser()));

        mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void unknownUser_shouldBeUnauthorized() throws Exception {
        when(authUserClient.findByUsername("nobody")).thenReturn(Result.success(null));

        mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    private UserAuthDTO adminUser() {
        UserAuthDTO dto = new UserAuthDTO();
        dto.setUserId(1L);
        dto.setUsername("admin");
        dto.setPassword(ADMIN_PASSWORD_HASH);
        dto.setDeptId(100L);
        dto.setRoles(new LinkedHashSet<>(Set.of("admin")));
        dto.setPermissions(new LinkedHashSet<>(Set.of("system:user:list", "system:role:list")));
        return dto;
    }

    private String extractToken(String json) {
        // 快速截取 access_token 字段值（无 Jackson JsonNode 依赖直接读取）
        String marker = "\"access_token\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
