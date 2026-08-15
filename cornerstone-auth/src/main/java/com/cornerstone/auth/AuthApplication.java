package com.cornerstone.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 认证中心启动类。OAuth2 授权服务器：client_credentials 签发 RS256 JWT，提供 JWKS 端点；并提供用户名密码登录换 JWT。 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cornerstone.api")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
