package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.service.SysLoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 登录日志接口。 */
@RestController
@RequestMapping("/system/loginlog")
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    public SysLoginLogController(SysLoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /** 登录日志分页查询 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<Page<SysLoginLog>> page(
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(loginLogService.page(current, size, username, status));
    }
}
