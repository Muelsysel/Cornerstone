# ADR-0002: 认证方案——Spring Authorization Server

Status: accepted

认证采用标准 OAuth2：`cornerstone-auth` 基于 **Spring Authorization Server** 作为授权服务器，RS256 签发 JWT 并提供 JWKS 端点；启用 `client_credentials`（服务间/演示）与**用户名密码登录**（`POST /auth/login`，BCrypt 校验后签发带角色权限的 JWT）；`authorization_code + PKCE` 留给后续前端 SPA 接入。网关统一校验令牌（白名单放行），下游服务作为资源服务器双保险。

Consequences: 标准生态、可平滑支持前端接入；`authorization_code + PKCE` 仍为待办（v6+ 路线图）。

Considered Options: 简化 JWT 直发端点（拒绝：非标准，v2 前端接入时推倒重来）；Sa-Token（用户明确弃用，选型时替换为 Spring Security 生态）。
