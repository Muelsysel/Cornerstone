# Changelog — cornerstone-api

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.0] - 2026-08-15（初始）

- feat: 服务名常量 `ServiceConstants`；契约先行——`SystemUserClient`/`AuthUserClient`/`LoginLogClient`（均带唯一 contextId 防 FeignClientSpecification 冲突）、DTO（UserDTO/UserAuthDTO/LoginLogDTO）

**测试方法**：纯契约库（无逻辑测试）；变更后 `mvn -q -DskipTests install -pl cornerstone-api` 供服务编译，全仓 `mvn test` 验证。
