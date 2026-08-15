package com.cornerstone.system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 服务间内部令牌过滤器：保护 /system/auth/** 内部接口（认证中心登录契约）。
 *
 * <p>请求必须携带 {@code X-Internal-Token}（与 auth 共享的 {@code cornerstone.internal-token}， 由 auth 的 Feign
 * 拦截器自动附加），否则 401。解决 v2 内部接口匿名访问问题——外部请求 无法经网关到达（网关白名单不含 /system/**），本过滤器进一步防止 system 端口直连被滥用。
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_HEADER = "X-Internal-Token";
    private static final String INTERNAL_PATH_PREFIX = "/system/auth/";

    @Value("${cornerstone.internal-token:}")
    private String internalToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            String presented = request.getHeader(INTERNAL_HEADER);
            // 恒定时间比较，避免时序侧信道
            boolean valid =
                    !internalToken.isBlank()
                            && presented != null
                            && MessageDigest.isEqual(
                                    internalToken.getBytes(StandardCharsets.UTF_8),
                                    presented.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未认证或令牌无效\",\"data\":null}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
