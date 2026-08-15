# 运行指南：认证链路演示

> 状态：**已验证** ✅（T10 完成——2026-08 实测 7 步链路全通）

## 前提

- Docker Desktop（推荐）或手动安装 Nacos 2.3 / MySQL 8 / Redis 7
- JDK 17 + Maven 3.9（或直接用项目自带 `./mvnw`）

## 第一步：启动依赖

```bash
docker compose up -d
# 验证：Nacos 控制台 http://localhost:8848/nacos
#       MySQL localhost:3307 root/cornerstone（库 cornerstone_system、cornerstone_demo 由初始化脚本自动创建）
#       Redis localhost:6379
```

> 说明：compose 将 MySQL 映射到宿主 **3307** 端口（避免与本机已有 MySQL 的 3306 冲突）；若 3306 空闲可改回 `3306:3306` 并同步各服务 `application.yml` 的 jdbc url。

无 Docker：分别安装 Nacos（standalone 模式）、MySQL 8（执行 `docker/mysql/init/01-create-databases.sql`）、Redis，配置同 docker-compose。

## 第二步：启动服务

```powershell
# 终端 1（认证中心）
$env:JAVA_HOME = "C:\Dev\Lang\JAVA\JAVA17"
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" spring-boot:run -pl cornerstone-auth

# 终端 2（系统服务）
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" spring-boot:run -pl cornerstone-system

# 终端 3（演示服务）
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" spring-boot:run -pl cornerstone-demo

# 终端 4（网关）
& "D:\.develop\apache-maven-3.9.5\bin\mvn.cmd" spring-boot:run -pl cornerstone-gateway
```

各服务注册到 Nacos（localhost:8848）后即可经网关访问。

## 第三步：演示认证链路（已实测）

```bash
# 1. 拿令牌（client_credentials，Basic Auth 客户端凭证，经网关转发）
TOKEN=$(curl -s -u "cornerstone-client:cornerstone-secret" -X POST \
  "http://localhost:8080/auth/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=read" | jq -r '.access_token')

# 2. 无令牌访问受保护接口 → 401（Result 结构）
curl -s http://localhost:8080/system/user/1
# → {"code":401,"message":"未认证或令牌无效","data":null}

# 3. 带令牌访问 → 200（网关校验 JWT 并透传用户上下文）
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/system/user/1
# → {"code":200,"message":"操作成功","data":{"userId":1,"username":"admin",...}}

# 4. 权限点：client_credentials 令牌无业务权限 → 403
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","content":"x"}' http://localhost:8080/demo/announcement
# → {"code":403,"message":"无权限访问","data":null}

# 5. 公开接口：demo 的公告查询无需令牌（模块内白名单；演示可直连服务端口）
curl -s "http://localhost:8083/demo/announcement/page?pageNum=1&pageSize=5"

# ---- v2：用户登录（完整 RBAC 演示，admin/admin123）----
# 6. admin 登录换 JWT（令牌携带全部菜单权限，存于 scope 声明）
LOGIN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
USER_TOKEN=$(echo $LOGIN | jq -r '.data.access_token')

# 7. admin 令牌访问需权限的接口 → 200（@PreAuthorize 从 scope 读取权限）
curl -s -H "Authorization: Bearer $USER_TOKEN" \
  "http://localhost:8080/system/user/page?pageNum=1&pageSize=5"

# 8. 错误密码 → 401
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}'
```

### 实测记录（7 步）

| # | 场景 | 期望 | 实测 |
| --- | --- | --- | --- |
| 1 | 经网关 client_credentials 拿 token | 200 + access_token | ✅ |
| 2 | 无 token 访问受保护接口 | 401 | ✅ |
| 3 | 带 token 经网关访问 system 用户接口 | 200 | ✅ |
| 4 | 带 token 经网关访问 demo 公告分页 | 200 | ✅ |
| 5 | 带 token POST 创建公告（无权限点） | 403 | ✅ |
| 6 | 带 token 访问 system 用户分页（无权限点） | 403 | ✅ |
| 7 | 无 token 直连 demo 公开接口 | 200 | ✅ |

### v3 补充实测

| # | 场景 | 期望 | 实测 |
| --- | --- | --- | --- |
| 8 | actuator 健康检查（4 服务） | 200 | ✅ |
| 9 | 登录成功/失败写登录日志（sys_login_log） | 落库 | ✅ |
| 10 | 直连 system /system/auth/** 无内部令牌 | 401 | ✅ |
| 11 | 带内部令牌（auth Feign 自动附加） | 200 | ✅ |

## 常见问题

- **服务注册不上 Nacos**：确认 Nacos 已启动（8848 可达），检查各服务 `spring.cloud.nacos.discovery.server-addr`
- **401 一直失败**：确认 gateway/auth/system/demo 四处 RSA 密钥一致（auth 签发私钥 ↔ 其余三处公钥）
- **Flyway 报错**：确认数据库已创建（`cornerstone_system` / `cornerstone_demo`），首次启动自动执行迁移
- **登录 401/内部接口 401**：确认 auth/system 的 `cornerstone.internal-token` 一致（服务间调用校验）
- **经网关 503**：确认 `cornerstone-gateway` 依赖含 `spring-cloud-starter-loadbalancer`（lb:// 解析必需）
- **client_credentials 拿到 token 但访问 403**：这是预期行为——服务身份令牌（scope=read/write）没有业务权限点；用户级权限需 v2 授权码流程登录后获得
