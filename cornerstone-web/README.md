# cornerstone-web — Cornerstone 管理后台（前端）

Cornerstone 的前端管理后台，基于 **Vite + Vue 3 + Element Plus + Pinia + Vue Router + Axios**（TypeScript）。

> 说明：本目录是纯前端目录，不属于 Maven 多模块工程，**不加入父 POM 的 modules**。按 [ADR-0005](../../docs/adr/0005-frontend-separate-repo.md) 规划，后续将拆分为独立仓库。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 构建工具 | Vite 7 |
| 框架 | Vue 3（`<script setup>` + TS） |
| UI 组件库 | Element Plus（**按需引入**：unplugin-auto-import + unplugin-vue-components） |
| 状态管理 | Pinia |
| 路由 | Vue Router 4（hash 模式） |
| HTTP | Axios |

> **按需引入说明**：组件与 API 样式按需打包（见 `vite.config.ts`），自动生成的类型声明
> `src/auto-imports.d.ts`、`src/components.d.ts` 随源码提交，供 `vue-tsc` 校验。
> 函数式组件（消息/弹窗/通知/加载）样式在 `main.ts` 手动引入。

## 前端设计（简洁 · 高级 · 非模板感）

设计语言集中定义在 `src/styles/theme.css`（CSS 变量），改主题只动这一个文件：

- **品牌主色**：「基石蓝」深靛蓝 `#4F46E5`（覆盖 Element Plus 默认亮蓝），见 `--cs-primary` 与 `--el-color-primary` 变量族
- **侧边栏**：石板深色 `#111827`，激活菜单项左侧品牌色条 + 渐隐高亮；**可折叠**（顶栏开关，220px ↔ 64px 过渡，窄屏/专注模式让出内容区）
- **登录页**：深色渐变背景 + 品牌光晕，左品牌区（logo/定位文案）+ 右登录卡片
- **首屏**：`index.html` 内置加载占位（品牌 logo 呼吸动画），Vue 挂载后消失，消除白屏闪烁
- **细节**：卡片统一圆角/轻投影、表格浅色表头、细滚动条、系统字体栈（中文优先 PingFang/Microsoft YaHei）
- **动效克制**：仅 hover 轻过渡，不堆砌动画

> 每次改动主题/布局，同步更新本章节与 `CHANGELOG.md`（AGENTS.md「文档维护义务」）。

## 目录结构

```
cornerstone-web/
├── index.html
├── vite.config.ts          # 开发代理：/auth、/system、/demo -> 网关 8080
├── src/
│   ├── main.ts             # 应用入口（按需引入 + 主题 + 路由/权限指令）
│   ├── App.vue             # el-config-provider（zh-cn 语言包）
│   ├── styles/theme.css    # 设计语言：品牌色/布局/组件细节（CSS 变量）
│   ├── api/                # 接口封装（request.ts 为 axios 封装）
│   │   ├── auth.ts         # 登录 / 退出
│   │   ├── system.ts       # 用户/角色/菜单/部门/字典/参数/日志
│   │   └── announcement.ts # 公告
│   ├── router/             # 路由表 + 登录/权限守卫
│   ├── stores/             # Pinia store（user 会话 + 权限点）
│   ├── directives/         # 自定义指令（permission.vue -> v-permission）
│   ├── layout/             # 后台布局（侧边菜单 + 顶栏 + 主内容）
│   ├── types/              # TS 类型
│   ├── utils/              # storage / 树 / 权限 / JWT 工具
│   └── views/
│       ├── login/          # 登录页
│       ├── dashboard/      # 首页
│       ├── error/          # 403 / 404 页
│       ├── system/         # 用户 / 角色 / 菜单 / 部门 / 字典 / 参数 / 操作日志 / 登录日志
│       └── demo/announcement/  # 公告
```

## 快速开始

### 1. 启动后端依赖与微服务

后端 4 个服务需先启动（详见仓库根 [README](../README.md) 与 [run-demo.md](../../docs/guides/run-demo.md)）：

```bash
# 启动依赖（Nacos / MySQL / Redis）
docker compose up -d

# 启动 4 个服务（4 个终端；spring-boot:run 为阻塞式，每个模块一个终端）
mvn -pl cornerstone-auth spring-boot:run
mvn -pl cornerstone-system spring-boot:run
mvn -pl cornerstone-demo spring-boot:run
mvn -pl cornerstone-gateway spring-boot:run
```

### 2. 安装依赖并启动前端

```bash
npm install
npm run dev
```

前端默认运行在 http://localhost:5173。

### 3. 登录

- 测试账号：`admin` / `admin123`（另有 `test` / `admin123` 演示数据权限）
- 登录走 `POST /auth/login`，经网关 8080 转发到 auth 服务；成功后返回
  `{ access_token, token_type, expires_in, userId, username, roles }`，
  令牌存入 Pinia + localStorage。

## Docker 部署（前后端分离，生产形态）

前端容器化：多阶段构建（node 构建 dist → nginx 托管），nginx 同时反向代理 API 到后端网关，
浏览器同源访问（无跨域），一键起全套。

```bash
# 后端 4 服务本地跑起来后，构建并启动前端容器
docker compose up --build frontend

# 访问 http://localhost:8088 （页面 + API 均由 nginx 提供）
```

- `Dockerfile`：`node:22-alpine` 构建 → `nginx:1.27-alpine` 托管
- `nginx.conf`：SPA 路由回退 + `/auth /system /demo` 反代到 `host.docker.internal:8080`（宿主机网关；
  后端若容器化改为 `http://cornerstone-gateway:8080` 即可）
- `docker-compose.yml`：`frontend` 服务（8088:80 + host-gateway）

## 代理说明

开发环境：`vite.config.ts` 配置了到网关 `http://localhost:8080` 的开发代理，前端无需额外 CORS 配置：

| 前缀 | 目标 | 说明 |
| --- | --- | --- |
| `/auth` | 网关 8080 | 认证（登录/退出/jwks） |
| `/system` | 网关 8080 | 系统管理（用户/角色/菜单/部门） |
| `/demo` | 网关 8080 | 演示业务（公告） |

生产环境：由 nginx 同路径反代到网关（见 Docker 部署），前端代码无需改动。

## 常用命令

```bash
npm run dev          # 开发服务器
npm run build        # 类型检查 + 生产构建（输出 dist/）
npm run preview      # 预览构建产物
npm test             # 单元测试（Vitest，jsdom）
npm run type-check   # 仅类型检查
```

> 单元测试：`npm test`（Vitest）。工具函数单测放 `src/**/__tests__/*.spec.ts`；
> store 涉及 localStorage，环境为 jsdom（见 `vitest.config.ts`）。
> 修改前端逻辑后需 `npm test` + `npm run build` 均通过再推送（AGENTS.md「文档维护义务」）。

## 接口对接约定

- 所有请求走后端统一返回结构 `Result<T>`：`code === 200` 为成功，否则为业务错误；
  `401`（HTTP 或业务码）统一跳转登录页。
- 请求拦截器自动附加 `Authorization: Bearer <token>` 请求头。
- **公告公开查询** `GET /demo/announcement/page` 无需登录（demo 模块白名单）；
  公告的增删改、系统管理的全部操作需要登录后的角色授权。

## 权限控制（RBAC 前端闭环）

- 登录后从 JWT 的 `scope` 声明解码出权限点（如 `system:user:add`），随用户信息存入 Pinia store 与
  localStorage；`admin` 角色直接放行全部。
- `utils/permission.ts` 提供 `hasPermission(perm)` / `hasRole(role)`，供脚本内判断。
- `v-permission` 指令做按钮级权限：无权限点不渲染元素（如 `v-permission="'system:user:add'"`）。
- 侧边菜单按路由 `meta.permission` 过滤：无权限的菜单项隐藏。
- 路由守卫双保险：未登录跳 `/login`；已登录但路由声明了 `meta.permission` 且不具备时跳 `/403`。
- `/403`、`/404` 页在 `views/error/`，未匹配路径统一指向 404。
