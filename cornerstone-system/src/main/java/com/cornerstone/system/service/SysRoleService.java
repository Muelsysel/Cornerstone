package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysRole;
import java.util.List;
import java.util.Set;

/** 角色服务接口。 */
public interface SysRoleService {

    /** 分页查询角色（roleKey 为权限字符模糊过滤） */
    Page<SysRole> page(long current, long size, String roleName, String roleKey, String status);

    /** 按 ID 查询角色 */
    SysRole getById(Long roleId);

    /** 新增角色 */
    SysRole add(SysRole role);

    /** 编辑角色 */
    SysRole update(SysRole role);

    /** 删除角色 */
    void delete(Long roleId);

    /** 分配菜单权限 */
    void assignMenus(Long roleId, List<Long> menuIds);

    /** 查询角色拥有的菜单ID */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /** 查询角色自定义数据范围的部门ID集合 */
    List<Long> getDeptIdsByRoleId(Long roleId);

    /** 查询角色标识（供权限解析） */
    List<SysRole> listAll();

    /** 查询用户拥有的角色标识集合 */
    Set<String> getRoleKeysByUserId(Long userId);
}
