package com.cornerstone.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.core.Result;
import com.cornerstone.demo.domain.Announcement;
import com.cornerstone.demo.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告接口。 形态示范：查询（分页/详情）为<b>公开接口</b>，管理操作需认证且具备 {@code demo:announcement:edit} 权限（网关白名单放行 +
 * 资源服务器双保险）。
 */
@Tag(name = "公告管理")
@RestController
@RequestMapping("/demo/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** 公开接口：分页查询，无需登录（标题模糊 + 状态过滤） */
    @Operation(summary = "分页查询公告")
    @GetMapping("/page")
    public Result<Page<Announcement>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "status", required = false) Integer status) {
        return Result.success(announcementService.page(pageNum, pageSize, title, status));
    }

    /** 公开接口：详情，无需登录 */
    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<Announcement> detail(@PathVariable("id") Long id) {
        return Result.success(announcementService.getById(id));
    }

    /** 受保护接口：新增草稿，需编辑权限 */
    @Operation(summary = "新增公告")
    @PreAuthorize("hasAuthority('demo:announcement:edit')")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody Announcement announcement) {
        announcementService.create(announcement);
        return Result.success();
    }

    /** 受保护接口：编辑草稿，需编辑权限 */
    @Operation(summary = "编辑公告")
    @PreAuthorize("hasAuthority('demo:announcement:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable("id") Long id, @Validated @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementService.update(announcement);
        return Result.success();
    }

    /** 受保护接口：发布（草稿→已发布），需编辑权限 */
    @Operation(summary = "发布公告")
    @PreAuthorize("hasAuthority('demo:announcement:edit')")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable("id") Long id) {
        announcementService.publish(id);
        return Result.success();
    }

    /** 受保护接口：下线（已发布→已下线），需编辑权限 */
    @Operation(summary = "下线公告")
    @PreAuthorize("hasAuthority('demo:announcement:edit')")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable("id") Long id) {
        announcementService.offline(id);
        return Result.success();
    }

    /** 受保护接口：删除，需编辑权限 */
    @Operation(summary = "删除公告")
    @PreAuthorize("hasAuthority('demo:announcement:edit')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}
