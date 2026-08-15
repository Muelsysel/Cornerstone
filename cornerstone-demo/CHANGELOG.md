# Changelog — cornerstone-demo

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.11] - 2026-08-15

- docs: CONTEXT.md 同步白名单常量拆分（PUBLIC_GET_PATHS/PUBLIC_OTHER_PATHS）与仅 GET 公开语义

**测试方法**：`mvn test -pl cornerstone-demo`（23 用例）。

## [1.0.10] - 2026-08-15

- test: `AnnouncementControllerTest` 补发布/下线契约用例（POST /{id}/publish、POST /{id}/offline 带权 200；@Transactional 回滚防污染共享种子数据）

**测试方法**：`mvn test -pl cornerstone-demo`（23 用例）。

## [1.0.9] - 2026-08-15（安全收紧）

- fix(security): 公开白名单仅放行 GET 公告读接口——此前 `/demo/announcement/*` 匹配所有 HTTP 方法（写操作虽方法级 @PreAuthorize 兜底，但 URL 层即应拒绝）；无 token 写操作响应由 403 变为 401（更符合未认证语义），对应测试同步

**测试方法**：`mvn test -pl cornerstone-demo`（21 用例）。

## [1.0.8] - 2026-08-15

- fix(perf): 分页拦截器补 `maxLimit=500`（与 system 一致，防超大 pageSize 拖垮数据库）

**测试方法**：`mvn test -pl cornerstone-demo`（21 用例）。

## [1.0.7] - 2026-08-15

- test: `MyMetaObjectHandlerTest` 4 用例（登录用户回填 createBy/updateBy、匿名回退 system、更新只触 update 字段、空用户名回退）

**测试方法**：`mvn test -pl cornerstone-demo`（MyMetaObjectHandlerTest + AnnouncementControllerTest + AnnouncementServiceImplTest）。

## [1.0.6] - 2026-08-15

- fix(隐私): 公告详情接口游客访问非已发布公告按不存在处理（防草稿/下线详情泄露），管理端不受影响——端到端实测

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest + AnnouncementServiceImplTest：MockMvc + H2 跑 Flyway + 纯单元）。

## [1.0.5] - 2026-08-15

- fix(隐私): 公开分页接口未认证时强制只返回已发布（此前游客可见草稿/下线公告）；已登录管理端查看全部——端到端实测（游客 0 草稿 / 管理 9 草稿）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest + AnnouncementServiceImplTest：MockMvc + H2 跑 Flyway + 纯单元）。

## [1.0.4] - 2026-08-15

- perf: V4 迁移补 `announcement(create_time)` 索引（分页倒序排序提速，活模板示范）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest + AnnouncementServiceImplTest：MockMvc + H2 跑 Flyway + 纯单元）。

## [1.0.3] - 2026-08-15

- test: `AnnouncementServiceImplTest` 8 用例（标题必填/强制草稿/作者自动填充/状态单向流转/删除保护）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest + AnnouncementServiceImplTest：MockMvc + H2 跑 Flyway + 纯单元）。

## [1.0.2] - 2026-08-15

- feat: 公告作者自动填充（创建时取网关透传用户名，防前端伪造）；V3 迁移补 `announcement.author` 列，实体同步（补全前端 author 列的契约缺口）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest：MockMvc + H2 跑 Flyway，覆盖公开/401/403/状态流转/校验）。

## [1.0.1] - 2026-08-15

- refactor: RSA 公钥解析改用 common `RsaKeyUtils`（与 gateway/auth/system 统一）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest：MockMvc + H2 跑 Flyway，覆盖公开/401/403/状态流转/校验）。

## [1.0.0] - 2026-08-15（初始）

- feat: 公告管理（CRUD/分页/状态流转 草稿→发布→下线）；资源服务器示范（JWT 校验 + 公开白名单 + @PreAuthorize 权限点）；审计字段自动填充；**新模块活模板**（克隆指引见 CONTEXT.md）

**测试方法**：`mvn test -pl cornerstone-demo`（AnnouncementControllerTest：MockMvc + H2 跑 Flyway，覆盖公开/401/403/状态流转/校验）。
