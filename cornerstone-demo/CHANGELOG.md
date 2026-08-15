# Changelog — cornerstone-demo

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.1] - 2026-08-15

- refactor: RSA 公钥解析改用 common `RsaKeyUtils`（与 gateway/auth/system 统一）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest：MockMvc + H2 跑 Flyway，覆盖公开/401/403/状态流转/校验）。

## [1.0.0] - 2026-08-15（初始）

- feat: 公告管理（CRUD/分页/状态流转 草稿→发布→下线）；资源服务器示范（JWT 校验 + 公开白名单 + @PreAuthorize 权限点）；审计字段自动填充；**新模块活模板**（克隆指引见 CONTEXT.md）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest：MockMvc + H2 跑 Flyway，覆盖公开/401/403/状态流转/校验）。
