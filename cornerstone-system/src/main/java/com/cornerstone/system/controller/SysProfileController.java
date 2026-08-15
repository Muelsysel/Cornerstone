package com.cornerstone.system.controller;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysDept;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysDeptMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口：当前用户信息与修改密码（用户自助，无需管理权限）。
 *
 * <p>当前用户取自 {@link UserContextHolder}（网关透传头）。修改密码验证旧密码（BCrypt）后更新， 不要求管理员权限——任何已登录用户可自助改密。
 */
@Tag(name = "个人中心")
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController {

    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SysDeptMapper deptMapper;

    public SysProfileController(
            SysUserService userService, PasswordEncoder passwordEncoder, SysDeptMapper deptMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.deptMapper = deptMapper;
    }

    /** 当前登录用户信息（含部门名，列表/个人中心展示用） */
    @Operation(summary = "查询当前用户信息")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<SysUser> profile() {
        SysUser user = userService.getById(requireUserId());
        enrichDeptName(user);
        return Result.success(user);
    }

    /** 单条回填部门名（复用 SysUser.deptName 非表字段） */
    private void enrichDeptName(SysUser user) {
        if (user == null || user.getDeptId() == null) {
            return;
        }
        SysDept dept = deptMapper.selectById(user.getDeptId());
        if (dept != null) {
            user.setDeptName(dept.getDeptName());
        }
    }

    /** 修改当前用户密码：验证旧密码后更新 */
    @Operation(summary = "修改当前用户密码", description = "验证旧密码（BCrypt）后更新，任意已登录用户可自助改密")
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @OperLog(title = "个人中心", businessType = BusinessType.UPDATE)
    public Result<Void> updatePassword(
            @jakarta.validation.Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = requireUserId();
        SysUser user = userService.getById(userId);
        if (user == null || !passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(SystemErrorCode.OLD_PASSWORD_ERROR);
        }
        SysUser update = new SysUser();
        update.setId(userId);
        // 传明文新密码，由 SysUserServiceImpl.update 统一 BCrypt 编码（避免双重编码）
        update.setPassword(request.newPassword());
        userService.update(update);
        return Result.success();
    }

    private Long requireUserId() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ctx.getUserId();
    }

    /** 修改密码请求体 */
    public record PasswordUpdateRequest(
            @NotBlank(message = "旧密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空")
                    @jakarta.validation.constraints.Size(
                            min = 6,
                            max = 72,
                            message = "新密码长度需在 6-72 个字符之间")
                    String newPassword) {}
}
