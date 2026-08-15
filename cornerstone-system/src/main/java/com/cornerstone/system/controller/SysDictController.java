package com.cornerstone.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import com.cornerstone.system.service.SysDictService;
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

/** 字典管理接口（类型 + 数据）。 */
@RestController
@RequestMapping("/system/dict")
public class SysDictController {

    private final SysDictService dictService;

    public SysDictController(SysDictService dictService) {
        this.dictService = dictService;
    }

    // ---------------- 字典类型 ----------------

    /** 字典类型分页查询 */
    @GetMapping("/type/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<Page<SysDictType>> pageType(
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "dictName", required = false) String dictName,
            @RequestParam(name = "dictType", required = false) String dictType,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(dictService.pageType(current, size, dictName, dictType, status));
    }

    /** 新增字典类型 */
    @PostMapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<SysDictType> addType(@RequestBody SysDictType type) {
        return Result.success(dictService.addType(type));
    }

    /** 编辑字典类型 */
    @PutMapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<SysDictType> updateType(@RequestBody SysDictType type) {
        return Result.success(dictService.updateType(type));
    }

    /** 删除字典类型（含数据） */
    @DeleteMapping("/type/{typeId}")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public Result<Void> deleteType(@PathVariable(name = "typeId") Long typeId) {
        dictService.deleteType(typeId);
        return Result.success();
    }

    // ---------------- 字典数据 ----------------

    /** 按类型查字典数据（读缓存） */
    @GetMapping("/data/type/{dictType}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<SysDictData>> dataByType(@PathVariable(name = "dictType") String dictType) {
        return Result.success(dictService.listData(dictType));
    }

    /** 字典数据分页查询 */
    @GetMapping("/data/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<Page<SysDictData>> pageData(
            @RequestParam(name = "current", defaultValue = "1") long current,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "dictType", required = false) String dictType,
            @RequestParam(name = "dictLabel", required = false) String dictLabel,
            @RequestParam(name = "status", required = false) String status) {
        return Result.success(dictService.pageData(current, size, dictType, dictLabel, status));
    }

    /** 新增字典数据 */
    @PostMapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<SysDictData> addData(@RequestBody SysDictData data) {
        return Result.success(dictService.addData(data));
    }

    /** 编辑字典数据 */
    @PutMapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<SysDictData> updateData(@RequestBody SysDictData data) {
        return Result.success(dictService.updateData(data));
    }

    /** 删除字典数据 */
    @DeleteMapping("/data/{dataId}")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public Result<Void> deleteData(@PathVariable(name = "dataId") Long dataId) {
        dictService.deleteData(dataId);
        return Result.success();
    }
}
