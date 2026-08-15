# ADR-0001: 文档约束架构与多上下文文档布局

Status: accepted

Cornerstone 的核心理念是"文档约束"：仓库文档是每个 AI 必须遵守的契约。我们决定采用多上下文文档布局——根 `CONTEXT-MAP.md` 指向每模块 `CONTEXT.md`，`AGENTS.md` 作为所有 AI 的入口（黄金法则 + 八荣八耻），决策记录在 `docs/adr/`。任何 AI 动代码前必须完成文档导航；文档与代码同改。

Consequences: 文档成为一等公民，变更必须同步文档；代价是文档维护成本，由 CI 门禁（ADR-0011 文档校验）分摊。

Considered Options: 单上下文根 `CONTEXT.md`（拒绝：微服务多模块下词汇与边界无法区分）。
