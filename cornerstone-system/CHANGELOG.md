# Changelog — cornerstone-system

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.0] - 2026-08-15（初始）

- feat: RBAC（用户/角色/菜单/部门 CRUD + 权限注解）；字典/参数（Redis 缓存）；操作日志（@OperLog AOP）；登录日志（record + 查询）；认证支持接口（AuthUserClient/LoginLogClient 契约，/system/auth/** 内部令牌保护）；个人中心（GET/PUT /system/user/profile）
- feat: 部门数据权限（DataPermissionInterceptor + Handler，mappedStatementId 拦截 SysUserMapper，条件与 WHERE AND 合并；V4 data_scope + role_dept）
- fix: 实体主键 @JsonProperty（userId/roleId/menuId/deptId 契约对齐）；Mapper 双参数补 @Param；V5/V7 补菜单权限点（日志/字典/参数）；sys_dept 补审计列（V7）；SysUser.password @JsonIgnore
- test: 数据权限 handler 8 用例（各范围 SQL）、认证支持接口、安全权限、扩展功能

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest）。
