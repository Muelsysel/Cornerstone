package com.cornerstone.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从网关透传头填充 {@link UserContextHolder}，请求结束自动清理。 仅当透传头存在时生效，匿名请求不受影响。
 *
 * <p><b>防伪造</b>：透传头（X-Cornerstone-*）只能由网关按 JWT 重建。 直接访问服务端口时攻击者可伪造透传头冒充任意身份（如伪造 roles=admin
 * 提升数据权限范围）。因此当配置了 {@code cornerstone.internal-token} 时， 携带透传头的请求必须同时携带有效的
 * X-Internal-Token（网关转发时附加）才被采信； 否则视为匿名（忽略透传头，fail-closed）。未配置该值时保持旧行为（直连信任）， 便于最小化部署与单测。
 */
public class UserContextFilter extends OncePerRequestFilter {

    private static final String INTERNAL_HEADER = "X-Internal-Token";

    @Value("${cornerstone.internal-token:}")
    private String internalToken;

    public UserContextFilter() {}

    /** 测试用：直接注入令牌，绕开 @Value。 */
    UserContextFilter(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(UserContext.HEADER_USER_ID, request.getHeader(UserContext.HEADER_USER_ID));
            headers.put(
                    UserContext.HEADER_USERNAME, request.getHeader(UserContext.HEADER_USERNAME));
            headers.put(UserContext.HEADER_DEPT_ID, request.getHeader(UserContext.HEADER_DEPT_ID));
            headers.put(UserContext.HEADER_ROLES, request.getHeader(UserContext.HEADER_ROLES));
            boolean hasIdentity =
                    headers.values().stream().anyMatch(v -> v != null && !v.isBlank());
            if (hasIdentity && !isTrustedGateway(request)) {
                // 声称身份但无法证明经网关转发：忽略透传头（匿名处理），防直连伪造
                UserContextHolder.clear();
            } else {
                UserContextHolder.set(UserContextHolder.parse(headers));
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    /** 透传头仅在网关转发场景采信：未配置令牌（旧行为）或请求携带有效 X-Internal-Token。 */
    private boolean isTrustedGateway(HttpServletRequest request) {
        if (internalToken == null || internalToken.isBlank()) {
            return true;
        }
        String presented = request.getHeader(INTERNAL_HEADER);
        // 恒定时间比较，避免时序侧信道
        return presented != null
                && MessageDigest.isEqual(
                        internalToken.getBytes(StandardCharsets.UTF_8),
                        presented.getBytes(StandardCharsets.UTF_8));
    }
}
