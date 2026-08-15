# 贡献指南（Contributing）

欢迎向 Cornerstone 贡献代码、文档或想法！无论你是人类还是 AI，都请遵守同一套规则——这就是
**文档约束**：仓库文档是契约，先读后写。

## 第一步：读文档（强制）

动手前必须完成文档导航（AGENTS.md 黄金法则）：

1. [CONTEXT-MAP.md](CONTEXT-MAP.md) —— 了解模块地图与词汇表
2. 目标模块的 `CONTEXT.md` —— 了解职责/边界/测试方法
3. [docs/adr/](docs/adr/) 与模块内 ADR —— 确认没有已记录的决策与你的改动冲突

> 若发现词汇缺口（需要但 CONTEXT 未定义的术语）：停下来，记录给领域建模，**不得自造术语**。

## 开发流程

1. **定位**：改动应落在一个模块内；跨模块/跨服务改动先走 ADR 门槛
2. **实现**：遵循既有模式（响应用 `Result<T>`、异常用 `BusinessException`、登录用户从
   `UserContextHolder` 取、Feign 契约只在 `cornerstone-api` 定义）
3. **验证**（未验证不算完成）：
   - 后端：`mvn test -pl <module>` + `mvn spotless:check`
   - 前端：`npm run build` + `npm test`
   - 文档：`bash scripts/check-docs.sh`
4. **文档与代码同改**：更新模块 `CONTEXT.md`（职责/词汇表/测试方法如有变化）与
   `CHANGELOG.md`（顶部新增条目，含涉及接口与测试）
5. **提交**：信息格式 `<type>(<scope>): <中文描述>`，type ∈ feat/fix/docs/refactor/test/chore

## 提交流程

1. Fork 本仓库并基于 `master` 开分支：`feat/xxx`、`fix/xxx`、`docs/xxx`
2. 提交前跑完第 3 步全部验证，CI 必须全绿
3. 发起 Pull Request，在描述中列出：改了什么、为什么、涉及哪些接口、验证结果

## 代码风格

- Java：google-java-format（AOSP），`mvn spotless:apply` 自动格式化
- SQL：Flyway 增量迁移（`V{n+1}__desc.sql`，纯 SQL，禁止修改已执行的历史迁移）
- 前端：TypeScript 严格模式；Vue3 `<script setup>`；Element Plus 按需引入
- 文档：中文；术语只用 CONTEXT 词汇表里的

## 问题与讨论

- Bug / 特性建议：开 Issue（说明复现步骤与期望行为）
- 架构讨论：先看 [docs/adr/](docs/adr/)，避免重复讨论已定决策
- AI 行为约定：见 [AGENTS.md](AGENTS.md) 与 `skills/cornerstone-dev/SKILL.md`

感谢你的贡献！🏗️
