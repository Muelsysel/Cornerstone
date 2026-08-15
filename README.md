<div align="center">

# 🏗️ Cornerstone

**文档约束驱动的多 AI 协作 Spring Cloud 脚手架**

采用业界成熟工程范式（RBAC · 前后端分离 · 模块化），**零依赖第三方业务代码、从零自研**。
核心卖点不是代码，而是：**任何 AI 读文档即可上手、遵循统一工作流、产出符合规范的代码**。

[![CI](https://img.shields.io/github/actions/workflow/status/Muelsysel/cornerstone/ci.yml?branch=master&style=flat-square&logo=githubactions&logoColor=white&label=CI)](https://github.com/Muelsysel/cornerstone/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/Muelsysel/cornerstone?style=flat-square&color=4f46e5)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6db33f?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0-6db33f?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883?style=flat-square&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)

</div>

---

## 📖 核心理念：文档约束（Documentation-Constraint）

> **先读文档，再写代码。文档与代码同改。**

Cornerstone 仓库里的文档不是摆设，而是每个 AI 必须遵守的**契约**。这套机制让团队里的人类与 AI
（以及不同家的 AI）产出**风格一致、边界清晰、可维护**的代码：

- **黄金法则**（[AGENTS.md](AGENTS.md)）：动代码前必须完成文档导航（CONTEXT-MAP → 模块 CONTEXT → ADR）
- **文档维护义务**：每个 AI 都是文档维护者——改代码必须同步该模块的 `CONTEXT.md`（职责/词汇表）与
  `CHANGELOG.md`（变更记录），并按"测试方法"全量验证后再提交
- **决策留痕**：难逆决策写进 [ADR](docs/adr/)（当前 9 条），后来的 AI 不再重复踩坑
- **可执行的技能**：`skills/cornerstone-dev` 是 AI 的强制工作流，装进 Claude Code / Cursor / DSH / Codex 即生效

## ✨ 特性

| 领域 | 能力 |
| --- | --- |
| 🔐 **认证** | Spring Authorization Server（client_credentials + 用户登录）；BCrypt + RS256 JWT（携带角色与权限 scope）；服务间内部令牌认证 |
| 🛡️ **权限** | RBAC 全链路（菜单-按钮权限点 + 前端角色分配权限树）；**部门数据权限**（全部/自定义/本部门及以下/本部门/仅本人）；前端 `v-permission` + 路由守卫双保险 |
| 👤 **用户自助** | 个人中心：查看/修改资料、修改密码（旧密码验证） |
| 📋 **审计** | 操作日志（AOP 自动记录 + 详情查看）、登录日志（成功/失败自动入库） |
| 🚦 **治理** | 网关 Redis 限流、网关访问日志、actuator 健康检查、CI 门禁（编译/测试/Spotless/文档校验） |
| 🧩 **模块化** | 7 个模块（4 服务 + 2 库 + 1 前端）；**API 契约先行**（禁止服务间直连）；新模块照 `cornerstone-demo` 活模板克隆 |

> ⚠️ **生产安全须知**：演示环境的内部令牌（`cornerstone.internal-token`）、数据库密码、Nacos 凭据为**硬编码默认值**，仅用于本地/CI 演示。生产部署必须通过环境变量覆盖（如 `SPRING_DATASOURCE_PASSWORD`、`CORNERSTONE_INTERNAL_TOKEN`），并更换 JWT 签名密钥对（`rsa-private.pem`/`rsa-public.pem` 与各服务 `public-key`）。
| 🎨 **前端** | Vue3 + Vite + Element Plus（**按需引入**，主包 gzip ≈ 130KB）；「基石蓝」设计语言；nginx 容器化部署（前后端分离） |
| 📊 **数据** | MySQL 8（Flyway 增量迁移）+ Redis + MyBatis-Plus + Nacos 注册配置 |

## 🏛️ 架构

```
┌──────────────┐      ┌───────────────────────────────────────────┐
│   Browser    │ ───▶ │ cornerstone-web · nginx (:8088)           │
└──────────────┘      │ Vue3 + Vite + Element Plus（SPA + 反代）   │
                      └────────────────────┬──────────────────────┘
                                           │ /auth · /system · /demo
                      ┌────────────────────▼──────────────────────┐
                      │ cornerstone-gateway (:8080)               │
                      │ 令牌校验 · Redis 限流 · 路由 · 访问日志     │
                      └─────────┬──────────────────────┬──────────┘
                                │                      │
                 ┌──────────────▼─────┐   ┌────────────▼──────────┐
                 │ cornerstone-auth   │   │ cornerstone-system    │
                 │ OAuth2 + 用户登录  │   │ RBAC + 部门数据权限     │
                 │ (:8081)            │   │ (:8082)               │
                 └──────────────┬─────┘   └────────────┬──────────┘
                                │    Feign 契约（cornerstone-api） │
                                └──────────────┬──────────────┘
                                               │
                                   ┌───────────▼───────────┐
                                   │ cornerstone-demo      │
                                   │ 公告业务 · 活模板 (:8083) │
                                   └───────────────────────┘
    基础设施（docker compose）：Nacos · MySQL 8（宿主 3307）· Redis
```

**模块一览**

| 模块 | 类型 | 职责 |
| --- | --- | --- |
| `cornerstone-common` | 库 | 统一返回 `Result<T>`、错误码、全局异常、用户上下文、工具 |
| `cornerstone-api` | 库 | 跨服务 Feign 契约与 DTO（契约即文档，禁止服务间直连） |
| `cornerstone-gateway` | 服务 | 统一入口：路由、令牌校验、白名单、跨域、Redis 限流、访问日志 |
| `cornerstone-auth` | 服务 | OAuth2 授权服务器 + 用户登录（签发带权限 JWT）+ 登录日志投递 |
| `cornerstone-system` | 服务 | RBAC（用户/角色/菜单/部门/字典/参数/日志）+ 部门数据权限 |
| `cornerstone-demo` | 服务 | 公告管理——**新模块活模板**（克隆指引见其 CONTEXT.md） |
| `cornerstone-web` | 前端 | 管理后台（Vue3 + Vite + Element Plus，非 Maven 模块，见 ADR-0005） |

## 🚀 快速开始

### 前置要求

- JDK 17 · Maven 3.9+ · Node.js 20+ · Docker（推荐）

### 1. 启动依赖（Nacos / MySQL / Redis）

```bash
docker compose up -d
```

### 2. 启动后端服务

```bash
# 方式 A：一键脚本（Windows，依赖检查 + common/api 安装 + 4 服务并行 + 前端，日志输出到 logs/）
#   默认 JDK：C:\Dev\Lang\JAVA\JAVA17；Maven：D:\.develop\apache-maven-3.9.5\bin\mvn.cmd
#   与本机路径不同时加参数：-JavaHome <jdk目录> -MavenCmd <mvn.cmd 路径>
powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1

# 方式 B：手动（4 个终端，详见 docs/guides/run-demo.md）
mvn -pl cornerstone-auth,cornerstone-system,cornerstone-demo spring-boot:run   # 终端 1
mvn -pl cornerstone-gateway spring-boot:run                                    # 终端 2
```

### 3. 启动前端

```bash
# 方式 A：开发模式（vite 代理到网关 8080）
cd cornerstone-web && npm install && npm run dev     # http://localhost:5173

# 方式 B：生产形态（nginx 容器化，前后端分离，页面与 API 同源）
docker compose up --build frontend                   # http://localhost:8088
```

### 4. 登录验证

- 测试账号：`admin / admin123`（另有 `test / admin123` 演示部门数据权限）
- 服务间认证演示（client_credentials）：

```bash
curl -u "cornerstone-client:cornerstone-secret" -X POST \
  "http://localhost:8080/auth/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read"
```

> 完整演示链路（数据权限 / 服务间认证 / 限流）见 [docs/guides/run-demo.md](docs/guides/run-demo.md)。

## 📚 文档导航（AI 与开发者必读）

| 想做什么 | 读什么 |
| --- | --- |
| 了解整个项目怎么运转 | [CONTEXT-MAP.md](CONTEXT-MAP.md)（模块地图 + 词汇表 + 测试总览） |
| 在某个模块里开发 | 该模块的 `CONTEXT.md` + 相关 ADR |
| 理解为什么这么设计 | [docs/adr/](docs/adr/)（8 条决策记录） |
| 让 AI 按项目工作流开发 | `skills/cornerstone-dev/SKILL.md` |
| 工程技能如何读这些文档 | [docs/agents/](docs/agents/) |
| 一键跑通演示 | [docs/guides/run-demo.md](docs/guides/run-demo.md) |

## ⚡ 性能指标（实测）

| 指标 | 数值 |
| --- | --- |
| 前端主包 | ~390KB（gzip **130KB**）——Element Plus 按需引入 + 路由级分包 |
| nginx gzip | 文本资源压缩 **66.5%**（主包 390KB → 130KB） |
| 前端生产构建 | ~5s（Vite 7，含 vue-tsc 类型检查） |
| 后端全量测试 | ~30s（clean test，H2/mock 不依赖外部服务） |
| 静态资源缓存 | `/assets/` 7 天 immutable |

> 性能细节见 [cornerstone-web/README.md](cornerstone-web/README.md)「前端设计」与 Docker 部署章节。

## 🧪 测试与质量

| 检查项 | 命令 | 说明 |
| --- | --- | --- |
| 后端全量测试 | `mvn clean test` | 编译 + 单测/集成测试（H2/mock，不依赖外部服务） |
| 代码格式 | `mvn spotless:check` | google-java-format（AOSP）；不过则 `spotless:apply` |
| 文档完整性 | `bash scripts/check-docs.sh` | CONTEXT/ADR/CHANGELOG 存在性 + ADR 编号 |
| 前端构建 | `cd cornerstone-web && npm run build` | vue-tsc 类型检查 + vite 构建 |
| 前端单测 | `cd cornerstone-web && npm test` | Vitest + jsdom |
| 端到端 | `scripts/verify-chain.ps1 -UseRunning` | 34 项契约断言（前端容器/认证/分页/隐私/树/审计/公告状态机/字段长度上限/IDOR/锁定/数据权限/JWT deptId/密码绑定/停用拒登/脱敏），见 `docs/guides/run-demo.md` |

> 接口纪律：**新增/修改接口必须补测试并全量跑通后再推送**（AGENTS.md「文档维护义务」）。

## 🤖 AI 协作指南

1. **让 AI 读 `AGENTS.md`** —— 它是每个 AI 的入口：黄金法则、文档维护义务、文档导航
2. **安装 `skills/cornerstone-dev`** —— 复制到你的 AI 工具（Claude Code / Cursor / DSH / Codex），
   AI 加载后即按项目工作流开发（导航 → 定位 → 规范 → 实现 → 验证 → 提交）
3. **先导航后动工** —— 任何改动前：CONTEXT-MAP → 模块 CONTEXT → 相关 ADR
4. **文档与代码同改** —— 改完代码同步更新模块 `CONTEXT.md` 与 `CHANGELOG.md`

## 🗺️ 路线图

- ✅ **v1**：核心五模块 + demo、认证链路、CI 门禁、文档体系
- ✅ **v2+**：授权码+PKCE · 数据权限 · 剩余系统功能 · 前端仓库（Vue3 独立）· 扩展服务
- ✅ **v3–v5**：个人中心 · 服务间内部令牌 · 网关限流 · 文档维护机制 · 前后端分离部署（nginx 容器化）·
  前端风格升级（按需引入 + 基石蓝设计语言）· Vitest 单测
- 🔜 **v6+**：授权码 + PKCE 前端接入 · Playwright 端到端 · 扩展服务（文件/消息/监控）· 多环境部署编排

## 🤝 贡献

欢迎 PR！请先阅读 [AGENTS.md](AGENTS.md) 与 `skills/cornerstone-dev/SKILL.md`，遵循
**导航 → 开发 → 测试 → 文档同步 → 提交** 的流程；提交信息格式 `<type>(<scope>): <中文描述>`。
参见 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [行为准则](CODE_OF_CONDUCT.md)。

## 📄 许可证

[Apache-2.0](LICENSE) © Cornerstone
