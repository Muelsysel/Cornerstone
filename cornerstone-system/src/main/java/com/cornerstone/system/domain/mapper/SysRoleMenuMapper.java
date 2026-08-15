package com.cornerstone.system.domain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 角色-菜单关联操作 Mapper。 关联表用 SQL 完成批处理，无需实体。 */
@Mapper
public interface SysRoleMenuMapper {

    /** 删除角色拥有的全部菜单 */
    int deleteRoleMenuByRoleId(Long roleId);

    /** 为角色批量分配菜单 */
    int batchInsertRoleMenu(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /** 查询角色拥有的菜单ID */
    List<Long> selectMenuIdsByRoleId(Long roleId);
}
