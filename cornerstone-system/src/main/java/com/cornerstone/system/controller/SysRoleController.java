package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.service.SysRoleService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** 角色管理接口。 */
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    /** 分页查询角色 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Page<SysRole>> page(
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "roleName", required = false) String roleName,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(roleService.page(current, size, roleName, status));
    }

    /** 角色列表 */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.listAll());
    }

    /** 新增角色 */
    @PostMapping
    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<SysRole> add(@RequestBody SysRole role) {
        return Result.success(roleService.add(role));
    }

    /** 编辑角色 */
    @PutMapping
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<SysRole> update(@RequestBody SysRole role) {
        return Result.success(roleService.update(role));
    }

    /** 删除角色 */
    @DeleteMapping("/{roleId}")
    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:role:remove')")
    public Result<Void> delete(@PathVariable(name = "roleId") Long roleId) {
        roleService.delete(roleId);
        return Result.success();
    }

    /** 分配菜单权限 */
    @PutMapping("/{roleId}/menus")
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> assignMenus(
            @PathVariable(name = "roleId") Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return Result.success();
    }

    /** 查询角色拥有的菜单ID */
    @GetMapping("/{roleId}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Map<String, Object>> roleMenu(@PathVariable(name = "roleId") Long roleId) {
        Map<String, Object> data = new HashMap<>();
        data.put("menuIds", roleService.getMenuIdsByRoleId(roleId));
        return Result.success(data);
    }
}
