package com.cornerstone.system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 内部令牌过滤器单测：/system/auth/** 需共享内部令牌（恒定时间比较），错误/缺失 401，非内部路径放行。 */
class InternalTokenFilterTest {

    private static final String TOKEN = "cornerstone-internal-secret";

    private InternalTokenFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new InternalTokenFilter();
        Field field = InternalTokenFilter.class.getDeclaredField("internalToken");
        field.setAccessible(true);
        field.set(filter, TOKEN);
    }

    private FilterChain chain() {
        return mock(FilterChain.class);
    }

    @Test
    void internalPathWithValidTokenPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/system/auth/user/admin");
        request.addHeader("X-Internal-Token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = chain();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void internalPathWithWrongTokenReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/system/auth/user/admin");
        request.addHeader("X-Internal-Token", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = chain();

        filter.doFilter(request, response, chain);

        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void internalPathWithoutTokenReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/system/auth/user/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void nonInternalPathPassesWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/system/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = chain();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
