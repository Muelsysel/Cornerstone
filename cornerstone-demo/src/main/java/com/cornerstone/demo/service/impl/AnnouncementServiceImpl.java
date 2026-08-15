package com.cornerstone.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.demo.domain.Announcement;
import com.cornerstone.demo.domain.AnnouncementStatus;
import com.cornerstone.demo.domain.DemoErrorCode;
import com.cornerstone.demo.mapper.AnnouncementMapper;
import com.cornerstone.demo.service.AnnouncementService;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 公告服务实现。业务规则集中在此：状态流转合法性、必填校验。 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    @Override
    public Page<Announcement> page(int pageNum, int pageSize, String title, Integer status) {
        // 游客（无用户上下文）强制只看已发布——无论是否显式传 status 都覆盖，
        // 否则传 status=0 可枚举全部草稿（此前仅在 status==null 时兜底，可被绕过）。
        // 已登录用户（管理后台）可按状态查看（草稿/发布/下线）。
        if (UserContextHolder.get() == null) {
            status = AnnouncementStatus.PUBLISHED.getCode();
        }
        LambdaQueryWrapper<Announcement> wrapper =
                new LambdaQueryWrapper<Announcement>()
                        .like(StringUtils.hasText(title), Announcement::getTitle, title)
                        .eq(status != null, Announcement::getStatus, status)
                        // 确定性排序：时间精确到秒，同一秒多条时按 id 倒序兜底，保证翻页不重不漏
                        .orderByDesc(Announcement::getCreateTime)
                        .orderByDesc(Announcement::getId);
        return baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Announcement getById(Long id) {
        Announcement announcement = baseMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(DemoErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }
        return announcement;
    }

    @Override
    public Announcement create(Announcement announcement) {
        validateTitle(announcement.getTitle());
        // 新建即草稿态
        announcement.setStatus(AnnouncementStatus.DRAFT.getCode());
        // 作者自动取自网关透传的当前用户（与审计列一致），避免前端传参伪造
        UserContext context = UserContextHolder.get();
        if (context != null && StringUtils.hasText(context.getUsername())) {
            announcement.setAuthor(context.getUsername());
        }
        save(announcement);
        return announcement;
    }

    @Override
    public void update(Announcement announcement) {
        validateTitle(announcement.getTitle());
        Announcement current = getById(announcement.getId());
        // 业务规则：只有草稿允许编辑内容；已发布/已下线只能走状态流转
        if (!Objects.equals(current.getStatus(), AnnouncementStatus.DRAFT.getCode())) {
            throw new BusinessException(DemoErrorCode.ANNOUNCEMENT_STATUS_ILLEGAL, "仅草稿状态的公告允许编辑");
        }
        current.setTitle(announcement.getTitle());
        current.setContent(announcement.getContent());
        updateById(current);
    }

    @Override
    public void publish(Long id) {
        Announcement announcement = getById(id);
        ensureStatus(announcement, AnnouncementStatus.DRAFT, "仅草稿状态可发布");
        announcement.setStatus(AnnouncementStatus.PUBLISHED.getCode());
        announcement.setPublishTime(LocalDateTime.now());
        updateById(announcement);
    }

    @Override
    public void offline(Long id) {
        Announcement announcement = getById(id);
        // 下线是"已发布"态专属动作
        ensureStatus(announcement, AnnouncementStatus.PUBLISHED, "仅已发布状态可下线");
        announcement.setStatus(AnnouncementStatus.OFFLINE.getCode());
        updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        removeById(id);
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(DemoErrorCode.ANNOUNCEMENT_TITLE_REQUIRED);
        }
    }

    private void ensureStatus(
            Announcement announcement, AnnouncementStatus expected, String reason) {
        if (!Objects.equals(announcement.getStatus(), expected.getCode())) {
            throw new BusinessException(DemoErrorCode.ANNOUNCEMENT_STATUS_ILLEGAL, reason);
        }
    }
}
