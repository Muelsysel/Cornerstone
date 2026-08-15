-- V4: 数据权限（ADR-0006 落地）
-- 角色增加数据范围；自定义范围关联部门表。

ALTER TABLE sys_role
    ADD COLUMN data_scope CHAR(1) DEFAULT '1' COMMENT '数据范围（1全部 2自定义 3本部门及以下 4本部门 5仅本人）';

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-部门关联（自定义数据范围）';

-- admin 角色保持全部数据范围
UPDATE sys_role SET data_scope = '1' WHERE role_key = 'admin';
