package com.cornerstone.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 系统服务启动类。 职责：RBAC 标准集（用户/角色/菜单/部门）+ 字典/参数/操作日志/登录日志。 */
@SpringBootApplication
@MapperScan("com.cornerstone.system.domain.mapper")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
