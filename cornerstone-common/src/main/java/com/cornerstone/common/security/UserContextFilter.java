package com.cornerstone.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;

/** 从网关透传头填充 {@link UserContextHolder}，请求结束自动清理。 仅当透传头存在时生效，匿名请求不受影响。 */
public class UserContextFilter extends OncePerRequestFilter {

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
            UserContextHolder.set(UserContextHolder.parse(headers));
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
