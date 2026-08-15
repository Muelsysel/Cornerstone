# ADR-0006: 数据权限延迟到 v2

Status: accepted — **已实现（v2）**，见下方"实现方式"

部门级数据权限（本部门/本部门及以下/自定义/全部/仅本人）是 RuoYi 最重的机制之一，v1 **不实现**，RBAC 演示到"菜单-权限点"粒度。数据权限以本 ADR 记录为 v2 独立迭代。

Consequences: v1 数据模型不含数据范围字段与规则表，v2 引入时需增量迁移——已预留（用户表含 dept_id 字段，为数据权限留位）。

Considered Options: v1 实现数据权限（拒绝：工程量与复杂度显著增加，且值得独立迭代认真做）。

## 实现方式（v2，已落地）

- **数据模型**：`sys_role.data_scope`（1全部 2自定义 3本部门及以下 4本部门 5仅本人）+ `sys_role_dept`（自定义范围部门关联），Flyway V4 迁移
- **机制**：MyBatis-Plus `DataPermissionInterceptor` + `CornerstoneDataPermissionHandler`（SQL 层拦截 `sys_user` 表查询，自动追加数据范围条件；其他表不拦截避免递归；匿名请求不限制）
- **范围解析**（`DataScopeService`）：取用户所有角色中最严格的 data_scope；自定义范围经 `sys_role_dept` 查询部门集合
- **管理入口**：角色 CRUD 支持 dataScope 字段与自定义范围部门分配（`/system/role/{id}/depts` 回显）
- **偏差**：未采用自定义 `@DataScope` 注解，改用 MyBatis-Plus 官方 DataPermissionInterceptor（SQL 层自动处理，无需注解与 AOP，符合"复用现有"原则）
