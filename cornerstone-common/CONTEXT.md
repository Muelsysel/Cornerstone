# Cornerstone Common（公共模块）

所有服务共享的基础契约库，无业务、无状态。

## Language

**Result**:
所有服务接口的统一返回结构（code/message/data）。禁止任何服务另起炉灶。
_Avoid_: 自定义响应体、裸对象返回

**ErrorCode**:
内置错误码枚举（200 成功 / 400 参数 / 401 未认证 / 403 无权限 / 404 不存在 / 405 方法不支持 / 415 媒体类型不支持 / 500 内部错误）。业务错误码从 1000 起，由各模块自定义枚举实现 `IErrorCode`。
_Avoid_: 每个服务各自定义一套错误码

**BusinessException**:
业务异常。业务代码抛出，由全局异常处理器（本模块自动配置注册）转为统一返回结构。
_Avoid_: 吞异常、返回 null

**UserContext / UserContextHolder**:
当前请求用户上下文，由网关透传头（X-Cornerstone-*）解析。服务只信任透传头，禁止自行解析 JWT。
_Avoid_: 在服务里解析令牌

**UserContextFilter**:
从透传头填充上下文（请求结束清理）。**防伪造**：配置 `cornerstone.internal-token` 后，携带透传头的请求须同时带有效 `X-Internal-Token`（网关转发时盖章）才被采信，否则按匿名处理（fail-closed，防直连服务端口伪造身份头）。

## Rules

- **边界**：本模块只放"所有服务都会用的"基础设施。带业务语义的代码放对应服务模块。
- **不做的事**：不定义业务实体、不访问数据库、不包含 WebFlux 适配（reactive 服务自行处理）。
- 自动配置经 `AutoConfiguration.imports` 注册：全局异常处理 + 用户上下文过滤器，服务依赖本模块即生效。
- 新增工具/类必须克制——只加被实际使用的方法，防止公共模块膨胀（见 ADR-0004）。
