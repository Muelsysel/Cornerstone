package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色权限字符串 */
    private String roleKey;

    /** 显示顺序 */
    private Integer sort;

    /** 角色状态:0正常,1停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 数据范围：1全部 2自定义 3本部门及以下 4本部门 5仅本人（默认 1 全部） */
    private String dataScope;

    /** 自定义数据范围的部门 ID 集合（非表字段，仅接收前端入参） */
    @TableField(exist = false)
    private List<Long> deptIds;
}
