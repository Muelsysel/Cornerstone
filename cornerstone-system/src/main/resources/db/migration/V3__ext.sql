-- ==========================================================
-- V3__ext.sql
-- 扩展表：字典、参数、操作日志、登录日志
-- ==========================================================

-- 字典类型表
CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    dict_name   VARCHAR(100) DEFAULT '' COMMENT '字典名称',
    dict_type   VARCHAR(100) DEFAULT '' COMMENT '字典类型',
    status      CHAR(1)      DEFAULT '0' COMMENT '状态:0正常,1停用',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='字典类型表';

-- 字典数据表
CREATE TABLE sys_dict_data (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    dict_type   VARCHAR(100) DEFAULT '' COMMENT '字典类型',
    dict_label  VARCHAR(100) DEFAULT '' COMMENT '字典标签',
    dict_value  VARCHAR(100) DEFAULT '' COMMENT '字典键值',
    dict_sort   INT         DEFAULT 0 COMMENT '显示顺序',
    status      CHAR(1)     DEFAULT '0' COMMENT '状态:0正常,1停用',
    is_default  CHAR(1)     DEFAULT 'N' COMMENT '是否默认:Y是,N否',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME    DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT     DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='字典数据表';

-- 参数配置表
CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    config_name  VARCHAR(100) DEFAULT '' COMMENT '参数名称',
    config_key   VARCHAR(100) DEFAULT '' COMMENT '参数键名',
    config_value VARCHAR(500) DEFAULT '' COMMENT '参数键值',
    config_type  CHAR(1)      DEFAULT 'N' COMMENT '系统内置:Y是,N否',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by    VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by    VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='参数配置表';

-- 操作日志表
CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    title          VARCHAR(50)  DEFAULT '' COMMENT '模块标题',
    business_type  INT          DEFAULT 0 COMMENT '业务类型:0其它,1新增,2修改,3删除',
    method         VARCHAR(200) DEFAULT '' COMMENT '方法名称',
    request_method VARCHAR(10)  DEFAULT '' COMMENT '请求方式',
    oper_name      VARCHAR(50)  DEFAULT '' COMMENT '操作人员',
    oper_url       VARCHAR(255) DEFAULT '' COMMENT '请求URL',
    oper_ip        VARCHAR(128) DEFAULT '' COMMENT '主机地址',
    oper_param     TEXT         COMMENT '请求参数',
    json_result    TEXT         COMMENT '返回参数',
    status         INT          DEFAULT 0 COMMENT '操作状态:0正常,1异常',
    error_msg      TEXT         COMMENT '错误消息',
    oper_time      DATETIME     DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='操作日志记录';

-- 登录日志表
CREATE TABLE sys_login_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    username   VARCHAR(50)  DEFAULT '' COMMENT '用户账号',
    ipaddr     VARCHAR(128) DEFAULT '' COMMENT '登录IP地址',
    status     CHAR(1)      DEFAULT '0' COMMENT '登录状态:0成功,1失败',
    msg        VARCHAR(255) DEFAULT '' COMMENT '提示消息',
    login_time DATETIME     DEFAULT NULL COMMENT '访问时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统访问记录';
