package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 菜单权限实体（目录/菜单/按钮三级）。 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型:M目录,C菜单,F按钮 */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识 */
    private String perms;

    /** 菜单图标 */
    private String icon;

    /** 显示顺序 */
    private Integer sort;

    /** 显示状态:0显示,1隐藏 */
    private String visible;

    /** 菜单状态:0正常,1停用 */
    private String status;

    /** 子菜单（树查询用，非表字段） */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
