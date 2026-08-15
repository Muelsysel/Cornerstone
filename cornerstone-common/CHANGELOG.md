# Changelog — cornerstone-common

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。
> 条目格式：`## [版本] - YYYY-MM-DD` → 变更类型（feat/fix/refactor/docs/test）+ 说明 + 涉及接口与测试。

## [1.2.3] - 2026-08-15

- fix: `GlobalExceptionHandler` 补 `HttpMessageNotReadableException` 处理（请求体 JSON 格式错误此前走兜底 500，现正确返回 400 BAD_REQUEST）；`GlobalExceptionHandlerTest` 补用例

**测试方法**：`mvn test -pl cornerstone-common`（含 GlobalExceptionHandlerTest 6 用例）。

## [1.2.2] - 2026-08-15

- test: `UserContextFilterTest` 3 用例（透传头填充上下文、链内可见、请求结束清理、匿名不受影响）

**测试方法**：`mvn test -pl cornerstone-common`（UserContextFilterTest + UserContextHolderTest + RsaKeyUtilsTest + ResultTest + GlobalExceptionHandlerTest）。

## [1.2.1] - 2026-08-15

- test: `GlobalExceptionHandlerTest` 5 用例（业务码透传/字段校验 400/约束违规/404/兜底不泄露内部细节）

**测试方法**：`mvn test -pl cornerstone-common`（Result 序列化契约、UserContext 解析、RsaKeyUtils、GlobalExceptionHandler）。

## [1.2.0] - 2026-08-15

- feat: `RsaKeyUtils` 统一 RSA PEM 解析（公钥 SPKI/私钥 PKCS8，容忍头尾与换行），gateway/auth/system/demo 四处共用（消除重复实现，AGENTS 四处密钥一致约定落地）
- test: RsaKeyUtilsTest（公/私钥带 PEM 头尾 round-trip、非法内容抛异常）

**测试方法**：`mvn test -pl cornerstone-common`（Result 序列化契约、UserContext 解析、RsaKeyUtils）。

## [1.1.0] - 2026-08-15

- feat: `JacksonTimeConfig` 统一 java.time 序列化格式（LocalDateTime → `yyyy-MM-dd HH:mm:ss`，附反序列化），前端时间列直接展示友好格式（前后端契约）
- fix: `UserContextHolder.parse` 对非法 deptId 透传头降级为 null（与 userId 一致，防外部可控输入抛 500）
- test: 新增非法 deptId 解析用例

**测试方法**：`mvn test -pl cornerstone-common`（Result 序列化契约、UserContext 解析）。

## [1.0.0] - 2026-08-15（初始）

- feat: 统一返回 `Result<T>`（@JsonIgnore 排除 isSuccess 防契约污染）、错误码 `ErrorCode`/`IErrorCode`、业务异常 `BusinessException`、全局异常处理器（自动配置注册，`Type.SERVLET` 条件跳过 Reactive）
- feat: 用户上下文 `UserContext`/`UserContextHolder`/`UserContextFilter`（网关透传头解析，非数字 userId 容错）
- refactor: `jakarta.validation-api` 改 compile 传递；自动配置加 `@ConditionalOnWebApplication(type = SERVLET)`

**测试方法**：`mvn test -pl cornerstone-common`（Result 序列化契约、UserContext 解析）。
