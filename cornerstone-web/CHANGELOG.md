# Changelog — cornerstone-web

> **变更记录规范**：每次修改/升级/修复，在本文件顶部新增条目。所有 AI 都是文档维护者（见 AGENTS.md「文档维护义务」）。

## [1.0.0] - 2026-08-15（初始）

- feat: Vue3 + Vite + Element Plus + Pinia + TS 管理后台；登录（POST /auth/login）、布局（菜单分组）、用户/角色（含分配权限树）/菜单/部门/字典/参数/操作日志/登录日志/公告管理页；403/404 页；dashboard
- feat: 权限闭环（v-permission 指令 + 路由守卫 + 菜单过滤 + JWT scope 解码）；修改密码弹窗；日志详情
- fix: status 值统一 '0'/'1'；changeUserStatus URL；实体主键契约（userId/roleId/menuId/deptId）
- deploy: Docker 多阶段构建（node 构建 → nginx 托管 + 反代 /auth /system /demo 到网关）

**测试方法**：`npm run build`（vue-tsc 类型检查 + vite 构建）；`npm run dev` 本地联调（vite 代理到网关 8080）。
