# CONTEXT — cornerstone-demo（演示模块：公告管理）

> 本模块是 Cornerstone 的**新模块活模板**：完整走一遍「建表 → 实体 → 服务 → 接口 → 权限 → 审计 → 测试 → 文档」的模块生命周期。下载者照本模块克隆新模块（见文末「如何照本模块克隆新模块」）。

## 职责

- **公告管理**：公告的增删改查、分页查询、状态流转（草稿 → 发布 → 下线）。
- **资源服务器示范**：作为 OAuth2 资源服务器，双保险校验 JWT（与 gateway/auth 共用 RSA 公钥）。
- **公开接口示范**：查询类接口（`GET /demo/announcement/page`、`GET /demo/announcement/{id}`）无需登录。
- **权限注解示范**：管理类接口需认证且具备 `demo:announcement:edit` 权限（方法级 `@PreAuthorize`）。
- **审计字段示范**：`create_by/create_time/update_by/update_time` 由 `MyMetaObjectHandler` 自动填充。
- **统一返回/异常示范**：复用 `cornerstone-common` 的 `Result<T>`、`BusinessException`、`GlobalExceptionHandler`。

## 边界（本模块不做的事）

- **不做权限管理**：RABC（用户/角色/菜单）属于 `cornerstone-system`。
- **不做认证签发**：令牌签发属于 `cornerstone-auth`，本模块只校验令牌、不创建令牌。
- **不跨服务直连**：对外需要的数据通过 `cornerstone-api` Feign 契约获取，禁止直连其他服务数据库。
- **不做数据权限（部门级）**：v2 ADR 后引入。
- **公告内容不做富文本/附件**：v1 仅存纯文本 `TEXT`。

## 依赖与端口

| 项 | 值 |
| --- | --- |
| 端口 | 8083 |
| 数据库 | `cornerstone_demo`（一服务一库）+ Flyway 版本化迁移 |
| 注册 | Nacos `spring.cloud.nacos.discovery.server-addr=localhost:8848` |
| 安全 | 资源服务器，RSA 公钥位于 `application.yml → cornerstone.security.public-key` |

## 词汇表

| 术语 | 定义 |
| --- | --- |
| **公告**（Announcement） | 本模块唯一的业务实体，一条对外发布的文字消息。 |
| **状态**（Status） | 公告生命周期状态：`草稿(0) → 已发布(1) → 已下线(2)`，单向流转。 |
| **公开接口** | 无需认证即可访问的接口（本模块的查询类）。 |
| **资源服务器** | 校验 JWT、拒绝未认证请求的安全角色（本模块与 auth 共用公钥）。 |
| **权限点** | 方法级权限标识，如 `demo:announcement:edit`，供 `@PreAuthorize` 使用。 |
| **审计字段** | `create_by/create_time/update_by/update_time`，由自动填充器写入。 |

## 架构与实现要点

```
controller → service(业务规则) → mapper(MyBatis-Plus) → announcement 表
     │            │
   Result<T>   BusinessException（状态非法/不存在/标题必填）
     └── cornerstone-common 契约
```

- **分页**：MyBatis-Plus `Page` + `PaginationInnerInterceptor(MYSQL)`（`config/MybatisPlusConfig`）。
- **状态流转规则**集中在 `AnnouncementServiceImpl`：
  - 新增即 `草稿`；仅 `草稿` 可编辑；`草稿→已发布`；`已发布→已下线`；非法流转抛 `BusinessException(ANNOUNCEMENT_STATUS_ILLEGAL)`。
- **权限**：`SecurityConfig` 配置公开白名单（公告查询仅 GET / Springdoc / **Actuator**）+ 资源服务器校验；`@PreAuthorize("hasAuthority('demo:announcement:edit')")`。
- **安全异常映射**：`config/ResourceServerExceptionAdvice` 将方法级无权限改为 HTTP 403（避免被 common 兜底吞成 200）。
- **审计**：`config/MyMetaObjectHandler` 实现 `MetaObjectHandler`，取当前用户上下文，匿名回退 `system`。

## 测试策略

- **MockMvc 集成测试**（`AnnouncementControllerTest`）：真实拉起 Spring 上下文，H2 MySQL 兼容模式跑 Flyway，不依赖真实 MySQL。
- 覆盖：公开接口无 token 200、受过滤接口无 token 401、带权访问 200、无权限 403、非法状态流转业务异常、标题校验。
- 测试 JWT 用 `src/test/resources` 下的测试密钥对 RS256 签发（`TestJwtIssuer`）。
- `application-test.yml` 关闭 Nacos、切到 H2。

## 如何照本模块克隆新模块

> 新模块是又一次「文档约束」决策，动手前先过 ADR 门槛（见 `docs/adr/0004-anti-bloat-gates.md`）。

1. **复制模块**：复制 `cornerstone-demo/` 为 `<新模块>/`（如 `cornerstone-order`）。
2. **改 POM artifactId**：把 `<artifactId>cornerstone-demo</artifactId>` 改为新模块名；`<name>`、`<description>` 同步更新；依赖保留（父 POM 统一管版本，禁止写版本号）。
3. **改包名 / 端口 / 库名**：
   - 包：`com.cornerstone.demo` → `com.cornerstone.<新模块>`（含 `src/test`）。
   - 端口：`application.yml → server.port` 改为 838x。
   - 库名：`spring.datasource.url` 的 `cornerstone_demo` → `<新库>`；在基础设施初始化脚本中建库。
   - 服务名：`spring.application.name` → `cornerstone-<新模块>`。
4. **写 Flyway 迁移**：删掉 demo 的迁移（`V1`–`V3`），新建 `V1__baseline.sql`（建你的表）+ `V2__seed.sql`（种子数据）+ 后续增量。表结构见 `docs/adr/0003`：纯 SQL、版本化、可回放。
5. **实现实体/服务/接口**：照 demo 的 `domain/mapper/service/controller` 分包：
   - 实体继承 MyBatis-Plus 注解（`@TableId`/`@TableName`/`@TableLogic`/`@TableField(fill=...)`）。
   - 自定义错误码枚举：实现 `com.cornerstone.common.core.IErrorCode`，从 **1000 起** 编号，避免与内置码冲突。
   - 服务抽象业务规则，非法状态抛 `BusinessException`。
   - 公开接口在 `SecurityConfig.PUBLIC_GET_PATHS`（仅 GET 读接口）或 `PUBLIC_OTHER_PATHS`（文档/Actuator）白名单加路径；受保护接口加 `@PreAuthorize`——写操作只允许 GET 之外的方法在 URL 层即要求认证。
   - 统一返回 `Result<T>`，禁止另起炉灶。
6. **写 CONTEXT.md**：照本节结构写清「职责 / 边界 / 不做的事 / 词汇表」；新模块的领域术语必须入库级词汇表。
7. **过 ADR 门槛**：新增模块 = 一次难逆决策，记录 `docs/adr/` 决策 + 更新根 `CONTEXT-MAP.md` 模块地图。
8. **对齐密钥**：demo 里的 RSA 公钥是**演示用测试密钥**。生产/联调时，`cornerstone-demo` 与 `gateway`/`auth` 必须落到**同一对真实密钥**，仅换 `cornerstone.security.public-key` 一处即可。

## 目录速览

```
cornerstone-demo/
├── CONTEXT.md                       ← 本文件（模块契约）
├── pom.xml
└── src/main/
    ├── java/com/cornerstone/demo/
    │   ├── DemoApplication.java     ← 启动入口
    │   ├── config/                  ← Security / MybatisPlus / MetaObjectHandler / 安全异常映射
    │   ├── domain/                  ← 实体 Announcement + 状态枚举 + 模块错误码
    │   ├── mapper/                  ← AnnouncementMapper (BaseMapper)
    │   ├── service/ (+ impl)        ← 业务规则（状态流转、必填校验）
    │   └── controller/              ← 公告接口（公开 + 受保护）
    └── resources/
        ├── application.yml
        └── db/migration/            ← V1__baseline.sql / V2__seed.sql / V3__author_column.sql / V4__create_time_index.sql
└── src/test/                        ← MockMvc 集成测试（H2 跑 Flyway）
```
