# CONTEXT — cornerstone-system（系统服务：RBAC + 字典/参数/日志）

## 职责

- **认证支持接口（支持 auth 登录）**：实现 `AuthUserClient` 契约，`GET /system/auth/user/{username}` 返回 `UserAuthDTO`（含 BCrypt 密码哈希、roleKey 角色、menu perms 权限），供 `cornerstone-auth` 登录换 JWT；并实现 `LoginLogClient` 契约，`POST /system/auth/login-log` 接收登录日志并经 `SysLoginLogService.record()` 落库。
- **RBAC 核心**：用户（user）、角色（role）、菜单（menu）、部门（dept）及其关联（user_role、role_menu）的完整管理。
  - 用户：分页、新增、编辑、删除（逻辑删除）、启用/停用、重置密码，实现 `SystemUserClient` 契约（`GET /system/user/{userId}`）。
  - 角色：分页、CRUD、分配菜单权限（写 role_menu）、**数据范围（data_scope）与自定义范围部门（role_dept）管理**。
  - 菜单：目录/菜单/按钮三级树查询与 CRUD。
  - 部门：树查询与 CRUD。
- **标准集扩展**：数据字典（类型/数据）、系统参数、操作日志（`@OperLog` AOP 自动记录）、登录日志（记录 + 查询）。
- **个人中心**：`GET/PUT /system/user/profile`——当前用户信息与修改密码（旧密码 BCrypt 验证后更新，用户自助，无需管理权限）。
- **资源服务器示范**：作为 OAuth2 资源服务器，双保险校验 JWT（RSA 公钥），方法级 `@PreAuthorize` 权限注解。
- **缓存示范**：字典数据、参数值写入 Redis（`cornerstone:dict:*`、`cornerstone:config:*`），Redis 不可用时降级直查库。

## 边界（本模块不做的事）

- **不签发达令牌**：令牌签发属 `cornerstone-auth`，本模块只校验、不创建。
- **数据权限管控范围**：数据范围（ADR-0006）当前仅作用于 `sys_user` 表查询（用户分页等）；业务表的行级权限按需扩展（在 `CornerstoneDataPermissionHandler` 增加受管表）。
- **不跨服务直连**：对外需要的数据经 `cornerstone-api` Feign 契约，禁止直连其他服务数据库。
- **不含业务模块**：公告等业务示例在 `cornerstone-demo`。
- **本模块不对外开放登录**：用户登录在 `cornerstone-auth` 的 `/login`，经 `AuthUserClient` 调本模块 `/system/auth/**` 内部接口取认证信息；登录日志的 `record()` 方法供登录流程调用，本模块无对外登录端点。

## 依赖与端口

| 项 | 值 |
| --- | --- |
| 端口 | 8082 |
| 数据库 | `cornerstone_system`（一服务一库）+ Flyway 版本化迁移 |
| 注册 | Nacos `spring.cloud.nacos.discovery.server-addr=localhost:8848` |
| 缓存 | Redis `localhost:6379`（字典/参数缓存键见 `CacheConstants`） |
| 安全 | 资源服务器，RSA 公钥位于 `application.yml → cornerstone.security.public-key` |

## 词汇表

| 术语 | 定义 |
| --- | --- |
| **用户**（User） | 一个可登录平台的账号，隶属某部门，状态 正常(0)/停用(1)，逻辑删除。 |
| **角色**（Role） | 一组权限的集合，有唯一 role_key（如 `admin`），可分配多个菜单。 |
| **菜单**（Menu） | 权限点，三类：目录(M)/菜单(C)/按钮(F)，按钮承载 `system:xxx:list` 等权限标识。按钮（F）是**叶子节点**，不能作为父节点挂子级（后端 `validateParent` 校验 + 前端父级选择器过滤），保证「目录/菜单/按钮」三级结构语义。 |
| **部门**（Dept） | 组织架构树节点，父节点可由 ancestors 追踪祖级链。 |
| **字典**（Dict） | 键值对配置：类型(dict_type)+数据(dict_data)，用于下拉统一维护。 |
| **参数**（Config） | 系统运行参数，键名唯一（config_key）。 |
| **操作日志**（OperLog） | 用户在受管操作上的审计记录，经 `@OperLog` 注解 + AOP 切面自动写入。 |
| **登录日志**（LoginLog） | 登录成功/失败记录，`SysLoginLogService.record()` 供 auth 登录流程经 `LoginLogClient` 契约投递后落库。 |
| **权限标识**（Permission） | 字符串形式的权限点，如 `system:user:list`，供 `@PreAuthorize("hasAuthority(...)")` 使用。 |
| **认证支持**（AuthSupport） | `/system/auth/**` 内部接口：按用户名提供认证信息（密码哈希+角色+权限），供 `cornerstone-auth` 登录换 JWT；v1 简化匿名（网关白名单不含 /system/** 已隔离），生产需服务间认证。 |
| **数据范围**（DataScope） | 角色级行级数据权限：1全部 2自定义 3本部门及以下 4本部门 5仅本人。经 `CornerstoneDataPermissionHandler`（MyBatis-Plus `DataPermissionInterceptor` 的回调）对 `sys_user` 查询自动追加条件，范围取用户所有角色中最严格者。**fail-closed**：无部门归属用户按「本部门/本部门及以下」范围时返回不可能条件（`dept_id=-1`），不越权看全部；JWT 携带 `deptId` claim，网关透传 `X-Cornerstone-Dept-Id`。 |
| **资源服务器** | 校验 JWT、拒绝未认证请求的安全角色（本模块与 auth/gateway 共用公钥）。 |
| **审计字段** | `create_by/create_time/update_by/update_time` 由 `MyMetaObjectHandler` 自动填充。 |

## 架构与实现要点

```
controller → service(业务规则) → mapper(MyBatis-Plus) → sys_* 表
     │             │
   Result<T>   BusinessException（SystemErrorCode）
     └── cornerstone-common 契约
```

- **分页**：MyBatis-Plus `Page` + `PaginationInnerInterceptor(MYSQL)`（`config/MybatisPlusConfig`，maxLimit=500）。**契约约定：查询参数一律 `pageNum`/`pageSize`**（前端统一命名，曾回归为 current/size 导致翻页失效，见根 CHANGELOG 1.5.8）；分页响应为 `{records,total,size,current,pages}`。
- **审计**：`config/MyMetaObjectHandler` 实现 `MetaObjectHandler`，取 `UserContextHolder` 当前用户，匿名回退空串。
- **逻辑删除**：`deleted` 字段 + MyBatis-Plus `logic-delete-config`（`application.yml`）。
- **权限**：`security/ResourceServerConfig` 校验 JWT（RSA 公钥），`JwtAuthenticationConverter` 关闭前缀，从 `scope` 声明解析权限；方法级 `@PreAuthorize`。`/system/auth/**` 与 `/actuator/**` 在公开白名单放行（前者为服务间内部接口，后者为健康检查）。
- **认证支持服务**：`service/AuthUserSupportService` 独立类实现认证信息查询（user→role→menu perms），不复用/修改 SysUserService/RoleService/MenuService；由 `controller/SysAuthUserController` 暴露（实现 `AuthUserClient` 契约），同控制器实现 `LoginLogClient` 契约（POST /login-log）转调 `SysLoginLogService.record`。
- **安全异常映射**：`config/ResourceServerExceptionAdvice` 将 `AccessDeniedException`→403、认证异常→401（避免被 common 兜底转成 200，与 demo 一致）。
- **操作日志 AOP**：`aspect/OperLogAspect` 拦截 `@OperLog`，记录方法、URL、IP、参数、结果、操作人（来自 `UserContextHolder`）。
- **缓存降级**：`util/JsonCache` 封装 Redis 读写并吞异常降级，保证无 Redis 时功能可用。

## 表与迁移

| 迁移 | 内容 |
| --- | --- |
| `V1__baseline.sql` | RBAC 核心表：sys_user/user_role/sys_role/sys_menu/role_menu/sys_dept，utf8mb4 |
| `V2__seed.sql` | 种子：admin 用户（bcrypt 哈希）、admin 超级角色、菜单树 |
| `V3__ext.sql` | 扩展表：sys_dict_type/sys_dict_data/sys_config/sys_oper_log/sys_login_log |
| `V4__data_scope.sql` | 部门数据权限：sys_role.data_scope + sys_role_dept |
| `V5__log_menus.sql` | 操作/登录日志的菜单与按钮权限点（+ admin role_menu） |
| `V6__demo_data_scope.sql` | 演示数据：test 用户与「仅本人」数据范围角色 |
| `V7__fix_audit_columns.sql` | 补 sys_dept 审计列；补字典/参数菜单权限点（+ admin role_menu） |
| `V8__log_remove_perm.sql` | 日志删除/清空权限点 `system:log:remove`（+ admin role_menu） |
| `V9__announcement_menu.sql` | 公告管理菜单与 `demo:announcement:edit` 权限点（+ admin role_menu，修复 admin scope 缺该权限导致的 403） |
| `V10__query_indexes.sql` | 查询/排序索引（日志时间/操作人/用户名、字典 dict_type、用户 dept_id、角色 role_key） |

## 测试策略

- **MockMvc 安全测试**（`SystemSecurityTest`）：覆盖无 token 401、带权限 200、无权限/缺权限 403。
- **MockMvc 扩展测试**（`SystemExtensionTest`）：字典/参数/操作日志/登录日志查询接口。
- **MockMvc 认证支持测试**（`SysAuthUserControllerTest`）：`/system/auth/user/{username}` 返回 UserAuthDTO（roles/permissions 正确、停用/空 perms 被过滤），Mapper 层 `@MockBean` 隔离、匿名访问无需 JWT。
- 服务层 `@MockBean` 隔离，不依赖真实 MySQL/Redis；`application-test.yml` 关闭 Nacos/Flyway，JWT 校验公钥切到测试密钥对（`src/test/resources/keys/test-private.pem`）。
- 测试 JWT 用 `src/test/resources/keys` 测试密钥对 RS256 签发，权限放 `scope` 声明。

## 目录速览

```
cornerstone-system/
├── CONTEXT.md
├── pom.xml
└── src/main/
    ├── java/com/cornerstone/system/
    │   ├── SystemApplication.java
    │   ├── annotation/       ← @OperLog
    │   ├── aspect/           ← OperLogAspect（操作日志 AOP）
    │   ├── config/           ← MybatisPlus / MyMetaObjectHandler / 安全异常映射
    │   ├── constant/         ← CacheConstants / BusinessType
    │   ├── controller/       ← user/role/menu/dept/dict/config/operlog/loginlog/auth(user)
    │   ├── domain/
    │   │   ├── entity/       ← BaseEntity + SysUser/Role/Menu/Dept/Dict*/Config/Log*
    │   │   └── mapper/       ← BaseMapper 子接口 + XML（user_role/role_menu）
    │   ├── exception/        ← SystemErrorCode（1000 起）
    │   ├── security/         ← ResourceServerConfig
    │   ├── service/(+impl)   ← 各管理服务 + AuthUserSupportService（认证支持）
    │   └── util/             ← JsonCache（Redis 缓存降级）
    └── resources/
        ├── application.yml
        ├── mapper/           ← SysUserRoleMapper.xml / SysRoleMenuMapper.xml
        └── db/migration/     ← V1/V2/V3 迁移
└── src/test/                 ← MockMvc 测试 + application-test.yml + 测试密钥
```

## 与 auth/gateway 的密钥对齐

本模块 `application.yml → cornerstone.security.public-key` 当前与 `cornerstone-auth`/`cornerstone-gateway` 共用同一对 RSA 密钥的公钥。
若 auth/gateway 更换密钥对，必须同步更新本文件的公钥三处，保证 JWT 可互相验证。
