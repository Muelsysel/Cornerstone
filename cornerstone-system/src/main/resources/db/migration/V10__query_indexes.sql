-- V10: 查询与排序索引
-- 日志分页按时间倒序 + 操作人/用户名筛选；字典按 dict_type 回源查询；数据权限按 dept_id 过滤；角色按 role_key 反查。
-- 注：role_key 用普通索引而非 UNIQUE——逻辑删除后允许重建同名角色（唯一性由服务层 add 校验保证）。

ALTER TABLE sys_oper_log
    ADD INDEX idx_oper_time (oper_time),
    ADD INDEX idx_oper_name (oper_name);

ALTER TABLE sys_login_log
    ADD INDEX idx_login_time (login_time),
    ADD INDEX idx_username (username);

ALTER TABLE sys_dict_data
    ADD INDEX idx_dict_type (dict_type);

ALTER TABLE sys_user
    ADD INDEX idx_dept_id (dept_id);

ALTER TABLE sys_role
    ADD INDEX idx_role_key (role_key);
