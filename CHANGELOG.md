# Changelog — Cornerstone（项目级）

> 项目级版本里程碑与仓库级变更记录。模块级细节见各模块 `CHANGELOG.md`
> （common/api/gateway/auth/system/demo/web）。所有 AI 都是文档维护者（AGENTS.md「文档维护义务」）。

## [1.5.6] - 2026-08-15（安全加固）

- feat(安全): 登录失败账号锁定（Redis 计数，5 次/5 分钟）——与网关限流组成双层防爆破，后端审查 P0/P1 全部闭环

## [1.5.5] - 2026-08-15（功能补全与测试深化）

- feat(web): 个人资料弹窗；日志行删除；公告状态类型契约修复（整数 0/1/2 三态，修复显示/按钮隐藏 bug）
- test: SysRoleServiceImpl 8 用例、AnnouncementServiceImpl 8 用例（补齐审查缺口核心服务）；后端用例总数 ~90
- 契约闭环：前端 API 与后端端点全部对齐（含 logout/clean/公告状态）

## [1.5.4] - 2026-08-15（深度修复与功能补全）

- fix(安全/正确性): 网关白名单路径带有效令牌也透传上下文头（此前 /demo/** 免校验放行导致公告管理审计/作者为空）；登录 401 不再误伤会话；redirect 防开放重定向
- fix(权限): V8 日志删除权限点 `system:log:remove`、V9 公告管理权限点 `demo:announcement:edit`（修复 admin scope 缺失导致的 403）
- feat: 日志删除/清空闭环（前后端 + 权限点三处一致）；公告作者自动填充（V3 迁移 author 列）；角色数据范围前端补全（自定义部门树）；菜单图标选择器
- refactor(web): dashboard 响应式断点、主题变量收口、死代码清理；前端单测 15 用例

## [1.5.3] - 2026-08-15（功能补全与验证）

- feat: 用户分配角色（前后端全链路，端到端实测）；公告页发布/下线按钮（对齐后端端点）
- perf: nginx gzip（主包 390KB → 130KB）；登录后默认进 dashboard
- fix(安全): 网关透传头防伪造（X-Cornerstone-* 入口剥除）
- test: 登录限流实测（30 次请求 11 次 429，5/s + burst 10 生效）

## [1.5.2] - 2026-08-15（安全与测试续批）

- fix(安全): 网关透传头防伪造（入口剥除 X-Cornerstone-*，含白名单路径；防身份伪造）；登录限流 bean @Primary/@Qualifier
- test: AuthUserSupportService/GlobalExceptionHandler/TokenAuthGlobalFilter 防伪造回归；verify-chain 支持 -UseRunning
- feat: 仓库元数据（描述/主题）+ CODE_OF_CONDUCT + Issue 模板；Vite 7 升级（audit 归零）；ADR-0008

## [1.5.1] - 2026-08-15（工程优化续批）

- fix(安全): 登录接口独立限流（/auth/login 5/s + burst 10）；限流 bean @Primary/@Qualifier 修复（Gateway 自动配置单 bean 需求），端到端 7 项链路实测通过
- refactor: RSA 密钥解析抽公共 `RsaKeyUtils`（4 处统一，含单测）；参数缓存一致性修复（evict 旧 key + null 不写）
- feat(web): Vite 7 升级（npm audit 归零）；登录页/布局窄屏响应式；全局错误捕获；JWT scope 解码与 401 死循环回归测试（前端单测 12 用例）
- docs: 新增 ADR-0008（前后端分离 Nginx 容器化）；AGENTS/模块 CONTEXT 同步；根 CHANGELOG 建立

## [1.5.0] - 2026-08-15（工程优化批次）

- docs: 重写根 README 为开源仓库风格（徽章/特性/架构图/快速开始/贡献）；新增 `CONTRIBUTING.md`
- ci: GitHub Actions 新增前端 job（npm ci + build + test），CI 覆盖后端与前端
- feat: 一键启动脚本 `scripts/start-all.ps1`（依赖检查 + 4 服务并行 + 前端，支持 -Stop）
- fix(安全): 操作日志密码脱敏（OperLogAspect，含回归测试）；内部令牌恒定时间比较；CORS 白名单收紧
- fix: 角色保存事务上移；错误码枚举化（消除 1003 冲突）；deptId 透传头解析容错
- feat: JWT issuer 配置化；Jackson 统一时间格式（LocalDateTime → yyyy-MM-dd HH:mm:ss）
- fix(web): 401 死循环修复（清会话再跳登录）；分页 size 变化重置页码；公告页权限控制；主题变量收口
- test: 新增 OperLogAspectTest / DataScopeServiceTest / UserContextHolder 非法 deptId 用例

## [1.0.0] - 2026-08-15（v1–v5 里程碑）

- v1：核心五模块（common/api/gateway/auth/system）+ demo 活模板；client_credentials 认证链路；CI 门禁；文档体系（AGENTS/CONTEXT-MAP/ADR/skill）
- v2+：用户登录（POST /auth/login，BCrypt + RS256 JWT 携带角色权限）；RBAC 全量管理功能；数据权限（部门 5 级）；前端仓库（Vue3 独立）
- v3：授权码+PKCE 前端接入规划；个人中心（资料/改密）；操作/登录日志
- v4：服务间内部令牌认证（ADR-0007）；网关 Redis 限流；访问日志；关键约定固化进 skill
- v5：文档维护机制（每模块 CHANGELOG + 测试方法章节 +「每个 AI 都是文档维护者」）；前后端分离部署（nginx 容器化 + 反代）；移除八荣八耻展示；前端风格升级（基石蓝设计语言 + Element Plus 按需引入）；Vitest 单测
