package com.cornerstone.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;

/** 用户基础信息 DTO（契约示例）。 服务间传递用户信息必须使用本 DTO，禁止在各服务内复制定义。 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull private Long userId;

    @NotBlank private String username;

    private String nickname;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
