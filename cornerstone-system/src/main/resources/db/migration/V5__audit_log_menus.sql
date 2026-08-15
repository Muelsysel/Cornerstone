-- V5: 审计日志菜单权限点（修复 v1 缺口）
-- 日志功能（操作日志/登录日志）接口要求 system:log:list 权限，但 V2 seed 菜单树缺对应权限点 → 无角色可访问。
-- 本迁移补齐菜单并关联 admin 角色（admin 的 role_menu 在 V2 为动态 SELECT，新菜单需显式关联）。

-- 菜单：操作日志 / 登录日志（挂在"系统管理"目录 id=1 下）
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (1, '操作日志', 'C', 'operlog', 'system/operlog/index', 'system:log:list', 'document', 5, '0', '0', NOW()),
       (1, '登录日志', 'C', 'loginlog', 'system/loginlog/index', 'system:log:list', 'finished', 6, '0', '0', NOW());

-- admin（role_id=1）关联新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms = 'system:log:list';
