# cornerstone-web — Cornerstone 管理后台（前端）

Cornerstone 的前端管理后台，基于 **Vite + Vue 3 + Element Plus + Pinia + Vue Router + Axios**（TypeScript）。

> 说明：本目录是纯前端目录，不属于 Maven 多模块工程，**不加入父 POM 的 modules**。按 [ADR-0005](../../docs/adr/0005-frontend-separate-repo.md) 规划，后续将拆分为独立仓库。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 构建工具 | Vite 5 |
| 框架 | Vue 3（`<script setup>` + TS） |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| HTTP | Axios |

## 目录结构

```
cornerstone-web/
├── index.html
├── vite.config.ts          # 开发代理：/auth、/system、/demo -> 网关 8080
├── src/
│   ├── main.ts             # 应用入口，注册 Element Plus / Pinia / Router
│   ├── App.vue
│   ├── api/                # 接口封装（request.ts 为 axios 封装）
│   │   ├── auth.ts         # 登录 / 退出
│   │   ├── system.ts       # 用户/角色/菜单/部门
│   │   └── announcement.ts # 公告
│   ├── router/             # 路由表 + 登录守卫
│   ├── stores/             # Pinia store（user 会话）
│   ├── layout/             # 后台布局（侧边菜单 + 顶栏 + 主内容）
│   ├── types/              # TS 类型
│   ├── utils/              # storage / 树 工具
│   └── views/
│       ├── login/          # 登录页
│       ├── dashboard/      # 首页
│       ├── system/         # 用户 / 角色 / 菜单 / 部门
│       └── demo/announcement/  # 公告
```

## 快速开始

### 1. 启动后端依赖与微服务

后端 4 个服务需先启动（详见仓库根 [README](../README.md) 与 [run-demo.md](../../docs/guides/run-demo.md)）：

```bash
# 启动依赖（Nacos / MySQL / Redis）
docker compose up -d

# 启动 4 个服务（4 个终端；登录端点正在开发，最终为 POST /auth/login）
mvn -pl cornerstone-auth,cornerstone-system,cornerstone-demo spring-boot:run
mvn -pl cornerstone-gateway spring-boot:run
```

### 2. 安装依赖并启动前端

```bash
npm install
npm run dev
```

前端默认运行在 http://localhost:5173。

### 3. 登录

- 测试账号：`admin` / `admin123`
- 登录走 `POST /auth/login`，经网关 8080 转发到 auth 服务；成功后返回
  `{ access_token, token_type, expires_in, userId, username, roles }`，
  令牌存入 Pinia + localStorage。

> 登录端点目前由后端子代理并行开发中，若尚未就绪，`/auth/login` 会返回错误提示——前端按约定实现，后端就绪后即可联调。

## 代理说明

开发环境下，`vite.config.ts` 配置了到网关 `http://localhost:8080` 的开发代理，前端无需额外 CORS 配置：

| 前缀 | 目标 | 说明 |
| --- | --- | --- |
| `/auth` | 网关 8080 | 认证（登录/退出/jwks） |
| `/system` | 网关 8080 | 系统管理（用户/角色/菜单/部门） |
| `/demo` | 网关 8080 | 演示业务（公告） |

生产环境：建议在前端部署由 Nginx 等做同路径反向代理到网关，前端代码无需改动。

## 常用命令

```bash
npm run dev          # 开发服务器
npm run build        # 类型检查 + 生产构建（输出 dist/）
npm run preview      # 预览构建产物
npm run type-check   # 仅类型检查
```

## 接口对接约定

- 所有请求走后端统一返回结构 `Result<T>`：`code === 200` 为成功，否则为业务错误；
  `401`（HTTP 或业务码）统一跳转登录页。
- 请求拦截器自动附加 `Authorization: Bearer <token>` 请求头。
- **公告公开查询** `GET /demo/announcement/page` 无需登录（demo 模块白名单）；
  公告的增删改、系统管理的全部操作需要登录后的角色授权。
