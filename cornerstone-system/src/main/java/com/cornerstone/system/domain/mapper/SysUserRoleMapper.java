package com.cornerstone.system.domain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户-角色关联操作 Mapper。 关联表用 SQL 完成批处理，无需实体。 */
@Mapper
public interface SysUserRoleMapper {

    /** 删除用户拥有的全部角色 */
    int deleteUserRoleByUserId(Long userId);

    /** 为用户批量分配角色 */
    int batchInsertUserRole(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /** 查询用户拥有的角色标识 */
    java.util.Set<String> selectRoleKeysByUserId(Long userId);

    /** 查询用户已分配的角色 ID 集合（分配弹窗回显） */
    List<Long> selectRoleIdsByUserId(Long userId);
}
