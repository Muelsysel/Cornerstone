-- ==========================================================
-- V2__seed.sql
-- 种子数据：admin 用户、超级管理员角色、菜单树
-- admin 密码为 bcrypt('admin123')
-- ==========================================================

-- 部门根节点
INSERT INTO sys_dept (id, parent_id, dept_name, ancestors, sort, leader, status, create_time)
VALUES (100, 0, 'Cornerstone', '0', 0, 'admin', '0', NOW());

-- 用户表：admin 超级管理员
-- 密码哈希：$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S  <=> bcrypt('admin123')
INSERT INTO sys_user (id, username, nickname, password, dept_id, status, create_by, create_time)
VALUES (1, 'admin', '超级管理员', '$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S', 100, '0', 'system', NOW());

-- 超级管理员角色
INSERT INTO sys_role (id, role_name, role_key, sort, status, remark, create_by, create_time)
VALUES (1, '超级管理员', 'admin', 1, '0', '内置超级管理员角色,拥有全部权限', 'system', NOW());

-- admin 用户分配超级管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 菜单树
-- 目录：系统管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (1, 0, '系统管理', 'M', '/system', NULL, NULL, 'system', 1, '0', '0', NOW());

-- 菜单：用户管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (100, 1, '用户管理', 'C', 'user', 'system/user/index', 'system:user:list', 'user', 1, '0', '0', NOW());
-- 菜单：角色管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (200, 1, '角色管理', 'C', 'role', 'system/role/index', 'system:role:list', 'peoples', 2, '0', '0', NOW());
-- 菜单：菜单管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (300, 1, '菜单管理', 'C', 'menu', 'system/menu/index', 'system:menu:list', 'tree-table', 3, '0', '0', NOW());
-- 菜单：部门管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (400, 1, '部门管理', 'C', 'dept', 'system/dept/index', 'system:dept:list', 'tree', 4, '0', '0', NOW());

-- 按钮权限：用户管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, perms, sort, visible, status, create_time)
VALUES (100, '用户查询', 'F', 'system:user:query', 1, '0', '0', NOW()),
       (100, '用户新增', 'F', 'system:user:add', 2, '0', '0', NOW()),
       (100, '用户编辑', 'F', 'system:user:edit', 3, '0', '0', NOW()),
       (100, '用户删除', 'F', 'system:user:remove', 4, '0', '0', NOW());
-- 按钮权限：角色管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, perms, sort, visible, status, create_time)
VALUES (200, '角色查询', 'F', 'system:role:query', 1, '0', '0', NOW()),
       (200, '角色新增', 'F', 'system:role:add', 2, '0', '0', NOW()),
       (200, '角色编辑', 'F', 'system:role:edit', 3, '0', '0', NOW()),
       (200, '角色删除', 'F', 'system:role:remove', 4, '0', '0', NOW());
-- 按钮权限：菜单管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, perms, sort, visible, status, create_time)
VALUES (300, '菜单新增', 'F', 'system:menu:add', 1, '0', '0', NOW()),
       (300, '菜单编辑', 'F', 'system:menu:edit', 2, '0', '0', NOW()),
       (300, '菜单删除', 'F', 'system:menu:remove', 3, '0', '0', NOW());
-- 按钮权限：部门管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, perms, sort, visible, status, create_time)
VALUES (400, '部门新增', 'F', 'system:dept:add', 1, '0', '0', NOW()),
       (400, '部门编辑', 'F', 'system:dept:edit', 2, '0', '0', NOW()),
       (400, '部门删除', 'F', 'system:dept:remove', 3, '0', '0', NOW());

-- 超级管理员角色分配所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;
