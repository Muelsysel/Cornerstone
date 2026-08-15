# ADR-0007: 服务间认证——共享内部令牌

Status: accepted

`cornerstone-auth` 经 Feign 调 `cornerstone-system` 的 `/system/auth/**` 内部接口（登录认证信息、登录日志）。v2 该接口匿名访问（仅靠网关白名单隔离外部，system 端口直连可滥用）。v3 改为**共享内部令牌**：auth 的 Feign 拦截器（`FeignInternalTokenConfig`）自动附加 `X-Internal-Token` 头（`cornerstone.internal-token`，auth/system 共享配置），system 的 `InternalTokenFilter` 校验，无令牌或令牌不符返回 401。

Consequences: 服务间调用具备最小认证，内部接口不再匿名；令牌为共享静态值（非签发/轮换），适合 v1 规模——生产环境可升级为 mTLS 或 JWT 服务凭证（v4 候选）。

Considered Options: Feign 拦截器动态获取 client_credentials 令牌（更标准、可审计，但需令牌获取与缓存逻辑，复杂度高，列为 v4 候选）；保持匿名（拒绝：system 端口直连可被滥用）。

## 后续扩展（2026-08-16 更新）：网关转发起源证明

同一令牌扩展为**网关→服务的转发起源证明**：网关 `TokenAuthGlobalFilter` 转发时附加 `X-Internal-Token`（并剥除客户端伪造值），服务端 `UserContextFilter` 仅在令牌有效时采信 X-Cornerstone-* 身份透传头。动机：直连服务端口可伪造透传头冒充任意身份（如 roles=admin 提升数据权限范围）；令牌证明请求确实经网关（网关是唯一可信入口）。令牌为共享静态值，泄漏即失守——与 Feign 场景同风险等级，生产升级路径不变（mTLS/JWT 服务凭证）。

