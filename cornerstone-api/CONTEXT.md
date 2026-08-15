# Cornerstone API（契约模块）

跨服务 Feign 契约的唯一载体：服务间通信只能经本模块定义，禁止直连或复制 DTO。

## Language

**契约（Contract）**:
本模块中定义的 Feign 接口与共享 DTO。任何跨服务调用的"事实来源"。
_Avoid_: 各服务私有的远程调用接口

**服务名常量（ServiceConstants）**:
所有服务名的唯一来源（如 `SYSTEM_SERVICE = "cornerstone-system"`）。路由与 Feign 客户端统一引用，禁止硬编码。
_Avoid_: 在配置或代码里直接写服务名字符串

## Rules

- **边界**：只放接口定义、DTO、常量。不放实现、不放业务逻辑。
- **不做的事**：不依赖具体服务、不包含 Spring Boot 启动逻辑（本模块是纯库）。
- 新增契约必须同时满足：接口注释说明用途、DTO 带校验注解、变更走 ADR 门槛（见 ADR-0004）。
- 示例契约：`SystemUserClient.getUserById`——新契约照此模式定义。
