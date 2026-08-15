# Changelog — cornerstone-system

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.2.41] - 2026-08-16

- fix(bug): 菜单父节点存在性校验——add/update 指定不存在的 parentId 此前生成悬空节点（树组装丢弃、后台不可见）；现与部门同级报 INVALID_PARENT；`SysMenuServiceImplTest` 新增 `addRejectsMissingParent`/`updateRejectsMissingParent`

**测试方法**：`mvn test -pl cornerstone-system`（154 用例）。

## [1.2.40] - 2026-08-16

- fix(security): **停用账号禁止登录**——`AuthUserSupportService.findByUsername` 查询强制过滤 `status='0'`，停用用户视为不存在（认证统一报「用户名或密码错误」，与游客访问非已发布公告按不存在处理同款 fail-closed，避免账号状态枚举）；`AuthUserSupportServiceTest` 新增 `disabledUserReturnsNull`
- fix(data): RBAC 各实体枚举字段补合法性校验（`ValidationUtils.oneOf`）——用户/部门/菜单/字典状态仅 0/1、菜单类型仅 M/C/F、菜单显示状态仅 0/1、参数类型仅 Y/N、字典默认值仅 Y/N、角色数据范围仅 1-5（此前非法值可入库；数据范围非法值会在权限解析时被 fail-closed 静默按「仅本人」收缩，配置错误难发现）；5 个 ServiceImplTest + ValidationUtilsTest 新增 10 个用例

**测试方法**：`mvn test -pl cornerstone-system`（152 用例）。

## [1.2.39] - 2026-08-16

- fix(bug): **RBAC 全部可写字符串字段补长度校验**（防 DB DataTruncation → 500）——参数（configName/configKey ≤100、configValue/remark ≤500）、字典（dictName/dictType/dictLabel/dictValue ≤100、remark ≤500）、菜单（menuName ≤50、path ≤200、component ≤255、perms/icon ≤100）、部门（leader/phone ≤30、email ≤50）、角色（remark ≤500）；新增 `ValidationUtils.maxLength` 统一入口（common），5 个 ServiceImplTest 共新增 11 个用例锁定

**测试方法**：`mvn test -pl cornerstone-system`（143 用例）。

## [1.2.38] - 2026-08-16

- fix(security): `changeStatus` 校验状态合法性（仅 0/1）——此前可传任意值污染 DB char(1) 列；`SysUserServiceImplTest` 新增 `changeStatusRejectsInvalidStatusValue`

**测试方法**：`mvn test -pl cornerstone-system`（132 用例）。

## [1.2.37] - 2026-08-16

- fix(contract): **主键 JSON 字段名对齐前端契约**——SysConfig/SysOperLog/SysLoginLog/SysDictType/SysDictData 补 `@JsonProperty`（configId/operId/infoId/dictId/dictCode）；此前响应为 `id`，前端读 `operId` 等为 null → 删除日志/字典等操作传 null id 失败；`SystemExtensionTest` 新增 `entityIdJsonContractUsesFrontendFieldNames` 锁定

**测试方法**：`mvn test -pl cornerstone-system`（131 用例）。

## [1.2.36] - 2026-08-16

- fix(bug): 部门名加长度校验（≤50，与 DB varchar(50) 一致）——超长曾触发 DataTruncation → 500；现返回友好 400；`SysDeptServiceImplTest` 新增 `addRejectsOversizedDeptName`；用户昵称同步补长度校验（≤30）

**测试方法**：`mvn test -pl cornerstone-system`（130 用例）。

## [1.2.35] - 2026-08-16

- fix(bug): 角色名称/标识加长度校验（roleName ≤30、roleKey ≤50，与 DB 列一致）——超长曾触发 DataTruncation → 500；现返回友好 400；`SysRoleServiceImplTest` 新增 `addRejectsOversizedRoleName`

**测试方法**：`mvn test -pl cornerstone-system`（129 用例）。

## [1.2.34] - 2026-08-16

- fix(bug): 用户名加长度上限（30 字符，与 DB varchar(30) 一致）——超长用户名曾触发 DataTruncation → 500；现业务层返回友好 400「用户名长度不能超过 30 个字符」；`SysUserServiceImplTest` 新增 `addRejectsOversizedUsername`

**测试方法**：`mvn test -pl cornerstone-system`（128 用例）。

## [1.2.33] - 2026-08-16

- fix(security): 密码契约统一 6-72——管理员重置密码补 `@Valid` + `@Size(6-72)`（此前仅 service 层 ≤72，无下限）；用户创建/更新/重置的 service 层校验补 <6 拒绝（弱密码防护）；`SysUserServiceImplTest` 新增 `addRejectsTooShortPassword`

**测试方法**：`mvn test -pl cornerstone-system`（127 用例）。

## [1.2.32] - 2026-08-16

- fix(security): 个人中心改密补 `@Valid` + 新密码 `@Size(min=6, max=72)`——此前改密仅 `@NotBlank` 且无 `@Valid`（校验不触发），绕过前端可提交 1 位弱密码；现与登录/创建契约一致（6-72 字符）

**测试方法**：`mvn test -pl cornerstone-system`（126 用例）。

## [1.2.31] - 2026-08-16

- fix(security): 资源服务器补 CORS 白名单配置（与网关 globalcors 一致）——此前直连 system 时即使标准预检（带 Access-Control-Request-Method）也被 Spring Security CORS 拒绝 403；现直连合法预检放行，生产 nginx 同源不受影响

**测试方法**：`mvn test -pl cornerstone-system`（126 用例）。

## [1.2.30] - 2026-08-16

- fix(security): 资源服务器放行 OPTIONS 预检——此前 CORS 预检（无凭据头）被 `authenticated()` 拦截返回 401，直连服务/跳过网关时跨域请求失败；`SystemSecurityTest` 新增 `optionsPreflight_shouldBePermittedWithoutToken`

**测试方法**：`mvn test -pl cornerstone-system`（126 用例）。

## [1.2.29] - 2026-08-16

- fix(security): 重置密码改为请求体传递——此前明文密码走 `@RequestParam` 进 URL，会被代理/网关访问日志与浏览器历史记录留存；现 `PUT /{userId}/password` 收 `{"password":...}` body，前端同步

**测试方法**：`mvn test -pl cornerstone-system`（125 用例）。

## [1.2.28] - 2026-08-16

- fix(security): `SysUser.password` 由 `@JsonIgnore` 改为 `@JsonProperty(WRITE_ONLY)`——此前 `@JsonIgnore` 同时阻止**反序列化**，前端创建/编辑用户填写的初始密码永远到不了 service（静默落到默认 123456）；现写入可接收、响应序列化仍忽略（防哈希泄露）。`SystemSecurityTest` 新增 `createUser_shouldBindPasswordToService`（密码透传断言）+ `userPage_shouldNotExposePasswordHash`（分页不泄露哈希）

**测试方法**：`mvn test -pl cornerstone-system`（125 用例）。

## [1.2.27] - 2026-08-16

- fix(security): 用户新增/编辑/重置密码统一校验 ≤72 字符——此前无限制，可创建超长密码但登录侧 `@Size(72)` 拒绝 → 用户永远无法登录（契约缺口）；`SysUserServiceImplTest` 新增 `addRejectsOversizedPassword`/`resetPasswordRejectsOversizedPassword`

**测试方法**：`mvn test -pl cornerstone-system`（123 用例）。

## [1.2.26] - 2026-08-16

- fix(bug): `changeStatus` 补用户存在性校验——此前对不存在 userId 静默成功（`updateById` 返回 0 被忽略），前端误报"启用成功"；现与 `resetPassword`/`assignRoles` 一致返回 USER_NOT_FOUND；`SysUserServiceImplTest` 新增 `changeStatusRejectsMissingUser` 回归用例

**测试方法**：`mvn test -pl cornerstone-system`（121 用例）。

## [1.2.25] - 2026-08-16

- fix(data): 分页拦截器开启 `overflow`——pageNum 超出总页数时自动回退最后一页，消除越界空页（URL 直跳/并发删除场景；与前端删除回退双保险）

**测试方法**：`mvn test -pl cornerstone-system`（120 用例）。

## [1.2.24] - 2026-08-16

- docs(api): 全部 10 个 controller 补 OpenAPI `@Tag`/`@Operation`（用户/角色/菜单/部门/字典/参数/操作日志/登录日志/个人中心/认证支持），Swagger UI 分组与端点描述完整（对齐 demo 活模板）

**测试方法**：`mvn test -pl cornerstone-system`（120 用例）。

## [1.2.23] - 2026-08-16

- refactor(contract): 分页 controller 参数变量名统一为 `pageNum`/`pageSize`（此前 `@RequestParam(name="pageNum") long current` 变量名与 HTTP 参数名不一致，易误读为契约分裂）；`SystemExtensionTest` 分页用例升级为参数透传断言（dict/type、config、operlog、loginlog 四个 page 端点锁定 `pageNum&pageSize` 契约，与 user 页一致）

**测试方法**：`mvn test -pl cornerstone-system`（120 用例）。

## [1.2.22] - 2026-08-15

- fix(data): 分页/列表排序全部加确定性 tiebreaker——`page`/`list` 由单键排序改为 `sort/time + id` 双键（SysLoginLog、SysOperLog、SysRole、SysDictData、SysMenu、SysDept），时间戳精确到秒/sort 可重复时顺序不再不确定，翻页不重不漏、树形结构稳定

**测试方法**：`mvn test -pl cornerstone-system`（120 用例）。

## [1.2.21] - 2026-08-15

- test: `SysUserServiceImplTest` 补 page 分页委托用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysUser 15 用例）。

## [1.2.20] - 2026-08-15

- test: `SysDeptServiceImplTest` 补 listTree 过滤参数传递用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysDept 12 用例）。

## [1.2.19] - 2026-08-15

- test: `SysRoleServiceImplTest` 补 getMenuIdsByRoleId/getRoleKeysByUserId 委托用例（查询委托 Mapper 全覆盖）

**测试方法**：`mvn test -pl cornerstone-system`（含 SysRole 14 用例）。

## [1.2.18] - 2026-08-15

- fix: `SysConfigServiceImpl.add`/`SysDictServiceImpl.addType`/`SysRoleServiceImpl.add` 补并发 DuplicateKeyException 兜底（count 预检查 + 唯一索引双保险，与 user add 一致；并发同 key 转业务错误而非裸 500）

**测试方法**：`mvn test -pl cornerstone-system`。

## [1.2.17] - 2026-08-15

- fix: `SysDictServiceImpl.updateData` 修改 dictType 时新旧类型缓存都清除（此前只清新类型 key，旧类型缓存残留过期数据）；`SysDictServiceImplTest` 补用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysDict 10 用例）。

## [1.2.16] - 2026-08-15

- fix: 删除菜单时清理 role_menu 关联（新增 `SysRoleMenuMapper.deleteRoleMenuByMenuId` + XML；此前删除菜单后角色权限关联留孤儿）；`SysMenuServiceImplTest` 补清理用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysMenu 10 用例）。

## [1.2.15] - 2026-08-15

- fix: 删除角色时补充清理 user_role 关联（新增 `SysUserRoleMapper.deleteUserRoleByRoleId` + XML；此前仅清理 role_menu/role_dept，user_role 留孤儿）

**测试方法**：`mvn test -pl cornerstone-system`（含 SysRole 12 用例）。

## [1.2.14] - 2026-08-15

- fix: 删除用户时清理 user_role 关联、删除角色时清理 role_menu/role_dept 关联（此前只删主记录，关联表留孤儿记录）；两测试类各补删除清理用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysUser 14 + SysRole 12 用例）。

## [1.2.13] - 2026-08-15

- fix: `SysConfigServiceImpl.update` 补 configKey 唯一性校验、`SysRoleServiceImpl.update` 补 roleKey 唯一性校验（与 user/dict 同级加固：改名撞名转业务错误 + 并发 DuplicateKeyException 兜底 + @Transactional）；两测试类各补 1 用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysConfig 9 + SysRole 11 用例）。

## [1.2.12] - 2026-08-15

- fix: `SysDictServiceImpl.updateType` 补 dictType 唯一性校验（改名撞已有类型此前裸 500，现转业务错误；排除自己 + DuplicateKeyException 并发兜底 + @Transactional）；`SysDictServiceImplTest` 补 2 用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysDictServiceImplTest 9 用例）。

## [1.2.11] - 2026-08-15

- fix: `SysUserServiceImpl.update` 补用户名唯一性校验（改名撞已有用户此前裸 500，现转业务错误；排除自己 + DuplicateKeyException 并发兜底 + @Transactional）；`SysUserServiceImplTest` 补 2 用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysUserServiceImplTest 13 用例）。

## [1.2.10] - 2026-08-15

- fix: `SysDeptServiceImpl.update` 移动部门后**级联更新子孙的 ancestors**（此前仅更新自身，子孙路径指向旧位置造成数据不一致）；`SysDeptServiceImplTest` 补级联用例

**测试方法**：`mvn test -pl cornerstone-system`（含 SysDeptServiceImplTest 11 用例）。

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
