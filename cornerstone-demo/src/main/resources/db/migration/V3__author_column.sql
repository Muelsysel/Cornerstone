-- V3: 公告作者列
-- 公告列表展示作者（创建时由 AnnouncementServiceImpl 自动填充当前用户名）。
ALTER TABLE announcement
    ADD COLUMN author VARCHAR(64) NULL COMMENT '作者（创建时自动填充当前用户名）' AFTER status;
