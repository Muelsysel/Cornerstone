package com.cornerstone.api.dto;

import java.io.Serializable;
import java.util.Set;

/**
 * 认证所需用户信息 DTO（契约）。
 *
 * <p>由 system 提供、auth 消费：认证中心据此校验密码（BCrypt）并签发带角色权限的 JWT。 注意：本 DTO 含密码哈希，仅限服务间内部传递，禁止出现在对外接口响应中。
 */
public class UserAuthDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（签发为 JWT sub） */
    private Long userId;

    /** 用户名 */
    private String username;

    /** BCrypt 密码哈希（仅服务间传递） */
    private String password;

    /** 部门 ID */
    private Long deptId;

    /** 角色标识集合（roleKey，签发为 JWT roles 声明） */
    private Set<String> roles;

    /** 权限点集合（菜单 perms，签发为 JWT scope 声明，供 @PreAuthorize 匹配） */
    private Set<String> permissions;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
