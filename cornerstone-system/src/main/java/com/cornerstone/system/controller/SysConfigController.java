package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysConfig;
import com.cornerstone.system.service.SysConfigService;
import java.util.HashMap;
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

/** 参数管理接口。 */
@RestController
@RequestMapping("/system/config")
public class SysConfigController {

    private final SysConfigService configService;

    public SysConfigController(SysConfigService configService) {
        this.configService = configService;
    }

    /** 参数分页查询 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<Page<SysConfig>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") long current,
            @RequestParam(name = "pageSize", defaultValue = "10") long size,
            @RequestParam(name = "configName", required = false) String configName,
            @RequestParam(name = "configKey", required = false) String configKey,
            @RequestParam(name = "configType", required = false) String configType) {
        return Result.success(configService.page(current, size, configName, configKey, configType));
    }

    /** 按键名查参数值（读缓存） */
    @GetMapping("/value/{configKey}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> valueByKey(
            @PathVariable(name = "configKey") String configKey) {
        Map<String, String> data = new HashMap<>();
        data.put(configKey, configService.getValueByKey(configKey));
        return Result.success(data);
    }

    /** 新增参数 */
    @PostMapping
    @OperLog(title = "参数管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:config:add')")
    public Result<SysConfig> add(@RequestBody SysConfig config) {
        return Result.success(configService.add(config));
    }

    /** 编辑参数 */
    @PutMapping
    @OperLog(title = "参数管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:config:edit')")
    public Result<SysConfig> update(@RequestBody SysConfig config) {
        return Result.success(configService.update(config));
    }

    /** 删除参数 */
    @DeleteMapping("/{configId}")
    @OperLog(title = "参数管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:config:remove')")
    public Result<Void> delete(@PathVariable(name = "configId") Long configId) {
        configService.delete(configId);
        return Result.success();
    }
}
