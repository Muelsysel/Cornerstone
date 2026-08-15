# Changelog — cornerstone-web

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.4.31] - 2026-08-16

- fix(ux): 登录提交加 loading 早退守卫——按钮 loading 之外，回车连发/快速双击不再触发重复登录请求

**测试方法**：`npm test`（34 用例）+ `npm run build`。

## [1.4.30] - 2026-08-16

- fix(security): nginx 补安全响应头——X-Frame-Options（防点击劫持）/ X-Content-Type-Options（防 MIME 嗅探）/ Referrer-Policy / CSP（禁外部脚本资源，Element Plus 需 style unsafe-inline）；实测页面与 API 均正常

**测试方法**：`npm test`（34 用例）+ `docker compose up -d --build frontend`（验证响应头）。

## [1.4.29] - 2026-08-16

- fix(ux): 5xx 错误提示友好中文「服务器开小差了，请稍后重试」——此前暴露 axios 英文原语；`request.spec.ts` 新增 HTTP 500 回归用例

**测试方法**：`npm test`（34 用例）+ `npm run build`。

## [1.4.28] - 2026-08-16

- fix(ux): 密码输入校验对齐后端契约 6-72——创建用户/重置密码/个人改密三处补 `max: 72`（此前仅 min 6，超长密码前端通过后端 400 拒绝）

**测试方法**：`npm test`（33 用例）+ `npm run build`。

## [1.4.27] - 2026-08-16

- feat(ux): 公告管理页新增「查看」详情弹窗——复用公开 GET /{id} 接口展示正文全文（此前列表仅标题，内容截断不可读）；api 新增 `getAnnouncementDetail`

**测试方法**：`npm test`（33 用例）+ `npm run build`。

## [1.4.26] - 2026-08-16

- fix(security): 重置密码请求体化——`resetUserPassword` 由 query 参数改为 body（明文密码不再进 URL/访问日志/浏览器历史）

**测试方法**：`npm test`（33 用例）+ `npm run build`。

## [1.4.25] - 2026-08-16

- fix(ux): 请求超时（ECONNABORTED）提示友好中文「请求超时，请稍后重试」，不再暴露 axios 英文原语；不清会话不跳登录页（超时≠会话失效）；`request.spec.ts` 新增超时回归用例

**测试方法**：`npm test`（33 用例）+ `npm run build`。

## [1.4.24] - 2026-08-15

- feat(ux): 删除当前页最后一条记录后自动回退一页，避免停留在空页——新增 `utils/pagination.ts`（`pageNumAfterDelete`）并接入用户/参数/角色/字典（类型+数据项）/操作日志/登录日志/公告 7 个分页页面；配套 `pagination.spec.ts`（3 用例）

**测试方法**：`npm test`（32 用例）+ `npm run build`。

## [1.4.23] - 2026-08-15

- fix(ux): dashboard 能力卡片描述区固定高度（卡片高度一致，消除 hover 上移时的布局抖动）

**测试方法**：`npm test`（29 用例）+ `npm run build`。

## [1.4.22] - 2026-08-15

- fix(ux): 用户分配角色弹窗 `el-select` 加 `collapse-tags`（角色多时折叠 + tooltip，避免弹窗挤高）

**测试方法**：`npm test`（29 用例）+ `npm run build`。

## [1.4.21] - 2026-08-15

- docs(ux): 登录页测试账号提示补充 test/admin123（数据权限演示账号）

**测试方法**：`npm test`（29 用例）+ `npm run build`。

## [1.4.20] - 2026-08-15

- refactor: 操作日志业务类型映射提取为 `utils/operlog.ts`（可测纯函数），页面引用工具；新增 `operlog.spec.ts` 2 用例

**测试方法**：`npm test`（29 用例）+ `npm run build`。

## [1.4.19] - 2026-08-15（审查反馈修正）

- fix(ux): 公告页公开查询文案改为"API 公开 + 页面管理需登录"（消除与路由 requireAuth 的语义矛盾）
- feat(ux): 菜单表格图标列加图标预览（`<component :is>` 渲染 + 名称），与表单图标选择器呼应

**测试方法**：`npm test`（27 用例）+ `npm run build`。

## [1.4.18] - 2026-08-15

- refactor: 公告状态展示映射提取为 `utils/announcement.ts`（可测纯函数），页面引用工具；新增 `announcement.spec.ts` 2 用例（文本/标签类型映射）

**测试方法**：`npm test`（27 用例）+ `npm run build`。

## [1.4.17] - 2026-08-15（依赖补丁升级）

- deps: element-plus 2.8.4→2.14.4、vue 3.5.12→3.5.41、axios 1.7.7→1.19.0（同代际最终补丁；跳过 vue-router 5/pinia 4/vite 8/typescript 7 等破坏性大版本）

**测试方法**：`npm test`（25 用例）+ `npm run build`（主包 gzip ~130KB）。

## [1.4.16] - 2026-08-15

- test: `tree.spec.ts` 3 用例（空输入、嵌套树深度优先摊平、叶子节点）

**测试方法**：`npm test`（25 用例）+ `npm run build`。

## [1.4.15] - 2026-08-15（契约注释修正）

- docs: `types/system.ts` User.status 注释对齐实际取值（0 正常/1 停用，原误写 ENABLE/DISABLE 与后端枚举不存在）；`api/auth.ts` 移除过时"后端子代理开发中"注释（POST /auth/login 已就绪）；`api/announcement.ts` 下线注释修正为 PUBLISHED → OFFLINE（原误写 DRAFT）

**测试方法**：`npm test`（22 用例）+ `npm run build`（vue-tsc + vite 产物 gzip 主包 ~130KB）。

## [1.4.14] - 2026-08-15（契约修复）

- fix: 公告编辑 URL 对齐后端 `PUT /demo/announcement/{id}`（此前无 id 路径导致编辑 404，100% 不可用）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，22 用例）。

## [1.4.13] - 2026-08-15（体验）

- feat: 操作日志搜索补「状态」筛选（成功/失败，与登录日志一致）；OperLogQuery 类型补 status

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，22 用例）。

## [1.4.12] - 2026-08-15（测试补充）

- test: `src/router/__tests__/guard.spec.ts`——路由守卫（未登录跳转/登录回首页/无权限 403/放行）；前端单测 22 用例

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，22 用例）。

## [1.4.11] - 2026-08-15（测试补充）

- test: `src/stores/__tests__/user.spec.ts`——user store 登录/重置会话/退出容错；前端单测 18 用例

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，18 用例）。

## [1.4.10] - 2026-08-15（体验）

- fix: HTTP 429（网关限流）统一友好提示「请求过于频繁，请稍后再试」

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.9] - 2026-08-15（功能补全）

- feat: 顶栏下拉加「个人资料」——弹窗展示当前用户资料（用户名/昵称/手机/邮箱/角色/权限点），复用 GET /system/user/profile 端点

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.8] - 2026-08-15（契约修复）

- fix: 公告状态类型对齐——后端整数 0/1/2 vs 前端误用字符串比较（'DRAFT'/'PUBLISHED'）导致状态显示全错、发布/下线按钮永不显示；改数字比较 + 三态显示（草稿/已发布/已下线）；表单移除无效 status 字段（后端强制草稿态）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.7] - 2026-08-15（功能补全）

- feat: 操作/登录日志行操作补「删除」（v-permission `system:log:remove` + 确认弹窗，对齐后端单条删除端点）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.6] - 2026-08-15（功能补全）

- feat: 用户管理页「重置密码」（行操作 + 新密码弹窗，对齐后端 resetPassword 端点）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.5] - 2026-08-15（功能补全）

- feat: 操作/登录日志页「清空」按钮（v-permission `system:log:remove` + 确认弹窗），对齐后端删除/清空端点

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

## [1.4.4] - 2026-08-15（功能补全与修复）

- fix: 登录接口 401（密码错误/禁用）不再误触发会话失效处理（清会话/跳登录），只提示后端 message；补回归用例
- feat: 角色管理「数据范围」——表单单选（1-5）+ 自定义(2)部门树多选弹窗，对齐后端 dataScope/deptIds 契约
- fix: 登录后 redirect 校验（防 `//` 开放重定向）；dashboard 日期改 computed（跨夜刷新）；dict 页清理未用变量

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测）。

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

## [1.3.4] - 2026-08-15（测试补充）

- test: 请求拦截器 Bearer 令牌附加用例（有/无令牌两分支）；单测总数 14

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建，须通过）；`npm test`（vitest 单测，14 用例）。

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
