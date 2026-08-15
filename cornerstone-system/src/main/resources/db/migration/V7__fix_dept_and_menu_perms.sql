-- V7: 修复 sys_dept 审计列缺失 + 补字典/参数菜单权限点
-- 1) sys_dept 继承 BaseEntity（含 create_by/update_by 审计字段），但 V1 建表遗漏这两列
--    → MyBatis-Plus 全字段查询报 Unknown column 'create_by'，部门新增/查询 500
ALTER TABLE sys_dept
    ADD COLUMN create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者' AFTER status,
    ADD COLUMN update_by VARCHAR(64) DEFAULT NULL COMMENT '更新者' AFTER create_by;

-- 2) 字典/参数管理：controller 有 system:dict:* / system:config:* 权限注解，但 V2/V3 菜单树从未建对应权限点
--    → admin（权限=菜单 perms）也无权，字典/参数新增/编辑/删除全部 403。本迁移补齐（同 V5 日志修复模式）。
-- 菜单：字典管理 / 参数设置（挂"系统管理"目录 id=1 下）
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time)
VALUES (1, '字典管理', 'C', 'dict', 'system/dict/index', 'system:dict:list', 'collection', 7, '0', '0', NOW()),
       (1, '参数设置', 'C', 'config', 'system/config/index', 'system:config:list', 'setting', 8, '0', '0', NOW());

-- 按钮权限：字典（新增/编辑/删除/查询）、参数（新增/编辑/删除/查询）
INSERT INTO sys_menu (parent_id, menu_name, menu_type, perms, sort, visible, status, create_time)
SELECT id, '字典查询', 'F', 'system:dict:query', 1, '0', '0', NOW() FROM sys_menu WHERE menu_name = '字典管理'
UNION ALL SELECT id, '字典新增', 'F', 'system:dict:add', 2, '0', '0', NOW() FROM sys_menu WHERE menu_name = '字典管理'
UNION ALL SELECT id, '字典编辑', 'F', 'system:dict:edit', 3, '0', '0', NOW() FROM sys_menu WHERE menu_name = '字典管理'
UNION ALL SELECT id, '字典删除', 'F', 'system:dict:remove', 4, '0', '0', NOW() FROM sys_menu WHERE menu_name = '字典管理'
UNION ALL SELECT id, '参数查询', 'F', 'system:config:query', 1, '0', '0', NOW() FROM sys_menu WHERE menu_name = '参数设置'
UNION ALL SELECT id, '参数新增', 'F', 'system:config:add', 2, '0', '0', NOW() FROM sys_menu WHERE menu_name = '参数设置'
UNION ALL SELECT id, '参数编辑', 'F', 'system:config:edit', 3, '0', '0', NOW() FROM sys_menu WHERE menu_name = '参数设置'
UNION ALL SELECT id, '参数删除', 'F', 'system:config:remove', 4, '0', '0', NOW() FROM sys_menu WHERE menu_name = '参数设置';

-- admin（role_id=1）关联新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
WHERE perms IN ('system:dict:list','system:dict:query','system:dict:add','system:dict:edit','system:dict:remove',
                'system:config:list','system:config:query','system:config:add','system:config:edit','system:config:remove');
