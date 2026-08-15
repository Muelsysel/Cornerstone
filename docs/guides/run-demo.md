# 运行指南：认证链路演示

> 状态：v1 初稿——服务实现完成后需按实际端点校验（对应票据 T10）。

## 前提

- Docker Desktop（推荐）或手动安装 Nacos 2.3 / MySQL 8 / Redis 7
- JDK 17 + Maven 3.9

## 第一步：启动依赖

```bash
docker compose up -d
# 验证：Nacos 控制台 http://localhost:8848/nacos（默认 nacos/nacos）
#       MySQL localhost:3306 root/cornerstone（库 cornerstone_system、cornerstone_demo 已建）
#       Redis localhost:6379
```

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

各服务注册到 Nacos 后即可通过网关访问。

## 第三步：演示认证链路

```bash
# 1. 拿令牌（client_credentials，经网关转发到认证中心）
TOKEN=$(curl -s -X POST http://localhost:8080/auth/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=cornerstone-client&client_secret=cornerstone-secret&scope=read" \
  | jq -r '.access_token')

# 2. 无令牌访问受保护资源 → 401
curl -s http://localhost:8080/system/user/1

# 3. 带令牌访问受保护资源 → 200 + Result 结构
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/system/user/1

# 4. 公开接口（公告分页）无需令牌
curl -s http://localhost:8080/demo/announcement/page?pageNum=1&pageSize=10
```

## 常见问题

- **服务注册不上 Nacos**：确认 Nacos 已启动（8848 可达），检查各服务 `spring.cloud.nacos.discovery.server-addr`
- **401 一直失败**：确认 gateway/auth/system/demo 四处的 RSA 密钥对一致（见各服务 application.yml）
- **Flyway 报错**：确认数据库已创建（`cornerstone_system` / `cornerstone_demo`），首次启动会自动执行迁移
