# cornerstone-auth — 认证中心

> 认证链路的**第一环**：OAuth2 授权服务器，负责签发与校验 JWT。

## 职责

- 基于 **Spring Authorization Server** 的授权服务器。
- 以 `client_credentials` 授权模式签发 RS256 JWT。
- 提供 `/oauth2/jwks` 端点发布 RSA 公钥，供依赖方校验令牌。
- 令牌额外声明：`client_id`、`scope`。
- 客户端注册：yaml 定义记忆客户端（InMemoryRegisteredClientRepository）。
- Nacos 服务注册（`cornerstone-auth`）。

## 边界

- **签发**在认证中心，**校验**在网关与资源服务；认证中心不校验业务接口令牌。
- 只负责认证（"你是谁"），不负责授权（"你能做什么"）——授权语法规则在网关/资源服务。
- 授权码 + PKCE 留待 v2（见 spec Out of Scope）。

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
| JWKS | JSON Web Key Set：由 `/oauth2/jwks` 暴露的 RSA 公钥集合，供校验方离线获取公钥。 |
