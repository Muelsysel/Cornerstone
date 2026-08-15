package com.cornerstone.system.domain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 角色-部门关联（自定义数据范围）操作 Mapper。 */
@Mapper
public interface SysRoleDeptMapper {

    /** 删除角色的自定义范围部门 */
    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 批量保存角色的自定义范围部门 */
    @Insert(
            "<script>"
                    + "INSERT INTO sys_role_dept(role_id, dept_id) VALUES "
                    + "<foreach collection='deptIds' item='deptId' separator=','>(#{roleId}, #{deptId})</foreach>"
                    + "</script>")
    int batchInsert(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);

    /** 查询角色关联的部门 ID 集合 */
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /** 按多个角色查询部门 ID 集合（去重） */
    @Select(
            "<script>"
                    + "SELECT DISTINCT dept_id FROM sys_role_dept WHERE role_id IN "
                    + "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach>"
                    + "</script>")
    List<Long> selectDeptIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
