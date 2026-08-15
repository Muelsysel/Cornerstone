package com.cornerstone.system.service;

import com.cornerstone.system.domain.entity.SysDept;
import java.util.List;

/** 部门服务接口。 */
public interface SysDeptService {

    /** 查询部门树 */
    List<SysDept> listTree(String deptName, String status);

    /** 新增部门 */
    SysDept add(SysDept dept);

    /** 编辑部门 */
    SysDept update(SysDept dept);

    /** 删除部门 */
    void delete(Long deptId);
}
