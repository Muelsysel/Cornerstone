# Spec 001 — Cornerstone v1：文档约束驱动的 AI 协作 Spring Cloud 脚手架

> 状态：ready-for-agent · 来源：grill-with-docs 设计树（用户已确认共享理解）
>
> **演进注记（读此文档前必看）**：本文档为 v1 原始规格，后续实现决策以 ADR 为准并已落地——
> 数据权限（部门级）已实现（ADR-0006）；前端管理后台已并入本仓库 `cornerstone-web`（Vue3 + Vite）；网关限流已落地（含登录独立限流，ADR-0009）；测试缝实际采用 H2 + Mock（非 Testcontainers）。仍待实现：授权码 + PKCE（前端接入时启用）。

## Problem Statement

团队使用多个 AI 工具（Claude Code / Cursor / Copilot 等）协作开发 Spring Cloud 项目时，每个 AI 对项目理解不一、各自为政，导致产出风格混乱、架构漂移、重复造轮子。市场上缺少一个以"文档约束"为核心的脚手架：让任何 AI 都能通过文档快速理解项目、遵循统一工作流、产出符合项目规范的代码。

## Solution

**Cornerstone**——一个文档约束驱动的 Spring Cloud 脚手架基石。仓库内置完整文档契约体系（AGENTS.md / CONTEXT-MAP.md / 各模块 CONTEXT.md / ADR / 自带 skill），任何 AI 必须"先读文档、再写代码"；提供可运行的微服务骨架（网关、认证中心、系统服务、演示业务模块），认证链路真实可跑；CI 门禁强制编译、测试、代码规范与文档完整性检查。

## User Stories

1. 作为**下载者**，我想按 README 用 docker-compose 一键启动 Nacos/MySQL/Redis，以便快速搭建演示环境
2. 作为**下载者**，我想用 curl 走 `client_credentials` 流程拿到 JWT 并访问受保护资源，以便亲眼验证认证链路跑通
3. 作为**开发者**，我想让任意 AI 工具读取 `AGENTS.md` 就能理解项目定位与规则，以便 AI 快速上手
4. 作为**开发者**，我想让 AI 加载 `skills/cornerstone-dev` 后按项目工作流开发，以便产出符合文档约束的代码
5. 作为**系统管理员**，我想管理用户（增删改查、启用停用、重置密码），以便控制平台访问
6. 作为**系统管理员**，我想管理角色并为角色分配菜单权限，以便实现 RBAC 授权
7. 作为**系统管理员**，我想管理菜单树（目录/菜单/按钮三级），以便定义权限点
8. 作为**系统管理员**，我想管理部门树，以便维护组织架构
9. 作为**系统管理员**，我想维护数据字典，以便下拉选项统一管理
10. 作为**系统管理员**，我想维护系统参数，以便运行参数可配置
11. 作为**审计者**，我想查询操作日志，以便追溯系统内谁做了什么
12. 作为**审计者**，我想查询登录日志，以便安全审计
13. 作为**业务用户**，我想在演示模块管理公告（CRUD、发布状态、分页），以便看到业务模块的标准开发模式
14. 作为**开发者**，我想使用统一的返回结构与异常体系，以便所有服务接口契约一致
15. 作为**开发者**，我想通过 OpenAPI 文档查看各服务接口，以便联调与 AI 查阅
16. 作为**开发者**，我想在 PR 时由 CI 自动执行编译/测试/规范/文档检查，以便合并前暴露疏漏
17. 作为**团队负责人**，我想新模块照 `cornerstone-demo` 克隆并走 ADR 门槛，以便扩展而不臃肿
18. 作为**开发者**，我想所有跨服务调用经 `cornerstone-api` 契约，以便服务间解耦、禁止直连
19. 作为**开发者**，我想数据库结构以 Flyway SQL 迁移版本化，以便结构可审阅、可回放
20. 作为**下载者**，我想看到每个模块的 `CONTEXT.md`（职责/边界/不做的事），以便快速判断改动落点

## Implementation Decisions

### 模块与边界（反臃肿：模块边界记录）
- `cornerstone-common`：共享库。统一返回 `Result<T>`、错误码枚举、全局异常处理、安全上下文持有者、通用工具。
- `cornerstone-api`：共享库。跨服务 Feign 契约与共享 DTO——**所有跨服务调用必须经此，禁止服务间直连/复制 DTO**（契约先行）。
- `cornerstone-gateway`：服务。统一入口：路由转发（Nacos 服务发现）、令牌校验（白名单放行）、CORS、基础限流预留。
- `cornerstone-auth`：服务。**Spring Authorization Server** 授权服务器：`client_credentials` 签发 JWT（RS256 + JWKS 端点）；授权码 + PKCE 留待 v2。
- `cornerstone-system`：服务。RBAC 标准集：用户、角色、菜单、部门、字典、参数、操作日志、登录日志。
- `cornerstone-demo`：服务。公告管理——**新模块活模板**，完整走一遍建表→实体→服务→接口→权限→审计→文档。

### 技术栈与版本（依赖集中管理：父 POM BOM）
- Java 17 · Spring Boot 3.2 · Spring Cloud 2023.0 · Spring Cloud Alibaba 2023.0（Nacos 注册+配置中心）· Spring Cloud Gateway · OpenFeign · Spring Authorization Server · MyBatis-Plus · Redis · MySQL 8 · Flyway · Springdoc OpenAPI
- 包结构 `com.cornerstone.*`；groupId `com.cornerstone`；artifactId `cornerstone-*`
- 一服务一库：`cornerstone_system`、`cornerstone_demo`；Flyway 迁移以 SQL 脚本编写（V1__baseline 建表、V2__seed 种子数据）

### 认证链路（v1）
- auth 提供 `/oauth2/token`（client_credentials）与 `/oauth2/jwks`
- gateway 白名单放行认证/公开端点，其余请求校验 JWT 并透传用户上下文头
- system/demo 作为资源服务器双保险校验 JWT
- 演示：curl client_credentials → 访问 `GET /system/...` 受保护资源

### 数据模型（RBAC）
- 用户(user)、角色(role)、用户角色(user_role)、菜单(menu，目录/菜单/按钮)、角色菜单(role_menu)、部门(dept，树)
- 字典类型/数据(dict_type/dict_data)、参数(config)、操作日志(oper_log)、登录日志(login_log)
- 数据权限（部门级）不在 v1，ADR 记录为 v2

### 工程机制（反臃肿全采纳）
- API 契约先行 · 模块边界记录 · ADR 门槛（新模块/新功能/依赖升级必须过 ADR）· 依赖集中管理 · demo 活模板

## Testing Decisions

- **测试缝（自高向低）**：
  1. 服务级 Spring Boot 集成测试（最高可用 seam）：每服务用 Testcontainers 起 MySQL/Redis（`-Pit` profile，需 Docker）验证 API→服务→存储完整行为；无 Docker 时默认只跑单元测试，集成测试跳过
  2. MockMvc 层测试（中间 seam）：各模块 REST 行为——统一返回结构、权限注解、参数校验
  3. 单元测试（低 seam）：错误码/异常/工具类等纯逻辑
- **认证测试**：auth 签发 JWT 的测试；system/demo 用测试 JWT（RS256，测试密钥）验证资源服务器校验
- **测试原则**：只测外部行为（API 契约），不测实现细节；以 API 契约为测试锚点
- **先例**：无既有先例——本 spec 首次建立测试模式，供后续模块复制

## Out of Scope

- 前端（v1 无；Vue3 管理后台为独立仓库，v2）
- 数据权限（部门级数据隔离，v2 ADR）
- OAuth2 授权码 + PKCE（前端接入时启用，v2）
- 扩展服务：文件、消息、监控、定时任务（v2 起按 ADR 逐个引入）
- 在线用户、岗位、通知公告（公告移入 demo 作业务示例；其余 v2）
- 多租户、分布式事务、灰度发布

## Further Notes

- **行为准则**（AGENTS.md 黄金法则）：不猜接口/不模糊执行/不臆想业务/不造新接口/不跳过验证/不破坏架构/不装懂/不盲改——项目行为准则
- **v2+ 扩展路径**（防臃肿）：授权码+PKCE 前端接入 → 数据权限 → 剩余系统功能 → 扩展服务；每项独立 ADR
- 部署双轨：docker-compose（推荐）与 README 手动说明（无 Docker 环境）
- GitHub Actions：编译 + 测试 + Spotless + 文档完整性检查；PR 模板强制勾选文档导航
