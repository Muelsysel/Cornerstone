package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysOperLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志接口。 */
@RestController
@RequestMapping("/system/operlog")
public class SysOperLogController {

    private final SysOperLogService operLogService;

    public SysOperLogController(SysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    /** 操作日志分页查询 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<Page<SysOperLog>> page(
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "operName", required = false) String operName,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(operLogService.page(current, size, title, operName, status));
    }
}
