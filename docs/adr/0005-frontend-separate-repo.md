# ADR-0005: 前端独立仓库——v2 起接入管理后台

Status: accepted

v1 聚焦后端微服务骨架，**不包含前端**。管理后台（Vue3 + Vite + Element Plus，前后端分离）作为独立仓库规划。

**当前状态更新（v2 前置）：** 前端已在本仓库内新建 `cornerstone-web/` 目录完成初版管理后台（登录 + 系统管理 + 公告），用于与后端认证链路联调。该目录是纯前端目录，**不加入父 POM 的 Maven modules**；后续按本 ADR 拆分为独立仓库（届时剥离到独立 Git 仓库，前端代码、构建与发布机制整体迁移）。

Consequences: 骨架保持"小而完整"；API 以 OpenAPI 文档化，为前端与 AI 联调提供契约。最终目标是独立前端仓库；当前先在 `cornerstone-web/` 内开发以便与后端同仓联调，待接入 OAuth2 授权码 + PKCE 时再正式拆分。
