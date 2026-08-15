package com.cornerstone.common.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * 用户上下文持有者：解析网关透传头并存放于 ThreadLocal。 必须在请求结束时调用 {@link #clear()} 防止线程复用串号（由 {@link
 * UserContextFilter} 自动完成）。
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    /** 从透传头解析用户上下文；无相关头时返回 null（匿名请求） */
    public static UserContext parse(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String userId = headers.get(UserContext.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            return null;
        }
        UserContext context = new UserContext();
        try {
            context.setUserId(Long.valueOf(userId.trim()));
        } catch (NumberFormatException e) {
            // 非数字主体（如 client_credentials 的 client_id）：不设 userId，仅保留其他字段
            context.setUserId(null);
        }
        context.setUsername(headers.get(UserContext.HEADER_USERNAME));
        String deptId = headers.get(UserContext.HEADER_DEPT_ID);
        context.setDeptId(deptId == null || deptId.isBlank() ? null : Long.valueOf(deptId.trim()));
        String roles = headers.get(UserContext.HEADER_ROLES);
        if (roles != null && !roles.isBlank()) {
            context.setRoles(new HashSet<>(Arrays.asList(roles.split(","))));
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
