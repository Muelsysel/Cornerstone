-- V6: 数据权限演示数据（端到端验证）
-- 新增：测试部门（与 admin 不同部门）、仅本人数据范围角色、测试用户
-- 用途：admin（全部范围）查用户分页见全部；test（仅本人）只见自己

-- 测试部门：挂在 Cornerstone(100) 下
INSERT INTO sys_dept (id, parent_id, dept_name, ancestors, sort, leader, phone, email, status, create_time)
VALUES (200, 100, '测试部门', '0,100', 2, NULL, NULL, NULL, '0', NOW());

-- 测试角色：仅本人数据范围（data_scope=5）
INSERT INTO sys_role (id, role_name, role_key, sort, status, data_scope, remark, create_time)
VALUES (2, '测试角色', 'test', 2, '0', '5', '演示数据权限：仅本人', NOW());

-- 测试用户：test（密码 admin123，与 admin 相同 BCrypt 哈希），隶属测试部门
INSERT INTO sys_user (id, username, nickname, password, dept_id, status, create_time)
VALUES (2, 'test', '测试用户', '$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S', 200, '0', NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);

-- 测试角色授予用户管理相关权限（system:user:list 等），使其可查用户分页
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE perms LIKE 'system:user:%' OR menu_name = '用户管理';
