# Changelog — cornerstone-gateway

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.0] - 2026-08-15（初始）

- feat: 路由（lb:// 到 auth/system/demo，仅 auth 剥离前缀）；JWT 校验过滤器（白名单、Bearer 校验、`X-Cornerstone-*` 透传——roles 优先 JWT roles 声明、scope 兜底）；CORS
- feat: Redis 限流（RequestRateLimiter 10/s + burst 20，三路由全启用）；访问日志 `AccessLogFilter`（方法/路径/状态/耗时/IP）
- fix: 补 loadbalancer 依赖（lb:// 解析）；自定义 SecurityWebFilterChain（permitAll + 禁 CSRF，阻止 resource-server 自动配置拦截）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。
