## 变更说明

<!-- 一句话说清：改了什么、为什么 -->

## 文档导航（必填，对应 AGENTS.md 黄金法则）

- [ ] 已阅读 `CONTEXT-MAP.md`，确认改动涉及的模块
- [ ] 已阅读目标模块 `CONTEXT.md` 与相关 ADR，无冲突
- [ ] 领域概念变化已同步更新 `CONTEXT.md` 词汇表
- [ ] 难逆决策已新增 ADR（如适用）

## 验证记录（必填）

- [ ] 编译通过（`mvn compile`）
- [ ] 测试通过（`mvn test`）
- [ ] 格式通过（`mvn spotless:check`）
- [ ] 测试未依赖外部中间件（MySQL/Redis/Nacos）

## 检查清单

- [ ] 响应统一使用 `Result<T>`，业务错误抛 `BusinessException`
- [ ] 跨服务调用经 `cornerstone-api` 契约（未直连其他服务）
- [ ] 未私自引入依赖版本（版本只存在于父 POM）
