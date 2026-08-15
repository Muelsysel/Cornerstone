-- V9: 公告管理菜单权限点
-- 公告增删改接口要求 demo:announcement:edit（controller 注解），但菜单种子缺该权限点 → admin 的
-- JWT scope 无此权限导致 403。补齐菜单 + 按钮权限点 + admin role_menu（三处一致约定）。

INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (1, '公告管理', 'C', 'demo/announcement', 'demo/announcement/index', 'demo:announcement:edit', 'bell', 7, '0', '0', NOW());

-- 按钮权限点：发布/下线/编辑/删除共用 demo:announcement:edit（前端 v-permission 同名）
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
SELECT id, '发布/编辑/删除', 'F', '', '', 'demo:announcement:edit', '', 1, '0', '0', NOW()
FROM sys_menu
WHERE perms = 'demo:announcement:edit';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms = 'demo:announcement:edit';
