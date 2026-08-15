# ADR-0005: 前端独立仓库——v1 无前端

Status: accepted

v1 聚焦后端微服务骨架，**不包含前端**。管理后台（Vue3 + Vite + Element Plus，前后端分离）作为独立仓库规划，v2 起接入 OAuth2 授权码 + PKCE。

Consequences: 骨架保持"小而完整"；API 以 OpenAPI 文档化，为前端与 AI 联调提供契约。若未来需要前端，应新建独立仓库而非并入本仓库。
