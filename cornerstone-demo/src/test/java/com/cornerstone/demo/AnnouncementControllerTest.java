package com.cornerstone.demo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 公告管理 MockMvc 集成测试（H2 跑 Flyway，不依赖 MySQL）。 覆盖：公开读接口免登录、受保护接口未认证 401、带权访问 200、非法状态流转业务异常、无权限 403。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnouncementControllerTest {

    private static final String PAGE_URL = "/demo/announcement/page";
    private static final String CREATE_URL = "/demo/announcement";
    private static final String EDIT_PERMISSION = "demo:announcement:edit";

    @Autowired private MockMvc mockMvc;

    @Test
    void publicPageReturns200WithoutToken() throws Exception {
        mockMvc.perform(get(PAGE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void publicPageFiltersByTitleAndStatus() throws Exception {
        mockMvc.perform(get(PAGE_URL).param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 种子数据只有一条已发布公告（V2__seed.sql）
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void protectedCreateReturns401WithoutToken() throws Exception {
        mockMvc.perform(
                        post(CREATE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"未认证创建\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns200WithTokenAndPermission() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        mockMvc.perform(
                        post(CREATE_URL)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"带权创建\",\"content\":\"内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void protectedCreateReturns403WithoutPermission() throws Exception {
        String token = TestJwtIssuer.tokenWithoutPermission();
        mockMvc.perform(
                        post(CREATE_URL)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"无权创建\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void illegalStateTransitionReturnsBusinessError() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        // 种子数据 id=2 为草稿(0)，草稿直接下线为非法流转，应返回业务错误结构（HTTP 200 + 业务错误 body）
        mockMvc.perform(
                        post("/demo/announcement/2/offline")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", not(200)))
                .andExpect(jsonPath("$.message", containsString("仅已发布")));
    }

    @Test
    void createWithBlankTitleReturnsBusinessError() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        mockMvc.perform(
                        post(CREATE_URL)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"无标题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", not(200)))
                .andExpect(jsonPath("$.message", containsString("标题")));
    }

    /** 编辑契约：PUT /demo/announcement/{id}（回归：曾用无 id 路径导致编辑 404） */
    @Test
    void updateDraftReturns200WithTokenAndPermission() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        // 种子 id=2 为草稿，可编辑
        mockMvc.perform(
                        put("/demo/announcement/2")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"更新后的标题\",\"content\":\"新内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void protectedUpdateReturns401WithoutToken() throws Exception {
        // 公开白名单仅放行 GET；PUT 写操作在 URL 层即要求认证（无 token → 401）
        mockMvc.perform(
                        put("/demo/announcement/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"未认证更新\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** 发布契约：POST /{id}/publish（种子 id=2 为草稿，可发布；@Transactional 回滚避免污染共享数据） */
    @Test
    @org.springframework.transaction.annotation.Transactional
    void publishReturns200WithTokenAndPermission() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        mockMvc.perform(
                        post("/demo/announcement/2/publish")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 下线契约：POST /{id}/offline（种子 id=1 为已发布，可下线；@Transactional 回滚） */
    @Test
    @org.springframework.transaction.annotation.Transactional
    void offlineReturns200WithTokenAndPermission() throws Exception {
        String token = TestJwtIssuer.tokenWithScope(EDIT_PERMISSION);
        mockMvc.perform(
                        post("/demo/announcement/1/offline")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
