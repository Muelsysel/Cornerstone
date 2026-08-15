# Changelog — cornerstone-web

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

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
