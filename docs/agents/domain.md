# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT-MAP.md`** at the repo root — Cornerstone is a **multi-context** repo: the map points at one `CONTEXT.md` per module. Read each one relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in. Also check `<module>/docs/adr/` for module-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Cornerstone is a multi-context repo (`CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md
├── AGENTS.md
├── docs/adr/                          ← system-wide decisions
├── docs/agents/                       ← engineering-skill config
├── skills/cornerstone-dev/            ← the workflow skill every AI must load
├── cornerstone-common/
│   └── CONTEXT.md
├── cornerstone-api/
│   └── CONTEXT.md
├── cornerstone-gateway/
│   ├── CONTEXT.md
│   └── docs/adr/                      ← gateway-scoped decisions (if any)
├── cornerstone-auth/
│   ├── CONTEXT.md
│   └── docs/adr/
├── cornerstone-system/
│   ├── CONTEXT.md
│   └── docs/adr/
├── cornerstone-demo/
│   ├── CONTEXT.md                     ← new-module live template
│   └── docs/adr/
└── cornerstone-web/
    └── README.md                      ← 前端（非 Maven 模块）：设计语言与部署说明，无 CONTEXT.md
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the relevant `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_
