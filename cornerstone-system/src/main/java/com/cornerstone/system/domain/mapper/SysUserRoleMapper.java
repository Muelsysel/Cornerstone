package com.cornerstone.system.domain.mapper;

import org.apache.ibatis.annotations.Mapper;

/** 用户-角色关联操作 Mapper。 关联表用 SQL 完成批处理，无需实体。 */
@Mapper
public interface SysUserRoleMapper {

    /** 删除用户拥有的全部角色 */
    int deleteUserRoleByUserId(Long userId);

    /** 为用户批量分配角色 */
    int batchInsertUserRole(Long userId, java.util.List<Long> roleIds);

    /** 查询用户拥有的角色标识 */
    java.util.Set<String> selectRoleKeysByUserId(Long userId);
}
