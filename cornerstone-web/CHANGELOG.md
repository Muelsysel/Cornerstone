# Changelog — cornerstone-web

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.4.3] - 2026-08-15（功能补全）

- feat: 公告页「发布/下线」操作按钮（DRAFT 显发布、PUBLISHED 显下线，对齐后端 publish/offline 端点）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，12 用例）。

## [1.4.2] - 2026-08-15（性能与导航）

- perf: nginx 开启 gzip（文本类资源压缩，主包 390KB → 130KB，降 66.5%）
- feat: 登录后默认进入首页 dashboard（原直达用户管理）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，12 用例）；`docker compose up --build frontend` 后访问 http://localhost:8088 验证页面与 API 反代。

## [1.4.1] - 2026-08-15（功能补全）

- feat: 用户管理页「分配角色」——行操作按钮 + 多选弹窗（角色列表/已分配回显/全量覆盖），配套后端 `PUT/GET /system/user/{userId}/roles`

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，12 用例）。

## [1.4.0] - 2026-08-15（构建工具升级）

- build: Vite 5 → **7**、@vitejs/plugin-vue 5 → 6（需 Node ≥ 20.19/22.12），`npm audit` 归零（修复 esbuild dev server 漏洞）；构建产物体积持平（主包 gzip ≈ 130KB）；Dockerfile/CI 构建节点同步 node 22

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）；`docker compose up --build frontend` 后访问 http://localhost:8088 验证页面与 API 反代。

## [1.3.3] - 2026-08-15（测试补充）

- test: 新增 `src/utils/__tests__/auth.spec.ts`——JWT scope 解码（字符串/数组/缺失/非法）；单测总数 12

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，12 用例）。

## [1.3.2] - 2026-08-15（测试补充）

- test: 新增 `src/api/__tests__/request.spec.ts`——401 登录失效死循环回归（HTTP/业务码 401 清会话+跳转、200 不清）；单测总数 8
- fix: 修改密码表单校验失败静默返回（消除 unhandled rejection）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，8 用例）。

## [1.3.1] - 2026-08-15（体验细节）

- feat: 全局错误捕获（app.config.errorHandler，渲染错误可定位不静默）；登录表单 autocomplete（浏览器密码管理器友好）；首页欢迎语加当前日期；index.html 补 description/theme-color meta

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.3.0] - 2026-08-15（健壮性与一致性）

- fix: 401 死循环——响应拦截器清会话（localStorage + store.resetSession）再跳登录页，避免与登录页守卫互相弹跳
- fix: 分页每页条数变化时重置页码为 1（8 处分页，避免越界空页）
- feat: 公告页操作按钮按后端权限点 `demo:announcement:edit` 控制（v-permission）；角色分配权限树加 loading
- style: 主题变量收口——页面硬编码色（中性灰/危险红/品牌紫/深色背景）全部收敛到 `theme.css` 的 `--cs-*` 变量，页面零硬编码色
- feat: 时间列展示统一——后端 Jackson 输出 `yyyy-MM-dd HH:mm:ss`（common 1.1.0），前端时间列自动友好，无需改前端

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）；`npm run dev` 本地联调；`docker compose up --build frontend` 后访问 http://localhost:8088 验证页面与 API 反代。

## [1.2.0] - 2026-08-15（前端单元测试地基）

- test: 引入 Vitest + jsdom（`npm test`），首个单测 `src/utils/__tests__/permission.spec.ts` 覆盖 hasPermission/hasRole（admin 放行/权限点精确判断/未登录/多角色任一命中）
- build: 新增 `vitest.config.ts`（与 vite.config.ts 同 @ 别名）；`npm run build` 类型检查覆盖测试文件

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）；`npm run dev` 本地联调；`docker compose up --build frontend` 后访问 http://localhost:8088 验证页面与 API 反代。

## [1.1.0] - 2026-08-15（前端风格升级 + 按需引入）

- style: 统一设计语言（简洁 · 高级 · 非模板感）——品牌主色「基石蓝」深靛蓝 #4F46E5、石板深色侧边栏、深色登录页 + 品牌区、卡片/表格/弹窗细节统一；样式集中于 `src/styles/theme.css`（CSS 变量），与 README「前端设计」章节同步
- perf: Element Plus 改为按需引入（unplugin-auto-import + unplugin-vue-components），产物按组件分包，主包 gzip 约 130KB（原全量引入大幅下降）；`el-config-provider` 注入 zh-cn 语言包
- fix: 删除/变更确认弹窗按钮改为中文（确定/取消）
- fix: 按需引入后 el-table 插槽 `row` 类型显式标注为 `any`（DefaultRow 逆变限制）、树选择器 `value` prop 改为标准 `node-key`，`vue-tsc` 严格检查全绿
- deploy: docker-compose 前端服务显式命名镜像 `cornerstone-frontend:latest`（不再继承 compose 项目名 ruoyi-ai-frontend）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm run dev` 本地联调；`docker compose up --build frontend` 后访问 http://localhost:8088 验证页面与 API 反代。

## [1.0.0] - 2026-08-15（初始）

- feat: Vue3 + Vite + Element Plus + Pinia + TS 管理后台；登录（POST /auth/login）、布局（菜单分组）、用户/角色（含分配权限树）/菜单/部门/字典/参数/操作日志/登录日志/公告管理页；403/404 页；dashboard
- feat: 权限闭环（v-permission 指令 + 路由守卫 + 菜单过滤 + JWT scope 解码）；修改密码弹窗；日志详情
- fix: status 值统一 '0'/'1'；changeUserStatus URL；实体主键契约（userId/roleId/menuId/deptId）
- deploy: Docker 多阶段构建（node 构建 → nginx 托管 + 反代 /auth /system /demo 到网关）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建）；`npm run dev` 本地联调（vite 代理到网关 8080）。
