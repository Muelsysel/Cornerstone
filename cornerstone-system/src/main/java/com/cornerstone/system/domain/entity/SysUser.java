package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用户ID（JSON 契约对齐前端 userId） */
    @TableId(type = IdType.AUTO)
    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    private Long id;

    /** 登录账号 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /**
     * 密码(BCrypt哈希)。WRITE_ONLY：请求可接收明文密码（新增/编辑时绑定）， 响应序列化时忽略（含分页列表，防哈希泄露）；仅服务内部与认证支持接口使用。
     * 曾误用 @JsonIgnore——它同时阻止反序列化，导致前端填写的初始密码永远不生效（落到默认 123456）。
     */
    @com.fasterxml.jackson.annotation.JsonProperty(
            access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 部门ID */
    private Long deptId;

    /** 部门名称（列表展示用，非表字段，page 服务批量填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String deptName;

    /** 帐号状态:0正常,1停用 */
    private String status;
}
