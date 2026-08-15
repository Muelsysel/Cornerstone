# Changelog — cornerstone-common

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。
> 条目格式：`## [版本] - YYYY-MM-DD` → 变更类型（feat/fix/refactor/docs/test）+ 说明 + 涉及接口与测试。

## [1.0.0] - 2026-08-15（初始）

- feat: 统一返回 `Result<T>`（@JsonIgnore 排除 isSuccess 防契约污染）、错误码 `ErrorCode`/`IErrorCode`、业务异常 `BusinessException`、全局异常处理器（自动配置注册，`Type.SERVLET` 条件跳过 Reactive）
- feat: 用户上下文 `UserContext`/`UserContextHolder`/`UserContextFilter`（网关透传头解析，非数字 userId 容错）
- refactor: `jakarta.validation-api` 改 compile 传递；自动配置加 `@ConditionalOnWebApplication(type = SERVLET)`

**测试方法**：`mvn test -pl cornerstone-common`（Result 序列化契约、UserContext 解析）。
