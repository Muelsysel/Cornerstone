package com.cornerstone.system.controller;

import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.service.SysMenuService;
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

/** 菜单管理接口。 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final SysMenuService menuService;

    public SysMenuController(SysMenuService menuService) {
        this.menuService = menuService;
    }

    /** 菜单树查询 */
    @Operation(summary = "菜单树查询", description = "支持菜单名模糊与状态过滤")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<SysMenu>> tree(
            @RequestParam(name = "menuName", required = false) String menuName,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(menuService.listTree(menuName, status));
    }

    /** 新增菜单 */
    @Operation(summary = "新增菜单")
    @PostMapping
    @OperLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<SysMenu> add(@RequestBody SysMenu menu) {
        return Result.success(menuService.add(menu));
    }

    /** 编辑菜单 */
    @Operation(summary = "编辑菜单", description = "父节点不能选自己或自身子节点（防成环）")
    @PutMapping
    @OperLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<SysMenu> update(@RequestBody SysMenu menu) {
        return Result.success(menuService.update(menu));
    }

    /** 删除菜单 */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{menuId}")
    @OperLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public Result<Void> delete(@PathVariable(name = "menuId") Long menuId) {
        menuService.delete(menuId);
        return Result.success();
    }
}
