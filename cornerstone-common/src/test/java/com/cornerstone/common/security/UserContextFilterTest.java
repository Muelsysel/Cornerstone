package com.cornerstone.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 用户上下文过滤器单测：透传头填充上下文、请求结束清理、匿名请求不受影响。 */
class UserContextFilterTest {

    private final UserContextFilter filter = new UserContextFilter();

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    @Test
    void passthroughHeadersPopulateContextAndCleanUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserContext.HEADER_USER_ID, "7");
        request.addHeader(UserContext.HEADER_USERNAME, "alice");
        request.addHeader(UserContext.HEADER_DEPT_ID, "100");
        request.addHeader(UserContext.HEADER_ROLES, "admin,editor");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        // 请求结束必须清理 ThreadLocal，防止线程复用串号
        assertThat(UserContextHolder.get()).isNull();
    }

    @Test
    void contextVisibleInsideChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserContext.HEADER_USER_ID, "7");
        request.addHeader(UserContext.HEADER_USERNAME, "alice");
        UserContext[] captured = new UserContext[1];
        FilterChain chain =
                (req, res) -> {
                    captured[0] = UserContextHolder.get();
                };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNotNull();
        assertThat(captured[0].getUsername()).isEqualTo("alice");
        assertThat(UserContextHolder.get()).isNull();
    }

    @Test
    void anonymousRequestLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserContext[] captured = new UserContext[1];
        FilterChain chain =
                (req, res) -> {
                    captured[0] = UserContextHolder.get();
                };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void forgedHeadersWithoutInternalTokenAreIgnored() throws Exception {
        // 回归：直连服务端口伪造透传头（无网关内部令牌）→ 身份被忽略（匿名处理，fail-closed）
        UserContextFilter guarded = new UserContextFilter("cornerstone-internal-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserContext.HEADER_USER_ID, "1");
        request.addHeader(UserContext.HEADER_ROLES, "admin");
        UserContext[] captured = new UserContext[1];
        FilterChain chain =
                (req, res) -> {
                    captured[0] = UserContextHolder.get();
                };

        guarded.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void forgedHeadersWithWrongInternalTokenAreIgnored() throws Exception {
        UserContextFilter guarded = new UserContextFilter("cornerstone-internal-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserContext.HEADER_USER_ID, "1");
        request.addHeader("X-Internal-Token", "wrong-secret");
        UserContext[] captured = new UserContext[1];
        FilterChain chain =
                (req, res) -> {
                    captured[0] = UserContextHolder.get();
                };

        guarded.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNull();
    }

    @Test
    void gatewayHeadersWithValidInternalTokenPopulate() throws Exception {
        UserContextFilter guarded = new UserContextFilter("cornerstone-internal-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(UserContext.HEADER_USER_ID, "7");
        request.addHeader(UserContext.HEADER_USERNAME, "alice");
        request.addHeader("X-Internal-Token", "cornerstone-internal-secret");
        UserContext[] captured = new UserContext[1];
        FilterChain chain =
                (req, res) -> {
                    captured[0] = UserContextHolder.get();
                };

        guarded.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(captured[0]).isNotNull();
        assertThat(captured[0].getUserId()).isEqualTo(7L);
        assertThat(captured[0].getUsername()).isEqualTo("alice");
    }
}
