# cornerstone-gateway — 网关

> 认证链路的**咽喉**：统一入口，负责路由转发、令牌校验与跨域。

## 职责

- 统一入口：按前缀路由到 Nacos 服务（`/auth/**`→auth、`/system/**`→system、`/demo/**`→demo、`lb://` 负载均衡）。
- 全局 JWT 校验：白名单路径放行，其余路径必须携带有效令牌。
- 令牌→上下文头透传：`sub`→`X-Cornerstone-User-Id`、`username/preferred_username`→`X-Cornerstone-Username`、`scope`→`X-Cornerstone-Roles`（逗号连接）。头名与 `UserContext` 约定一致。
- 全局 CORS（v1 放开所有来源）。
- 校验失败返回 401 + `Result` 统一结构。
- Nacos 服务注册（`cornerstone-gateway`）。

## 边界

- **校验不签发**：令牌由认证中心签发，网关只校验并透传。
- 网关用**公钥**校验（`NimbusReactiveJwtDecoder`），持有与认证中心一致的 RSA 公钥，私钥只在认证中心。
- 各下游服务（system/demo）作为资源服务可再校验（双保险），但网关是透传头的唯一来源。

## 不做的事

- 不签发/刷新令牌（认证中心的职责）。
- 不实现限流（基础限流预留至后续）。
- 不引入 `spring-boot-starter-web`（WebFlux 环境，绝不放 servlet 依赖）；并排除 common 的 servlet 自动配置。
- 不处理业务逻辑（纯网关横切）。

## 词汇表

| 术语 | 定义 |
| --- | --- |
| 令牌（Token） | 认证中心签发的 JWT，网关校验其有效性。 |
| 白名单（Whitelist） | 免令牌放行的路径前缀（认证、Actuator、OpenAPI 文档端点）。 |
| 透传上下文头（Passthrough Headers） | 网关校验令牌后写入的 `X-Cornerstone-*` 请求头，下游服务经 `UserContextHolder` 读取。 |
| 路由（Route） | 网关将匹配前缀的请求转发到对应 `lb://service` 的规则。 |
| lb:// | Spring Cloud 负载均衡服务发现标识，配合 Nacos 实现按服务名路由。 |
