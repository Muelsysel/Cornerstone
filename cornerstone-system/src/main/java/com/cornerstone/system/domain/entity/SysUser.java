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

    /** 密码(BCrypt哈希)。@JsonIgnore：禁止序列化到任何响应（含分页列表），仅服务内部与认证支持接口使用 */
    @com.fasterxml.jackson.annotation.JsonIgnore private String password;

    /** 部门ID */
    private Long deptId;

    /** 帐号状态:0正常,1停用 */
    private String status;
}
