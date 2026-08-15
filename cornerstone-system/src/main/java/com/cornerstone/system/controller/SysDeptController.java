package com.cornerstone.system.controller;

import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysDept;
import com.cornerstone.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 部门管理接口。 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final SysDeptService deptService;

    public SysDeptController(SysDeptService deptService) {
        this.deptService = deptService;
    }

    /** 部门树查询 */
    @Operation(summary = "部门树查询", description = "支持部门名模糊与状态过滤")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<List<SysDept>> tree(
            @RequestParam(name = "deptName", required = false) String deptName,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(deptService.listTree(deptName, status));
    }

    /** 新增部门 */
    @Operation(summary = "新增部门")
    @PostMapping
    @OperLog(title = "部门管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:dept:add')")
    public Result<SysDept> add(@RequestBody SysDept dept) {
        return Result.success(deptService.add(dept));
    }

    /** 编辑部门 */
    @Operation(summary = "编辑部门", description = "父节点不能选自己或自身子节点（防成环）；移动时级联更新后代祖先链")
    @PutMapping
    @OperLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<SysDept> update(@RequestBody SysDept dept) {
        return Result.success(deptService.update(dept));
    }

    /** 删除部门 */
    @Operation(summary = "删除部门")
    @DeleteMapping("/{deptId}")
    @OperLog(title = "部门管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:dept:remove')")
    public Result<Void> delete(@PathVariable(name = "deptId") Long deptId) {
        deptService.delete(deptId);
        return Result.success();
    }
}
