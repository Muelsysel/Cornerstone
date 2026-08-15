-- Cornerstone 初始化脚本：创建各服务独立数据库（一服务一库）
-- 表结构由各服务的 Flyway 迁移创建，此处只建库。

CREATE DATABASE IF NOT EXISTS cornerstone_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS cornerstone_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
