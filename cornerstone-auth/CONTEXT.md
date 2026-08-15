# cornerstone-auth — 认证中心

> 认证链路的**第一环**：OAuth2 授权服务器，负责签发与校验 JWT。

## 职责

- 基于 **Spring Authorization Server** 的授权服务器。
- 以 `client_credentials` 授权模式签发 RS256 JWT。
- **用户登录流程（方向 A）**：`POST /login`（JSON {username, password}）用 BCrypt 校验密码，成功后签发带角色权限的用户 JWT。
- 提供 `/oauth2/jwks` 端点发布 RSA 公钥，供依赖方校验令牌。
- 令牌额外声明：`client_id`、`scope`。
- 客户端注册：yaml 定义记忆客户端（InMemoryRegisteredClientRepository）。
- Nacos 服务注册（`cornerstone-auth`）。

## 用户登录流程（方向 A）

`POST /login` 为用户换取 JWT：

1. 调用 `cornerstone-api` 的 `AuthUserClient.findByUsername(username)`（Feign，`/system/auth/user/{username}`）从 system 获取 `UserAuthDTO`（含 BCrypt 密码哈希、角色、权限）。
2. 用 `BCryptPasswordEncoder` 比对密码；用户不存在或密码错误 → 抛 `BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误")`。
3. **登录锁定（ADR-0009）**：连续失败 ≥5 次锁定 5 分钟（Redis `login:fail:{username}` 计数），锁定期间即使密码正确也拒绝；锁定提示返回剩余秒数（Redis TTL），Redis 不可用降级为通用提示。
3. 成功用 `JwtEncoder`（NimbusJwtEncoder，RS256）签发 JWT：
   - `sub`=userId、`username`、`roles`（角色集合）、`scope`（**权限集合**，供下游 `@PreAuthorize` 读 scope）；
   - `deptId`（用户部门 ID，非空才携带；数据权限「本部门/本部门及以下」依赖，网关透传 `X-Cornerstone-Dept-Id`）；
   - `iss`=http://localhost:8081，有效期 12 小时。
4. 返回 `LoginResponse{access_token, token_type:"Bearer", expires_in, userId, username, roles}`。
5. **登录日志（v3）**：不论成功/失败，`LoginService` 经 `LoginLogClient`（Feign，`POST /system/auth/login-log`）把登录日志投递 system 落库；成功 `status=0`「登录成功」，失败 `status=1`「用户名或密码错误」（用户不存在时用请求中的用户名）。投递失败 try-catch 吞掉并 warn，绝不影响登录主流程。

安全链：`config/SecurityConfig` 新增 `@Order(2)` 链放行 `/login`，其余需认证；OAuth2 链（`@Order(1)`）用 `securityMatcher("/oauth2/**")` 限定，避免拦截 `/login`。

> 登录失败的 HTTP 状态按 common `GlobalExceptionHandler` 契约返回 200、`Result` body 内 `code=401`（业务错误码走 body，不映射 HTTP 状态码）。

## 边界

- **签发**在认证中心，**校验**在网关与资源服务；认证中心不校验业务接口令牌。
- 只负责认证（"你是谁"），不负责授权（"你能做什么"）——授权语法规则在网关/资源服务。
- 授权码 + PKCE 留待 v2（见 spec Out of Scope）。
- 用户/角色/权限数据来自 system（经 `AuthUserClient` 契约），认证中心不直连 system 数据库。

## 不做的事

- 不允许自定义 JOSE 算法，只做 RS256。
- 不保存令牌状态（无状态 JWT），不提供吊销/刷新令牌端点（v1）。
- 不管理用户/角色/权限表（那是 system 的职责）。
- 不引入数据库存客户端（v1 用记忆仓库）。

## 词汇表

| 术语 | 定义 |
| --- | --- |
| 令牌（Token） | 授权服务器签发的 JWT，携带 `client_id`、`scope` 等声明，是认证链路中的通行凭证。 |
| 客户端（Client） | 使用本授权服务器的应用方身份（`cornerstone-client`）。网关作为资源服务校验客户端签发的令牌。 |
| 授权服务器（Authorization Server） | 本模块角色：签发令牌、发布公钥，是认证链路的起点。 |
| client_credentials | 客户端凭证授权模式，用于服务间通信换取令牌（本次签发走该模式）。 |
| 用户登录（Login） | `POST /login` 用户名密码换 JWT 的流程，用户认证信息经 `AuthUserClient` 契约从 system 取得。 |
| `AuthUserClient` | 认证支持 Feign 契约（`/system/auth`），auth 登录时从 system 取用户认证信息。 |
| `LoginLogClient` | 登录日志投递 Feign 契约（`/system/auth/login-log`），auth 成功/失败登录时把日志投递 system 落库。 |
| JWKS | JSON Web Key Set：由 `/oauth2/jwks` 暴露的 RSA 公钥集合，供校验方离线获取公钥。 |
