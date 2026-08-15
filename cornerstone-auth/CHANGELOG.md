# Changelog — cornerstone-auth

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.12] - 2026-08-16

- fix(security): 安全链放行 OPTIONS 预检（直连服务/跳过网关时跨域预检此前 401）

**测试方法**：`mvn test -pl cornerstone-auth`（17 用例）。

## [1.0.11] - 2026-08-16

- fix(security): `LoginRequest` 补 `@Size` 长度上限——用户名 ≤64（Redis key/日志防超大值）、密码 ≤72（BCrypt 仅处理前 72 字节，超长静默截断会致两个不同长密码判同）；随 common 补 validation 实现后真实生效；`LoginControllerTest` 新增超长用户名/密码 400 拒绝用例（不触发服务调用）

**测试方法**：`mvn test -pl cornerstone-auth`（17 用例）。

## [1.0.10] - 2026-08-16

- docs(api): LoginController 补 OpenAPI `@Tag`/`@Operation`（登录/退出端点描述完整，对齐 demo 活模板）

**测试方法**：`mvn test -pl cornerstone-auth`（15 用例）。

## [1.0.9] - 2026-08-15

- docs: CONTEXT.md 登录流程补锁定语义（ADR-0009 引用 + 剩余秒数提示）

**测试方法**：`mvn test -pl cornerstone-auth`（15 用例）。

## [1.0.8] - 2026-08-15

- feat(ux): 登录锁定拒绝提示返回剩余锁定秒数（Redis TTL，"请 N 秒后再试"；Redis 不可用降级为不含时间的通用提示）；`LoginControllerTest` 断言同步

**测试方法**：`mvn test -pl cornerstone-auth`（15 用例）。

## [1.0.7] - 2026-08-15

- test: `LoginServiceTest` 7 用例（成功签发并清失败计数、错误密码计数+首次失败设 TTL、后续失败不重置 TTL、Redis 不可用降级放行、计数异常仍拒绝、日志投递失败不阻塞登录、锁定拒绝不重复计数）

**测试方法**：`mvn test -pl cornerstone-auth`（LoginServiceTest 7 用例、LoginControllerTest 5 用例、AuthEndToEndTest）。

## [1.0.6] - 2026-08-15

- test: 账号锁定回归用例（Redis 计数触发锁定 + 锁定拒绝落审计日志）

## [1.0.5] - 2026-08-15

- feat(安全): 登录失败账号锁定——Redis 计数（`login:fail:{username}`，连续 5 次失败锁 5 分钟），与网关限流组成双层防爆破；Redis 不可用时降级不阻塞登录

## [1.0.4] - 2026-08-15

- feat: `POST /auth/logout` 契约端点（前端已调用但后端缺失 → 404；无状态 JWT 下返回成功，未来可接令牌黑名单）；禁用内置 LogoutFilter 避免消费冲突（曾致带认证登出 500）
- fix: `/logout` 加入默认链放行

## [1.0.3] - 2026-08-15

- refactor: RSA 密钥对加载改用 common `RsaKeyUtils`（私钥 PKCS8/公钥 SPKI，与 gateway/system/demo 统一）

## [1.0.2] - 2026-08-15

- feat: JWT issuer 配置化（`cornerstone.auth.issuer`，默认 http://localhost:8081；多环境无需改代码）

## [1.0.1] - 2026-08-15

- chore: 清理 `LoginService.recordLog` 注释中的"八荣八耻"引用（项目文档去八荣八耻展示，行为准则实质保留于工程规范）；无逻辑变更

## [1.0.0] - 2026-08-15（初始）

- feat: Spring Authorization Server（client_credentials + RS256 + JWKS）；用户登录 `POST /login`（BCrypt 校验 + 签发带角色权限 JWT，claims：sub=userId/roles/scope）；登录日志经 `LoginLogClient` 投递 system（成功/失败双路径，含客户端 IP）
- fix: token 端点禁用 CSRF；`PasswordEncoder` 用自定义 Delegating（{noop} 客户端密钥 + 无前缀 BCrypt 用户哈希）；补 loadbalancer 依赖（Feign 服务发现）

**测试方法**：`mvn test -pl cornerstone-auth`（AuthEndToEndTest：client_credentials 链路；LoginControllerTest：登录成功/失败/日志投递）。
