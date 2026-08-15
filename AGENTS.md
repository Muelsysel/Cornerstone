# Cornerstone — 文档约束驱动的 AI 协作 Spring Cloud 脚手架

Cornerstone 的定位：**快速使用 AI 开发 Spring Cloud 项目的基石**。它采用业界成熟工程范式（RBAC、前后端分离、模块化）从零自研。仓库里的文档不是摆设，而是每个 AI 必须遵守的**契约**——这就是"文档约束"：任何 AI 在本仓库动代码之前，必须先完成文档导航。

## 黄金法则（任何开发工作前）

1. **先读文档，再写代码**：按序阅读 `CONTEXT-MAP.md` → 目标模块的 `CONTEXT.md` → 相关 `docs/adr/`。没读过 = 不能开工。
2. **文档与代码同改**：领域概念变化 → 同步更新 `CONTEXT.md` 词汇表；做了难逆决策 → 新增 ADR。
3. **顺应工作流开发**：加载 `skills/cornerstone-dev/SKILL.md`，按其步骤执行（定位模块 → 遵守规范 → 验证 → 提交）。
4. **只用词汇表里的术语**：不得自造与 `CONTEXT.md` 冲突的同义词；发现词汇缺口，记录给 `/domain-modeling`。
5. **每个 AI 都是文档维护者**：修改任何模块，必须同步更新该模块的 `CONTEXT.md` 与 `CHANGELOG.md`（见"文档维护义务"）。

## 文档维护义务（强制）

**任何代码变更的验收标准之一**：

- **改了什么 → 记入该模块 `CHANGELOG.md`**：每次修改/升级/修复，在模块 `CHANGELOG.md` 顶部新增条目（版本/日期/变更类型/说明/涉及接口与测试），方便后续任何 AI 快速了解模块演化。
- **模块文档同步**：`CONTEXT.md` 的职责/边界/词汇表如有变化 → 同步更新。
- **测试说明同步**：模块 `CONTEXT.md` 的"测试方法"章节记录如何跑该模块的单元/集成测试；新增/修改接口必须补测试，推送前按该模块测试方法全量验证。

## 项目速览

- 定位：面向多 AI 协作的 Spring Cloud 脚手架基石，业界成熟工程范式（RBAC、前后端分离、模块化）从零自研
- 技术栈：Java 17 · Spring Boot 3.2 · Spring Cloud 2023 · Spring Cloud Alibaba (Nacos) · Spring Cloud Gateway · OpenFeign · Spring Security + OAuth2 · MyBatis-Plus · Redis · MySQL · Vue3 + Vite 7 + Element Plus（按需引入，Vitest 单测，cornerstone-web）
- 模块：`cornerstone-gateway`（网关）· `cornerstone-auth`（认证中心，含用户登录）· `cornerstone-system`（系统服务：RBAC + 数据权限）· `cornerstone-demo`（演示业务）· `cornerstone-common`（公共）· `cornerstone-api`（跨服务契约）· `cornerstone-web`（前端）
- 已实现能力：client_credentials 认证 · 用户名密码登录（POST /auth/login，admin/admin123，网关独立限流防爆破）· RBAC（用户/角色/菜单/部门/字典/参数/日志）· 部门数据权限 · 操作/登录日志（含密码脱敏）· 公告演示 · 管理后台 · Nginx 容器化部署（前后端分离）· 一键启动脚本 scripts/start-all.ps1
- 文档体系：根 `CONTEXT-MAP.md` 是模块地图；每个模块自带 `CONTEXT.md` + `CHANGELOG.md`；`docs/adr/` 记录难逆决策

## Agent skills

### Issue tracker

Issues live as GitHub issues via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context: root `CONTEXT-MAP.md` points at per-module `CONTEXT.md` files. See `docs/agents/domain.md`.

## 快速导航

| 想做什么 | 读什么 |
| --- | --- |
| 了解整个项目怎么运转 | `CONTEXT-MAP.md` |
| 在某个模块里开发 | 该模块的 `CONTEXT.md` + 相关 ADR |
| 理解为什么这么设计 | `docs/adr/` |
| 按项目工作流开发 | `skills/cornerstone-dev/SKILL.md` |
| 工程技能如何读这些文档 | `docs/agents/` |
