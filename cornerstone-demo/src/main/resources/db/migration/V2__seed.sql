-- 种子公告：一条已发布、一条草稿，供演示分页/过滤/详情/状态流转
INSERT INTO announcement (title, content, status, publish_time, create_by, create_time, update_by, update_time, deleted)
VALUES ('欢迎使用 Cornerstone', '这是一条已发布的欢迎公告，用于演示公开详情与分页查询。', 1, NOW(), 'system', NOW(), 'system', NOW(), 0);

INSERT INTO announcement (title, content, status, create_by, create_time, update_by, update_time, deleted)
VALUES ('草稿：即将上线的功能', '这是一条草稿公告，用于演示"草稿→发布→下线"状态流转。', 0, 'system', NOW(), 'system', NOW(), 0);
