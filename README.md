# Cornerstone 🏗️

**文档约束驱动的 AI 协作 Spring Cloud 脚手架** —— 快速使用 AI 开发 Spring Cloud 项目的基石。

借鉴 RuoYi 的工程思想（RBAC、前后端分离、模块化），零代码借鉴，从零自研。核心卖点不是代码，而是**让任何 AI 都能通过文档快速上手、遵循统一工作流、产出符合规范的代码**。

## 核心理念：文档约束

仓库里的文档不是摆设，而是每个 AI 必须遵守的**契约**：

> 先读文档，再写代码。文档与代码同改。

配套行为准则（八荣八耻，见 [AGENTS.md](AGENTS.md)）：不猜接口、不模糊执行、不臆想业务、不造新接口、不跳过验证、不破坏架构、不装懂、不盲改。

## 技术栈

Java 17 · Spring Boot 3.2 · Spring Cloud 2023.0 · Spring Cloud Alibaba (Nacos) · Spring Cloud Gateway（含 Redis 限流）· OpenFeign · **Spring Security + OAuth2 (Spring Authorization Server)** · MyBatis-Plus · Redis · MySQL 8 · Flyway · Springdoc OpenAPI · Vue3 + Element Plus（前端）

## 模块结构

| 模块 | 类型 | 职责 |
| --- | --- | --- |
| `cornerstone-common` | 库 | 统一返回 `Result<T>`、错误码、全局异常、用户上下文、工具 |
| `cornerstone-api` | 库 | 跨服务 Feign 契约（契约先行，禁止服务间直连） |
| `cornerstone-gateway` | 服务 | 统一入口：路由、令牌校验、白名单、跨域、**Redis 限流** |
| `cornerstone-auth` | 服务 | OAuth2 授权服务器 + **用户登录**（POST /auth/login，签发带权限 JWT） |
| `cornerstone-system` | 服务 | RBAC：用户/角色/菜单/部门/字典/参数/操作日志/登录日志 + **部门数据权限** |
| `cornerstone-demo` | 服务 | 公告管理——**新模块活模板** |
| `cornerstone-web` | 前端 | 管理后台（Vue3 + Vite + Element Plus；非 Maven 模块，见 ADR-0005） |

## 已实现能力（v1–v4）

- **认证**：client_credentials（服务间）+ 用户名密码登录（admin/admin123）+ JWT（RS256，携带角色与权限）
- **权限**：RBAC（菜单-按钮权限点，前端角色分配权限树）+ 部门数据权限（全部/自定义/本部门及以下/本部门/仅本人）+ 前端 v-permission/路由守卫
- **用户自助**：个人中心（个人信息 + 修改密码，旧密码验证）
- **审计**：操作日志（@OperLog AOP + 详情查看）+ 登录日志（成功/失败自动记录）
- **治理**：服务间内部令牌认证（ADR-0007）、actuator 健康检查、网关 Redis 限流、网关访问日志、CI（编译/测试/Spotless/文档校验）
- **文档约束**：AGENTS.md（黄金法则 + 八荣八耻）、CONTEXT-MAP、7 条 ADR、项目技能 `skills/cornerstone-dev`

## 快速开始

### 方式一：docker-compose（推荐）

```bash
# 1. 启动依赖（Nacos / MySQL / Redis）
docker compose up -d

# 2. 启动服务（四个终端或 IDE）
mvn -pl cornerstone-auth,cornerstone-system,cornerstone-demo spring-boot:run
mvn -pl cornerstone-gateway spring-boot:run

# 3. 演示认证链路（详见 docs/guides/run-demo.md，已实测）
curl -u "cornerstone-client:cornerstone-secret" -X POST \
  "http://localhost:8080/auth/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read"
```

### 方式二：手动（无 Docker）

详见 [docs/guides/run-demo.md](docs/guides/run-demo.md)（Nacos/MySQL/Redis 手动安装说明）。

### 前端管理后台（Vue3）

```bash
cd cornerstone-web
npm install
npm run dev     # http://localhost:5173，admin / admin123
```

详见 [cornerstone-web/README.md](cornerstone-web/README.md)。

## 文档导航（AI 与开发者必读）

| 想做什么 | 读什么 |
| --- | --- |
| 了解整个项目怎么运转 | [CONTEXT-MAP.md](CONTEXT-MAP.md) |
| 在某个模块里开发 | 该模块的 `CONTEXT.md` + 相关 ADR |
| 理解为什么这么设计 | [docs/adr/](docs/adr/) |
| 让 AI 按项目工作流开发 | `skills/cornerstone-dev/SKILL.md`（含规范与验证） |
| 工程技能如何读这些文档 | [docs/agents/](docs/agents/) |
| v1 规范与验收 | [docs/specs/001-cornerstone-v1.md](docs/specs/001-cornerstone-v1.md) |

## AI 协作指南

1. **让 AI 读 `AGENTS.md`** —— 它是每个 AI 的入口，说明黄金法则、八荣八耻、文档导航
2. **安装 `skills/cornerstone-dev`** —— 把 `skills/cornerstone-dev/` 安装到你的 AI 工具（Claude Code / Cursor / DSH / Codex），AI 加载后即按项目工作流开发
3. **先导航后动工** —— 任何改动前：CONTEXT-MAP → 模块 CONTEXT → 相关 ADR

## 反臃肿机制

- **API 契约先行**：跨服务调用只能经 `cornerstone-api` 定义
- **模块边界记录**：每个模块 CONTEXT.md 记录职责/边界/不做的事
- **ADR 门槛**：新模块、新功能、依赖升级必须过 ADR
- **依赖集中管理**：版本只在父 POM BOM 定义
- **活模板**：新模块照 `cornerstone-demo` 克隆

## 路线图

- **v1**（当前）：核心五模块 + demo、认证链路、CI 门禁、文档体系
- **v2+**：授权码+PKCE 前端接入 · 数据权限 · 剩余系统功能 · 前端仓库（Vue3 独立）· 扩展服务（文件/消息/监控）

## 许可证

[Apache-2.0](LICENSE)
