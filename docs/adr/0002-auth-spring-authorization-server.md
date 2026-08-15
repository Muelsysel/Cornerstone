# ADR-0002: 认证方案——Spring Authorization Server

Status: accepted

认证采用标准 OAuth2：`cornerstone-auth` 基于 **Spring Authorization Server** 作为授权服务器，RS256 签发 JWT 并提供 JWKS 端点；v1 启用 `client_credentials`（服务间/演示），`authorization_code + PKCE` 留给 v2 前端 SPA。网关统一校验令牌（白名单放行），下游服务作为资源服务器双保险。

Consequences: 标准生态、可平滑支持前端接入；v1 无登录换令牌流程（用户登录在 v2 授权码流程中实现）。

Considered Options: 简化 JWT 直发端点（拒绝：非标准，v2 前端接入时推倒重来）；Sa-Token（用户明确弃用，选型时替换为 Spring Security 生态）。
