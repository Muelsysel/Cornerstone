-- 公告表基线：演示模块建表范本（一服务一库 + Flyway 版本化）
-- 约定：id 自增主键；soft delete 用 deleted 字段配合 MyBatis-Plus 逻辑删除；
--       审计字段 create_by/create_time/update_by/update_time 由 MyMetaObjectHandler 自动填充。
CREATE TABLE announcement (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(100) NOT NULL COMMENT '公告标题',
    content       TEXT                  COMMENT '公告内容',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0草稿/1已发布/2已下线',
    publish_time  DATETIME              COMMENT '发布时间',
    create_by     VARCHAR(64)           COMMENT '创建人',
    create_time   DATETIME              COMMENT '创建时间',
    update_by     VARCHAR(64)           COMMENT '更新人',
    update_time   DATETIME              COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删/1已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='公告表';
