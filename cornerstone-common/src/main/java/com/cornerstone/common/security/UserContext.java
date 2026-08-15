package com.cornerstone.common.security;

import java.io.Serializable;
import java.util.Set;

/** 当前请求的用户上下文。由网关校验令牌后透传，各服务经 {@link UserContextHolder} 读取。 服务间禁止自行解析令牌——令牌校验是网关的职责，服务只信任透传头。 */
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 透传头前缀（网关写入、服务读取，约定一致） */
    public static final String HEADER_PREFIX = "X-Cornerstone-";

    public static final String HEADER_USER_ID = HEADER_PREFIX + "User-Id";
    public static final String HEADER_USERNAME = HEADER_PREFIX + "Username";
    public static final String HEADER_DEPT_ID = HEADER_PREFIX + "Dept-Id";
    public static final String HEADER_ROLES = HEADER_PREFIX + "Roles";

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 部门 ID（可为空） */
    private Long deptId;

    /** 角色标识集合 */
    private Set<String> roles;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
