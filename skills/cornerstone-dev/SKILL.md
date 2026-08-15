---
name: cornerstone-dev
description: Cornerstone 项目开发工作流。当你在 Cornerstone 仓库（含子模块目录）中开发新功能、修改代码、新建模块、审查改动或需要理解项目结构时加载。它强制"先导航文档→再写代码→验证→提交"的流程，确保任何 AI 的产出符合文档约束。
---

# Cornerstone 开发工作流

Cornerstone 是**文档约束驱动**的 Spring Cloud 脚手架：文档是契约，先读后写。本技能是每个 AI 在本仓库工作的标准流程。与 `AGENTS.md` 的黄金法则和八荣八耻配套使用。

## 工作流（按序执行，每步有完成标准）

### 1. 导航（动手前必做，缺一不可）

| 步骤 | 读什么 | 完成标准 |
| --- | --- | --- |
| 1a | `CONTEXT-MAP.md`（仓库根） | 能说出改动涉及哪个模块、该模块的边界 |
| 1b | 目标模块的 `CONTEXT.md` | 能说出模块职责、边界、词汇表术语 |
| 1c | `docs/adr/` 与模块内 `docs/adr/` | 确认没有 ADR 与你将做的改动冲突 |

若发现词汇缺口（需要但 CONTEXT 未定义的术语）→ 停下，向人类确认或记录，不得自造术语。

### 2. 定位（确认落点）

- 确认改动落在**一个模块**内；跨模块改动 = 走 ADR 门槛（先记录决策再动手）
- 跨服务调用：检查 `cornerstone-api` 是否已有契约；没有 → 在 api 模块定义，**禁止直连其他服务 HTTP 接口**

### 3. 规范（八荣八耻，摘自 AGENTS.md）

- **不猜接口，认真查阅**——接口签名以 `cornerstone-api` 与 OpenAPI 文档为准
- **不模糊执行，寻求确认**——需求含糊时先问人类，不自行脑补
- **不臆想业务，人类确认**——业务规则以 CONTEXT.md 词汇表为准
- **不造新接口，复用现有**——先查 common 的 Result/错误码/异常是否够用
- **不跳过验证，主动测试**——每个功能带测试（见步骤 5）
- **不破坏架构，遵循规范**——包结构、命名、统一返回、Flyway 迁移规则
- **不假装理解，诚实无知**——不懂就说不懂，先查文档再问
- **不盲目修改，谨慎重构**——重构先过测试，小步提交

### 4. 实现（遵循项目模式）

- 新建模块：**照 `cornerstone-demo` 克隆**（活模板，含 CONTEXT.md 与克隆指引）
- 新功能：建表走 Flyway 增量迁移（`V{n+1}__desc.sql`，纯 SQL）→ 实体 → Mapper → Service → Controller → 测试 → 文档
- 响应一律 `Result<T>`；业务错误抛 `BusinessException`（错误码从 1000 起自定义枚举实现 `IErrorCode`）
- 登录用户从 `UserContextHolder.get()` 取（网关透传头解析而来，服务不自行解析令牌）
- 代码格式：google-java-format（AOSP），提交前 `mvn spotless:apply`

### 4b. 关键约定（v2+ 已固化的架构事实）

- **登录与权限**：用户登录在 `cornerstone-auth` 的 `POST /login`（BCrypt 校验 + 签发 JWT，claims：sub=userId、roles、scope=权限集合）；`@PreAuthorize("hasAuthority('system:user:list')")` 从 JWT scope 读取权限；客户端凭据（client_credentials）令牌无业务权限（访问业务接口返回 403 是预期行为）
- **认证支持接口**：`/system/auth/**` 是 auth 登录时经 Feign（`AuthUserClient`）调 system 的内部接口（含 BCrypt 哈希，禁止暴露到网关外部；生产需服务间认证）
- **Feign 契约**：`cornerstone-api` 中每个 `@FeignClient` 必须带唯一 `contextId`（同服务多客户端否则 FeignClientSpecification 冲突）；服务调用方需 `spring-cloud-starter-loadbalancer`
- **数据权限**：`sys_role.data_scope`（1全部~5仅本人）+ `sys_role_dept`；`CornerstoneDataPermissionHandler` 按 `mappedStatementId` 拦截 `SysUserMapper` 查询自动追加条件；扩展管控其他表时在 handler 调整。**注意**：handler 条件必须与原有 WHERE 合并（AND），不能替换（否则参数丢失报 Parameter index out of range）
- **前后端契约**：实体主键序列化为 `userId/roleId/menuId/deptId`（`@JsonProperty`），前端统一用该命名；status 用 `'0'/'1'`；新增管理功能必须同时补菜单权限点（V5/V7 教训：controller 权限注解 + 菜单树权限点 + admin role_menu 三处一致）
- **审计列**：实体继承 `BaseEntity` 的表必须有 `create_by/update_by` 列（否则 MyBatis-Plus 全字段查询报 Unknown column）
- **Mapper 双参数**：`@Param` 必加（父 POM 未开 `-parameters`，XML foreach 无法按名绑定）
- **密码与密钥**：密码用 DelegatingPasswordEncoder（支持 {noop} 客户端密钥 + 无前缀 BCrypt 用户哈希）；RSA 密钥四处一致（auth 私钥 ↔ gateway/system/demo 公钥）
- **前端**：`cornerstone-web/`（Vue3 + Element Plus），开发经 vite 代理到网关 8080，登录态存 localStorage，接口统一 Result 处理

### 5. 验证（未验证不算完成）

```powershell
# 每次先设 JAVA_HOME（JDK 17）
$env:JAVA_HOME = "C:\Dev\Lang\JAVA\JAVA17"
# 编译 + 测试 + 格式检查（以模块为例）
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" test -pl cornerstone-system
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" spotless:check -pl cornerstone-system
```

完成标准：编译通过 · 测试通过 · spotless:check 通过 · 测试不允许因缺 MySQL/Redis 失败（用 H2 或 mock）

### 6. 提交（文档与代码同改）

- 提交信息格式：`<type>(<scope>): <描述>`，type ∈ feat/fix/docs/refactor/test/chore
- 涉及领域概念 → 同步更新相关 `CONTEXT.md` 词汇表
- 难逆决策 → 新增 ADR（`docs/adr/NNNN-slug.md`）
- 中文描述，一句话说清"改了什么、为什么"

## 安装到你的 AI 工具

- **Claude Code**：把本目录（`skills/cornerstone-dev/`）复制到项目的 `.claude/skills/` 下
- **Cursor**：在 `.cursor/rules/` 放一条规则：开发前阅读 `AGENTS.md` 与本技能
- **DeepSeek Harness (DSH)**：复制到你的 DSH skills 目录，重启会话
- **Codex / Copilot**：确保 `AGENTS.md` 可被读取，并在提示中引用本技能路径

无论哪种工具，`AGENTS.md` 都是入口——先让它读到黄金法则与八荣八耻，再让 AI 加载本技能。
