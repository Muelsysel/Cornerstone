# CONTEXT-MAP — Cornerstone 模块地图

> 本文件是所有 AI 与开发者的**第一份必读文档**。动手前先定位你要改动的模块，再读该模块的 `CONTEXT.md` 和相关 ADR。

## 词汇表（已结晶术语）

| 术语 | 定义 |
| --- | --- |
| **Cornerstone** | 本项目的名称。定位：快速使用 AI 开发 Spring Cloud 项目的基石，借鉴 RuoYi 工程思想、零代码借鉴。 |
| **文档约束**（Documentation-Constraint） | 本项目的核心理念：仓库文档（AGENTS.md / CONTEXT-MAP / CONTEXT / ADR / skill）是每个 AI 必须遵守的**契约**，先读文档后写代码，文档与代码同改。 |
| **模块**（Module） | 一个可独立构建、独立部署的 Spring Boot 服务（如 gateway/auth/system），或一个共享库（如 common/api）。每个模块有自己的 `CONTEXT.md`。 |
| **认证链路**（Auth Chain） | gateway → auth → system 的请求认证通路：客户端经网关获取/校验令牌，凭据访问受保护资源。 |
| **API 契约**（API Contract） | `cornerstone-api` 中定义的跨服务 Feign 接口，是服务间通信的契约文档。 |
| **用户登录**（Login） | `cornerstone-auth` 的 `POST /auth/login`：用户名密码（BCrypt）校验后签发带角色与权限（scope）的 JWT；测试账号 admin/admin123。 |
| **数据范围**（DataScope） | 角色级行级数据权限（1全部~5仅本人，见 ADR-0006）：`cornerstone-system` 经 `DataPermissionInterceptor` 对用户查询自动追加条件。 |
| **权限点**（Permission） | 形如 `system:user:list` 的字符串权限标识，存于 JWT scope，供 `@PreAuthorize` 与前端 `v-permission` 使用。 |

## 模块地图

| 模块 | 路径 | 类型 | 职责一句话 |
| --- | --- | --- | --- |
| 公共模块 | `cornerstone-common/` | 共享库 | 统一返回结构、异常体系、工具类、常量、用户上下文持有者 |
| API 契约 | `cornerstone-api/` | 共享库 | 跨服务 Feign 接口与 DTO 定义（契约即文档） |
| 网关 | `cornerstone-gateway/` | 服务 | 统一入口：路由转发、跨域、令牌校验（白名单放行）、透传用户上下文、**Redis 限流** |
| 认证中心 | `cornerstone-auth/` | 服务 | OAuth2 授权服务器 + **用户登录**（POST /auth/login，签发带角色权限 JWT）+ 登录日志投递 |
| 系统服务 | `cornerstone-system/` | 服务 | RBAC（用户/角色/菜单/部门/字典/参数/日志）+ **部门数据权限** + 认证支持接口（/system/auth/**，内部令牌保护） |
| 演示模块 | `cornerstone-demo/` | 服务 | 公告管理——**新模块活模板**，克隆指引见其 CONTEXT.md |
| 前端后台 | `cornerstone-web/` | 前端 | Vue3 管理后台（非 Maven 模块，见 ADR-0005），权限闭环（v-permission/路由守卫） |

## 部署与运行

- 依赖（Nacos/MySQL/Redis）：`docker compose up -d`（MySQL 映射宿主 3307），或见 `docs/guides/run-demo.md` 手动启动
- 服务端口：gateway 8080 · auth 8081 · system 8082 · demo 8083 · 前端 dev 5173
- 演示链路：见 `docs/guides/run-demo.md`（client_credentials / 用户登录 admin/admin123 / 数据权限 / 服务间认证 / 限流）

## 决策记录（ADR）

系统级难逆决策见 `docs/adr/`；模块级决策见各模块 `docs/adr/`。动手前检查是否有 ADR 触及你的改动区域。

## 文档导航顺序（黄金法则）

1. 读本文件（模块地图）
2. 定位目标模块 → 读其 `CONTEXT.md`
3. 检查 `docs/adr/` 与 `<模块>/docs/adr/` 中相关决策
4. 按 `skills/cornerstone-dev/SKILL.md` 的工作流开发
