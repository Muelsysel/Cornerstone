package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.api.dto.UserDTO;
import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 用户管理接口。 GET /system/user/{userId} 实现 cornerstone-api 的 SystemUserClient 契约。 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    /** 分页查询用户（权限演示：需 system:user:list） */
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Page<SysUser>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(userService.page(pageNum, pageSize, username, status));
    }

    /** 按 ID 查询用户基础信息：SystemUserClient 契约 */
    @Operation(summary = "查询用户基础信息", description = "实现 cornerstone-api SystemUserClient 契约")
    @GetMapping("/{userId}")
    public Result<UserDTO> getById(@PathVariable(name = "userId") Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setDeptId(user.getDeptId());
        return Result.success(dto);
    }

    /** 查询登录用户信息及权限（供前端/网关获取当前用户能力）。仅允许查询本人，防 IDOR 越权。 */
    @Operation(summary = "查询本人信息", description = "仅允许查询自己，跨用户返回 403（防 IDOR）")
    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> info(@RequestParam(name = "userId") Long userId) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null || !ctx.getUserId().equals(userId)) {
            // 非本人：拒绝（返回业务 403，不泄露是否存在）
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询本人信息");
        }
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        return Result.success(data);
    }

    /** 新增用户 */
    @Operation(summary = "新增用户")
    @PostMapping
    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<SysUser> add(@RequestBody SysUser user) {
        return Result.success(userService.add(user));
    }

    /** 编辑用户 */
    @Operation(summary = "编辑用户")
    @PutMapping
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<SysUser> update(@RequestBody SysUser user) {
        return Result.success(userService.update(user));
    }

    /** 删除用户（逻辑删除） */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{userId}")
    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:user:remove')")
    public Result<Void> delete(@PathVariable(name = "userId") Long userId) {
        userService.delete(userId);
        return Result.success();
    }

    /** 启用/停用 */
    @Operation(summary = "启用/停用用户")
    @PutMapping("/{userId}/status")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> changeStatus(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "status") String status) {
        userService.changeStatus(userId, status);
        return Result.success();
    }

    /** 重置密码 */
    @Operation(summary = "重置用户密码")
    @PutMapping("/{userId}/password")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> resetPassword(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "password") String password) {
        userService.resetPassword(userId, password);
        return Result.success();
    }

    /** 分配角色（全量覆盖：先清后插） */
    @Operation(summary = "分配用户角色", description = "全量覆盖：先清后插")
    @PutMapping("/{userId}/roles")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> assignRoles(
            @PathVariable(name = "userId") Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success();
    }

    /** 查询用户已分配的角色 ID（分配弹窗回显） */
    @Operation(summary = "查询用户已分配角色 ID")
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Map<String, Object>> userRoles(@PathVariable(name = "userId") Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("roleIds", userService.getRoleIdsByUserId(userId));
        return Result.success(data);
    }
}
