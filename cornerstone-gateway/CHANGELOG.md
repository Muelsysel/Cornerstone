# Changelog — cornerstone-gateway

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.1.1] - 2026-08-15

- refactor: RSA 公钥解析改用 common `RsaKeyUtils`（与 auth/system/demo 统一）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。

## [1.1.0] - 2026-08-15

- fix: 全局 CORS 由 `*` 收紧为本地白名单（dev 5173 / 前端容器 8088；生产经 nginx 同源反代不触发 CORS）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。

## [1.0.0] - 2026-08-15（初始）

- feat: 路由（lb:// 到 auth/system/demo，仅 auth 剥离前缀）；JWT 校验过滤器（白名单、Bearer 校验、`X-Cornerstone-*` 透传——roles 优先 JWT roles 声明、scope 兜底）；CORS
- feat: Redis 限流（RequestRateLimiter 10/s + burst 20，三路由全启用）；访问日志 `AccessLogFilter`（方法/路径/状态/耗时/IP）
- fix: 补 loadbalancer 依赖（lb:// 解析）；自定义 SecurityWebFilterChain（permitAll + 禁 CSRF，阻止 resource-server 自动配置拦截）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。
