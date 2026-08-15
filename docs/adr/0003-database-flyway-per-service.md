# ADR-0003: 数据库策略——一服务一库 + Flyway SQL 迁移

Status: accepted

每个服务拥有独立数据库（`cornerstone_system`、`cornerstone_demo`），表结构由 **Flyway** 版本化迁移管理，迁移以**纯 SQL 脚本**编写（V1 建表、V2 种子数据）。库由 docker-compose 初始化脚本创建。

Consequences: 结构可审阅、可回放，符合文档约束理念；服务间不共享表，数据集成走 API 契约。

Considered Options: MyBatis-Plus 自动建表（拒绝：结构不可审阅，违背"schema 即文档"）；手写 SQL 无版本管理（拒绝：无法回放与审阅——用户确认 Flyway + SQL 形式，两者兼得）。
