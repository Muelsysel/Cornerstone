package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 登录日志接口。 */
@Tag(name = "登录日志")
@RestController
@RequestMapping("/system/loginlog")
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    public SysLoginLogController(SysLoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /** 登录日志分页查询 */
    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<Page<SysLoginLog>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(loginLogService.page(pageNum, pageSize, username, status));
    }

    /** 删除单条登录日志 */
    @Operation(summary = "删除登录日志")
    @DeleteMapping("/{infoId}")
    @OperLog(title = "登录日志", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:log:remove')")
    public Result<Void> delete(@PathVariable(name = "infoId") Long infoId) {
        loginLogService.delete(infoId);
        return Result.success();
    }

    /** 清空登录日志 */
    @Operation(summary = "清空登录日志", description = "全表清空，不可恢复")
    @DeleteMapping("/clean")
    @OperLog(title = "登录日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("hasAuthority('system:log:remove')")
    public Result<Void> clean() {
        loginLogService.clean();
        return Result.success();
    }
}
