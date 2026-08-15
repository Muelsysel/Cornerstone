-- V4: 公告表查询索引（分页按 create_time 倒序）
ALTER TABLE announcement
    ADD INDEX idx_create_time (create_time);
