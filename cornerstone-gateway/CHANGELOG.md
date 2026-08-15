# Changelog — cornerstone-gateway

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.2.13] - 2026-08-16

- fix(security): 网关转发时附加 `X-Internal-Token`（`cornerstone.internal-token`），并剥除客户端伪造的内部令牌——下游 `UserContextFilter` 凭此证明请求经网关转发（防直连服务端口伪造身份头）；`TokenAuthGlobalFilterTest` 新增 `forwardAddsInternalTokenAndStripsClientSupplied`

**测试方法**：`mvn test -pl cornerstone-gateway`（19 用例）+ 实测限流。

## [1.2.12] - 2026-08-16

- feat(security): 限流键解析支持受信反代——新增 `ClientIpKeyResolver`：默认按直连 IP 限流；当直连对端在 `cornerstone.gateway.trusted-proxy-ips` 列表时取 X-Forwarded-For 首值还原真实客户端（生产经 nginx 容器 8088 反代时，此前所有用户共享一个限流桶，登录爆破防护失效；直接信任 XFF 可被伪造绕过，故仅受信代理采信）；`ClientIpKeyResolverTest` 4 用例（不受信不采信/受信取首值/无 XFF 回退/畸形头回退）

**测试方法**：`mvn test -pl cornerstone-gateway`（18 用例）+ 实测限流。

## [1.2.11] - 2026-08-16

- fix(security): **修复网关限流完全失效**——曾用 `new RedisRateLimiter(10, 20)` 构造（无 RedisTemplate/脚本注入），限流器空转（请求全放行、Redis 无 request_rate_limiter key）；改为「模板 + 脚本 + ConfigurationService」正确构造，速率按路由 id 预置 config map（auth-login 5/s+突发10，auth/system/demo 10/s+突发20）；实测登录 20 连发触发 9 次 429

**测试方法**：`mvn test -pl cornerstone-gateway`（14 用例）+ 实测限流。

## [1.2.10] - 2026-08-16

- test: `TokenAuthGlobalFilterTest` 新增白名单边界用例——`/auth` 精确放行、`/auth-extra` 与 `/demo-secret` 前缀不误匹配（防前缀越权）

**测试方法**：`mvn test -pl cornerstone-gateway`（14 用例）。

## [1.2.9] - 2026-08-16

- test: `GatewayContextLoadTest` 新增限流契约断言——反射读取 `RedisRateLimiter.defaultConfig`，锁定文档记录的限流数值（默认 10/s+突发 20、登录 5/s+突发 10），防数值漂移

**测试方法**：`mvn test -pl cornerstone-gateway`（13 用例）。

## [1.2.8] - 2026-08-15

- docs: CONTEXT.md 职责补访问日志说明（X-Forwarded-For 客户端 IP + 探针静默）

**测试方法**：`mvn test -pl cornerstone-gateway`（12 用例）。

## [1.2.7] - 2026-08-15

- fix: `AccessLogFilter` 记录客户端 IP 优先取 X-Forwarded-For 第一个（经 nginx 反代时此前记录的是反代地址，无法定位真实客户端）；`AccessLogFilterTest` 补用例

**测试方法**：`mvn test -pl cornerstone-gateway`（12 用例）。

## [1.2.6] - 2026-08-15

- docs: application.yml 注释修正（"信号"→"签名"）

**测试方法**：`mvn test -pl cornerstone-gateway`（11 用例）。

## [1.2.5] - 2026-08-15

- test: `AccessLogFilterTest` 3 用例（actuator 探针静默、常规请求记录方法/路径/状态/耗时、执行顺序最后——访问日志行为回归）

**测试方法**：`mvn test -pl cornerstone-gateway`（AccessLogFilter 3 用例、TokenAuthGlobalFilter 8 用例、上下文装配）。

## [1.2.4] - 2026-08-15

- fix: 白名单路径携带有效令牌时同样重建透传上下文头（此前 /demo/** 免校验直接放行导致公告管理等受保护操作的审计/作者为空）；无/无效令牌仍放行（公开语义不变），补 3 个透传用例

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 8 用例、上下文装配）。

## [1.2.3] - 2026-08-15

- perf: LoadBalancer 换用 Caffeine 服务实例缓存（消除生产默认缓存警告，提升服务发现性能）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 6 用例、上下文装配）。

## [1.2.2] - 2026-08-15

- fix(安全): 透传头防伪造——入口统一剥除客户端可控的 `X-Cornerstone-*` 头（白名单路径同样清洗），透传头只由 JWT 重建；补 2 个防伪造回归用例

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 6 用例、上下文装配）。

## [1.2.1] - 2026-08-15

- fix: 访问日志过滤 `/actuator/**`（健康检查高频请求不入日志，避免噪音刷屏）

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。

## [1.2.0] - 2026-08-15

- feat: 登录接口独立限流——`/auth/login` 专用路由（声明于 /auth/** 之前）按 IP 每秒 5 令牌/桶 10，缓解账号密码定向爆破；其余路由维持 10/s + burst 20

**测试方法**：`mvn test -pl cornerstone-gateway`（TokenAuthGlobalFilter 单测、上下文装配）。

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
