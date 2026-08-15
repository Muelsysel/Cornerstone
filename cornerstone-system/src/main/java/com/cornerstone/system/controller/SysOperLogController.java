package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志接口。 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/system/operlog")
public class SysOperLogController {

    private final SysOperLogService operLogService;

    public SysOperLogController(SysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    /** 操作日志分页查询 */
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<Page<SysOperLog>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "operName", required = false) String operName,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "beginTime", required = false) String beginTime,
            @RequestParam(name = "endTime", required = false) String endTime) {
        return Result.success(
                operLogService.page(
                        pageNum, pageSize, title, operName, status, beginTime, endTime));
    }

    /** 删除单条操作日志 */
    @Operation(summary = "删除操作日志")
    @DeleteMapping("/{operId}")
    @OperLog(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:log:remove')")
    public Result<Void> delete(@PathVariable(name = "operId") Long operId) {
        operLogService.delete(operId);
        return Result.success();
    }

    /** 清空操作日志 */
    @Operation(summary = "清空操作日志", description = "全表清空，不可恢复")
    @DeleteMapping("/clean")
    @OperLog(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("hasAuthority('system:log:remove')")
    public Result<Void> clean() {
        operLogService.clean();
        return Result.success();
    }
}
