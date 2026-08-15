package com.cornerstone.system.service;

import com.cornerstone.system.domain.entity.SysMenu;
import java.util.List;

/** 菜单服务接口。 */
public interface SysMenuService {

    /** 查询菜单树 */
    List<SysMenu> listTree(String menuName, String status);

    /** 新增菜单 */
    SysMenu add(SysMenu menu);

    /** 编辑菜单 */
    SysMenu update(SysMenu menu);

    /** 删除菜单 */
    void delete(Long menuId);
}
