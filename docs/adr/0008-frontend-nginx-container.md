# ADR-0008: 前后端分离部署（Nginx 容器化）

Status: accepted

## 背景

v5 之前前端仅有开发形态（Vite dev server :5173，vite 代理到网关），缺少可交付的生产部署形态；且
docker-compose 未集成前端服务。用户要求"前后端分离部署，前端由 Nginx 容器托管，保留主流技术栈"。

## 决策

- **多阶段 Docker 构建**：`cornerstone-web/Dockerfile` 用 `node:22-alpine` 构建 `dist` → `nginx:1.27-alpine` 托管静态产物。
- **nginx 同源反代**：`/assets/` 走静态 + 强缓存；`/auth` `/system` `/demo` 前缀反代到宿主机网关
  （`host.docker.internal:8080`，后端容器化时改为 `http://cornerstone-gateway:8080`）；浏览器同源访问，无跨域。
- **SPA hash 路由**：`createWebHashHistory`——history 路由下 `/system/user` 等路径刷新会被 nginx 的
  `/system/` 反代吞掉，hash 模式（`#/system/user`）根路径恒为 `/`，与 API 前缀天然隔离。
- **镜像/容器命名**：显式 `image: cornerstone-frontend:latest` + `container_name: cornerstone-frontend`
  （不继承 compose 项目名，避免出现 `ruoyi-*` 命名），端口 `8088:80`。
- **一键启动**：`scripts/start-all.ps1`（依赖检查 → common/api install → 4 服务并行 → 前端 dev 或容器）。

## Consequences

- 前后端分离（独立构建/部署）但生产同源（nginx 一个端口），新增前端页面无需后端配合、无跨域配置。
- 修改部署形态只需动 `Dockerfile`/`nginx.conf`/`docker-compose.yml` 三处，并同步
  `cornerstone-web/README.md`「Docker 部署」章节与根 README。
- 代价：本地联调需起容器（或直接用 dev 模式）；后端若容器化需改 nginx 反代目标。

## Considered Options

- **history 路由 + nginx try_files**（拒绝）：`/system/*` 等前端路径与 API 反代前缀冲突，需复杂 location 区分，脆弱。
- **前端独立静态站点 + 跨域调用**（拒绝）：需网关 CORS 白名单，演示复杂度上升，违背"易用"。
- **前端保留在 Vite dev 形态**（拒绝）：无生产形态，不符合"前后端分离部署"要求。
