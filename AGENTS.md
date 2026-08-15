# Cornerstone — 文档约束驱动的 AI 协作 Spring Cloud 脚手架

Cornerstone 的定位：**快速使用 AI 开发 Spring Cloud 项目的基石**。它借鉴 RuoYi 的工程思想（RBAC、前后端分离、模块化），但不借鉴一行代码。仓库里的文档不是摆设，而是每个 AI 必须遵守的**契约**——这就是"文档约束"：任何 AI 在本仓库动代码之前，必须先完成文档导航。

## 黄金法则（任何开发工作前）

1. **先读文档，再写代码**：按序阅读 `CONTEXT-MAP.md` → 目标模块的 `CONTEXT.md` → 相关 `docs/adr/`。没读过 = 不能开工。
2. **文档与代码同改**：领域概念变化 → 同步更新 `CONTEXT.md` 词汇表；做了难逆决策 → 新增 ADR。
3. **顺应工作流开发**：加载 `skills/cornerstone-dev/SKILL.md`，按其步骤执行（定位模块 → 遵守规范 → 验证 → 提交）。
4. **只用词汇表里的术语**：不得自造与 `CONTEXT.md` 冲突的同义词；发现词汇缺口，记录给 `/domain-modeling`。

## AI 行为准则（八荣八耻）

本项目所有 AI 与开发者共同遵守的行为准则：

| 耻 | 荣 |
| --- | --- |
| 以**暗猜接口**为耻 | 以**认真查阅**为荣 |
| 以**模糊执行**为耻 | 以**寻求确认**为荣 |
| 以**盲想业务**为耻 | 以**人类确认**为荣 |
| 以**创造接口**为耻 | 以**复用现有**为荣 |
| 以**跳过验证**为耻 | 以**主动测试**为荣 |
| 以**破坏架构**为耻 | 以**遵循规范**为荣 |
| 以**假装理解**为耻 | 以**诚实无知**为荣 |
| 以**盲目修改**为耻 | 以**谨慎重构**为荣 |

## 项目速览

- 定位：面向多 AI 协作的 Spring Cloud 脚手架基石，ruoyi 思想借鉴、零代码借鉴
- 技术栈：Java 17 · Spring Boot 3.2 · Spring Cloud 2023 · Spring Cloud Alibaba (Nacos) · Spring Cloud Gateway · OpenFeign · Spring Security + OAuth2 · MyBatis-Plus · Redis · MySQL · Vue3 (cornerstone-web)
- 模块：`cornerstone-gateway`（网关）· `cornerstone-auth`（认证中心，含用户登录）· `cornerstone-system`（系统服务：RBAC + 数据权限）· `cornerstone-demo`（演示业务）· `cornerstone-common`（公共）· `cornerstone-api`（跨服务契约）· `cornerstone-web`（前端）
- 已实现能力：client_credentials 认证 · 用户名密码登录（POST /auth/login，admin/admin123）· RBAC（用户/角色/菜单/部门/字典/参数/日志）· 部门数据权限 · 公告演示 · 管理后台
- 文档体系：根 `CONTEXT-MAP.md` 是模块地图；每个模块自带 `CONTEXT.md`；`docs/adr/` 记录难逆决策

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
