# cornerstone-gateway — 网关

> 认证链路的**咽喉**：统一入口，负责路由转发、令牌校验与跨域。

## 职责

- 统一入口：按前缀路由到 Nacos 服务（`/auth/**`→auth、`/system/**`→system、`/demo/**`→demo、`lb://` 负载均衡）。
- 全局 JWT 校验：白名单路径放行，其余路径必须携带有效令牌。
- 令牌→上下文头透传：`sub`→`X-Cornerstone-User-Id`、`username/preferred_username`→`X-Cornerstone-Username`、角色→`X-Cornerstone-Roles`（优先 JWT 的 `roles` 声明，client_credentials 无 roles 时兜底 `scope`，逗号连接）。头名与 `UserContext` 约定一致；入口统一剥除外部伪造的同名头。
- Redis 限流（按客户端 IP，默认 10 请求/秒 + 突发 20；`/auth/login` 独立更严限流 5/s + burst 10 防账号爆破，路由声明于 `/auth/**` 之前）。
- 全局 CORS（仅本地白名单：dev 5173 / 前端容器 8088；生产经 nginx 同源反代不触发）。
- 校验失败返回 401 + `Result` 统一结构。
- Nacos 服务注册（`cornerstone-gateway`）。

## 边界

- **校验不签发**：令牌由认证中心签发，网关只校验并透传。
- 网关用**公钥**校验（`NimbusReactiveJwtDecoder`），持有与认证中心一致的 RSA 公钥，私钥只在认证中心。
- 各下游服务（system/demo）作为资源服务可再校验（双保险），但网关是透传头的唯一来源。

## 不做的事

- 不签发/刷新令牌（认证中心的职责）。
- 不引入 `spring-boot-starter-web`（WebFlux 环境，绝不放 servlet 依赖）；并排除 common 的 servlet 自动配置。
- 不处理业务逻辑（纯网关横切）。

## 词汇表

| 术语 | 定义 |
| --- | --- |
| 令牌（Token） | 认证中心签发的 JWT，网关校验其有效性。 |
| 白名单（Whitelist） | 免令牌放行的路径前缀（认证、Actuator、OpenAPI 文档端点）。 |
| 限流（Rate Limit） | 基于 Redis 令牌桶（RequestRateLimiter），按客户端 IP 计数；登录接口独立更严限流。 |
| 透传上下文头（Passthrough Headers） | 网关校验令牌后写入的 `X-Cornerstone-*` 请求头，下游服务经 `UserContextHolder` 读取。 |
| 路由（Route） | 网关将匹配前缀的请求转发到对应 `lb://service` 的规则。 |
| lb:// | Spring Cloud 负载均衡服务发现标识，配合 Nacos 实现按服务名路由。 |
