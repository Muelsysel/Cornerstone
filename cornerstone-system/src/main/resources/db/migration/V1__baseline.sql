-- ==========================================================
-- V1__baseline.sql
-- cornerstone_system 基线：RBAC 核心表结构
-- ==========================================================

-- 用户表
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(30)  NOT NULL COMMENT '登录账号',
    nickname    VARCHAR(30)  DEFAULT NULL COMMENT '用户昵称',
    password    VARCHAR(100) DEFAULT NULL COMMENT '密码(BCrypt哈希)',
    dept_id     BIGINT       DEFAULT NULL COMMENT '部门ID',
    status      CHAR(1)      DEFAULT '0' COMMENT '帐号状态:0正常,1停用',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户信息表';

-- 角色表
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name   VARCHAR(30) NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50) NOT NULL COMMENT '角色权限字符串',
    sort        INT         DEFAULT 0 COMMENT '显示顺序',
    status      CHAR(1)     DEFAULT '0' COMMENT '角色状态:0正常,1停用',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME    DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT     DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色信息表';

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户和角色关联表';

-- 菜单权限表（目录/菜单/按钮三级）
CREATE TABLE sys_menu (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id   BIGINT      DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type   CHAR(1)     DEFAULT 'M' COMMENT '菜单类型:M目录,C菜单,F按钮',
    path        VARCHAR(200) DEFAULT '' COMMENT '路由地址',
    component   VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    icon        VARCHAR(100) DEFAULT '' COMMENT '菜单图标',
    sort        INT         DEFAULT 0 COMMENT '显示顺序',
    visible     CHAR(1)     DEFAULT '0' COMMENT '显示状态:0显示,1隐藏',
    status      CHAR(1)     DEFAULT '0' COMMENT '菜单状态:0正常,1停用',
    create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='菜单权限表';

-- 角色-菜单关联表
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色和菜单关联表';

-- 部门表（树）
CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父部门ID',
    dept_name   VARCHAR(50)  NOT NULL COMMENT '部门名称',
    ancestors   VARCHAR(255) DEFAULT '' COMMENT '祖级列表',
    sort        INT          DEFAULT 0 COMMENT '显示顺序',
    leader      VARCHAR(30)  DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(30)  DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(50)  DEFAULT NULL COMMENT '邮箱',
    status      CHAR(1)      DEFAULT '0' COMMENT '部门状态:0正常,1停用',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除:0存在,1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='部门表';
