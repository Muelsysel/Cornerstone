-- V8: 日志清空权限点
-- 操作/登录日志新增删除与清空接口，需要独立权限点 system:log:remove（避免"可查看即可清空"语义过宽）。
-- 在操作日志/登录日志菜单（perms='system:log:list'）下各挂一个按钮权限点，并关联 admin 角色。

INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
SELECT id, '清空日志', 'F', '', '', 'system:log:remove', '', 1, '0', '0', NOW()
FROM sys_menu
WHERE perms = 'system:log:list';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms = 'system:log:remove';
