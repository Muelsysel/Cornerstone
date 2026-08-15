# CONTEXT — cornerstone-system（系统服务：RBAC + 字典/参数/日志）

## 职责

- **RBAC 核心**：用户（user）、角色（role）、菜单（menu）、部门（dept）及其关联（user_role、role_menu）的完整管理。
  - 用户：分页、新增、编辑、删除（逻辑删除）、启用/停用、重置密码，实现 `SystemUserClient` 契约（`GET /system/user/{userId}`）。
  - 角色：分页、CRUD、分配菜单权限（写 role_menu）。
  - 菜单：目录/菜单/按钮三级树查询与 CRUD。
  - 部门：树查询与 CRUD。
- **标准集扩展**：数据字典（类型/数据）、系统参数、操作日志（`@OperLog` AOP 自动记录）、登录日志（记录 + 查询）。
- **资源服务器示范**：作为 OAuth2 资源服务器，双保险校验 JWT（RSA 公钥），方法级 `@PreAuthorize` 权限注解。
- **缓存示范**：字典数据、参数值写入 Redis（`cornerstone:dict:*`、`cornerstone:config:*`），Redis 不可用时降级直查库。

## 边界（本模块不做的事）

- **不签发达令牌**：令牌签发属 `cornerstone-auth`，本模块只校验、不创建。
- **不做数据权限（部门级）**：v2 ADR 后引入。
- **不跨服务直连**：对外需要的数据经 `cornerstone-api` Feign 契约，禁止直连其他服务数据库。
- **不含业务模块**：公告等业务示例在 `cornerstone-demo`。
- **v1 无用户登录流程**：登录日志的 `record()` 方法已提供，v2 登录服务调用；v1 无对外登录接口。

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
| **菜单**（Menu） | 权限点，三类：目录(M)/菜单(C)/按钮(F)，按钮承载 `system:xxx:list` 等权限标识。 |
| **部门**（Dept） | 组织架构树节点，父节点可由 ancestors 追踪祖级链。 |
| **字典**（Dict） | 键值对配置：类型(dict_type)+数据(dict_data)，用于下拉统一维护。 |
| **参数**（Config） | 系统运行参数，键名唯一（config_key）。 |
| **操作日志**（OperLog） | 用户在受管操作上的审计记录，经 `@OperLog` 注解 + AOP 切面自动写入。 |
| **登录日志**（LoginLog） | 登录成功/失败记录，`SysLoginLogService.record()` 供 v2 登录流程调用。 |
| **权限标识**（Permission） | 字符串形式的权限点，如 `system:user:list`，供 `@PreAuthorize("hasAuthority(...)")` 使用。 |
| **资源服务器** | 校验 JWT、拒绝未认证请求的安全角色（本模块与 auth/gateway 共用公钥）。 |
| **审计字段** | `create_by/create_time/update_by/update_time` 由 `MyMetaObjectHandler` 自动填充。 |

## 架构与实现要点

```
controller → service(业务规则) → mapper(MyBatis-Plus) → sys_* 表
     │             │
   Result<T>   BusinessException（SystemErrorCode）
     └── cornerstone-common 契约
```

- **分页**：MyBatis-Plus `Page` + `PaginationInnerInterceptor(MYSQL)`（`config/MybatisPlusConfig`）。
- **审计**：`config/MyMetaObjectHandler` 实现 `MetaObjectHandler`，取 `UserContextHolder` 当前用户，匿名回退空串。
- **逻辑删除**：`deleted` 字段 + MyBatis-Plus `logic-delete-config`（`application.yml`）。
- **权限**：`security/ResourceServerConfig` 校验 JWT（RSA 公钥），`JwtAuthenticationConverter` 关闭前缀，从 `scope` 声明解析权限；方法级 `@PreAuthorize`。
- **安全异常映射**：`config/ResourceServerExceptionAdvice` 将 `AccessDeniedException`→403、认证异常→401（避免被 common 兜底转成 200，与 demo 一致）。
- **操作日志 AOP**：`aspect/OperLogAspect` 拦截 `@OperLog`，记录方法、URL、IP、参数、结果、操作人（来自 `UserContextHolder`）。
- **缓存降级**：`util/JsonCache` 封装 Redis 读写并吞异常降级，保证无 Redis 时功能可用。

## 表与迁移

| 迁移 | 内容 |
| --- | --- |
| `V1__baseline.sql` | RBAC 核心表：sys_user/user_role/sys_role/sys_menu/role_menu/sys_dept，utf8mb4 |
| `V2__seed.sql` | 种子：admin 用户（bcrypt 哈希）、admin 超级角色、菜单树 |
| `V3__ext.sql` | 扩展表：sys_dict_type/sys_dict_data/sys_config/sys_oper_log/sys_login_log |

## 测试策略

- **MockMvc 安全测试**（`SystemSecurityTest`）：覆盖无 token 401、带权限 200、无权限/缺权限 403。
- **MockMvc 扩展测试**（`SystemExtensionTest`）：字典/参数/操作日志/登录日志查询接口。
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
    │   ├── controller/       ← user/role/menu/dept/dict/config/operlog/loginlog
    │   ├── domain/
    │   │   ├── entity/       ← BaseEntity + SysUser/Role/Menu/Dept/Dict*/Config/Log*
    │   │   └── mapper/       ← BaseMapper 子接口 + XML（user_role/role_menu）
    │   ├── exception/        ← SystemErrorCode（1000 起）
    │   ├── security/         ← ResourceServerConfig
    │   ├── service/(+impl)   ← 各管理服务
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
