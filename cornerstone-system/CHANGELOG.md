# Changelog — cornerstone-system

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.2.9] - 2026-08-15

- fix: `SysMenuServiceImpl.update` 补"父节点不能选自己或自身子孙"校验（与部门同级加固，防菜单树成环）；`SysMenuServiceImplTest` 补环检测用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysMenuServiceImplTest 10 用例）。

## [1.2.8] - 2026-08-15

- fix: `SysDeptServiceImpl.update` 补"父节点不能选自身子孙"校验（此前仅查自己，选子孙会成环 A→B→C→A 破坏树结构）；`SysDeptServiceImplTest` 补环检测用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysDeptServiceImplTest 10 用例）。

## [1.2.7] - 2026-08-15

- docs: CONTEXT.md 分页条目补 pageNum/pageSize 契约约定（1.5.8 回归修复的防复发记录）

**测试方法**：`mvn test -pl cornerstone-system`。

## [1.2.6] - 2026-08-15

- docs: SysAuthUserController 类注释同步 ADR-0007（内部令牌已实现，移除"生产环境需服务间认证"过时表述）

**测试方法**：`mvn test -pl cornerstone-system`。

## [1.2.5] - 2026-08-15

- test: `SysRoleServiceImplTest` 扩至 10 用例（新增 update 自定义数据范围重建 dept 关联、update 全局范围清空 dept 关联）

**测试方法**：`mvn test -pl cornerstone-system`（含 SysRoleServiceImplTest 10 用例）。

## [1.2.4] - 2026-08-15

- test: `SysUserServiceImplTest` 扩至 11 用例（新增 update 缺人拒绝/带密码编码/无密码保留原哈希、changeStatus 只补状态、resetPassword 编码更新）

**测试方法**：`mvn test -pl cornerstone-system`（含 SysUserServiceImplTest 11 用例）。

## [1.2.3] - 2026-08-15

- test: `SysProfileControllerTest` 5 用例（个人中心无上下文 401、返回当前用户、旧密码错误拒绝、用户不存在拒绝、改密只更新密码字段且传明文由服务层统一编码）

**测试方法**：`mvn test -pl cornerstone-system`（含 SysProfileControllerTest）。

## [1.2.2] - 2026-08-15

- test: `SysMenuServiceImplTest` 9 用例（树组装/过滤/增删改校验/子菜单删除保护）、`SysDeptServiceImplTest` 8 用例（树组装/ancestors 解析/父节点非法校验/子部门删除保护）、`SysLoginLogServiceImplTest` 5 用例（状态映射/分页条件/删除清空）、`SysOperLogServiceImplTest` 5 用例（记录/分页/删除清空）

**测试方法**：`mvn test -pl cornerstone-system`（含上述 27 个新增服务用例）。

## [1.2.1] - 2026-08-15

- test: `InternalTokenFilterTest` 4 用例（内部令牌正确放行/错误 401/缺失 401/非内部路径放行——安全逻辑回归）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest / SysRoleServiceImplTest / SysConfigServiceImplTest / SysDictServiceImplTest / InternalTokenFilterTest）。

## [1.2.0] - 2026-08-15

- perf: V10 迁移补查询/排序索引（日志 oper_time/oper_name、login_time/username、字典 dict_type、用户 dept_id、角色 role_key）——大日志表分页排序与数据权限/关联查询提速

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest / SysRoleServiceImplTest / SysConfigServiceImplTest / SysDictServiceImplTest）。

## [1.1.9] - 2026-08-15

- test: `SysDictServiceImplTest` 7 用例（列表缓存命中/回源写缓存/数据变更缓存失效/类型重名拒绝）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest / SysRoleServiceImplTest / SysConfigServiceImplTest / SysDictServiceImplTest）。

## [1.1.8] - 2026-08-15

- test: `SysConfigServiceImplTest` 8 用例（缓存命中/回源回填/null 不写缓存/更新清旧 key/删除清缓存——固化缓存一致性修复）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest / SysRoleServiceImplTest / SysConfigServiceImplTest）。

## [1.1.7] - 2026-08-15

- test: `SysRoleServiceImplTest` 8 用例（角色唯一性/数据范围自定义部门维护/内置角色保护/菜单分配/空列表仅删除）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest / SysRoleServiceImplTest）。

## [1.1.6] - 2026-08-15

- fix: 公告管理权限点缺失——V9 迁移补 `demo:announcement:edit` 菜单/按钮权限点 + admin role_menu（此前 admin JWT scope 缺该权限导致公告增删改 403）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest）。

## [1.1.5] - 2026-08-15

- feat: 操作/登录日志删除与清空——`DELETE /system/{operlog|loginlog}/{id}` 与 `/clean`（权限点 `system:log:remove`，V8 迁移补按钮权限点 + admin 关联）；BusinessType 补 CLEAN(8)
- fix: 补全前端已声明但后端缺失的日志删除/清空端点（消除契约缺口）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest）。

## [1.1.4] - 2026-08-15

- test: `SysUserServiceImplTest` 6 用例（密码编码/默认密码/重名拒绝/内置用户保护/角色分配委托/空列表仅删除）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest / SysUserServiceImplTest）。

## [1.1.3] - 2026-08-15

- feat: 用户分配角色——`PUT/GET /system/user/{userId}/roles`（全量覆盖，事务保证；补 selectRoleIdsByUserId 查询），消除用户管理无法维护角色关联的功能缺口

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest）。

## [1.1.2] - 2026-08-15

- test: `AuthUserSupportServiceTest`（认证核心组装：用户不存在/角色权限组装/空角色/菜单 perms 与状态过滤）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest / AuthUserSupportServiceTest）。

## [1.1.1] - 2026-08-15

- fix: 参数缓存一致性——`SysConfigServiceImpl.update` 先清旧 key 缓存再写新值（configKey 可变更），null 值不写缓存（避免读到过期旧值）
- refactor: RSA 公钥解析改用 common `RsaKeyUtils`（与 gateway/auth/demo 统一）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest）。

## [1.1.0] - 2026-08-15

- fix(安全): 操作日志脱敏——`OperLogAspect` 递归屏蔽 password/secret/token 字段（修改密码/用户管理不再把明文口令落库），补回归测试
- fix: 角色保存事务上移（`SysRoleServiceImpl.add/update` 加 `@Transactional`，消除 self-invocation 失效导致的脏数据风险）
- fix: 错误码枚举化——`SystemErrorCode` 补 1008-1012（内置用户/角色删除保护、子节点删除保护、旧密码错误），消除散落魔法码与 1003 冲突
- fix: `InternalTokenFilter` 令牌比对改恒定时间比较（MessageDigest.isEqual）
- test: `DataScopeServiceTest` 6 用例（最严格范围选取/匿名回退/自定义部门委托）

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest / DataScopeServiceTest / OperLogAspectTest）。

## [1.0.0] - 2026-08-15（初始）

- feat: RBAC（用户/角色/菜单/部门 CRUD + 权限注解）；字典/参数（Redis 缓存）；操作日志（@OperLog AOP）；登录日志（record + 查询）；认证支持接口（AuthUserClient/LoginLogClient 契约，/system/auth/** 内部令牌保护）；个人中心（GET/PUT /system/user/profile）
- feat: 部门数据权限（DataPermissionInterceptor + Handler，mappedStatementId 拦截 SysUserMapper，条件与 WHERE AND 合并；V4 data_scope + role_dept）
- fix: 实体主键 @JsonProperty（userId/roleId/menuId/deptId 契约对齐）；Mapper 双参数补 @Param；V5/V7 补菜单权限点（日志/字典/参数）；sys_dept 补审计列（V7）；SysUser.password @JsonIgnore
- test: 数据权限 handler 8 用例（各范围 SQL）、认证支持接口、安全权限、扩展功能

**测试方法**：`mvn test -pl cornerstone-system`（CornerstoneDataPermissionHandlerTest / SysAuthUserControllerTest / SystemSecurityTest / SystemExtensionTest）。
