package com.cornerstone.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类。统一入口：路由转发、JWT 校验、CORS。
 *
 * <p>common 的 servlet 自动配置（全局异常/用户上下文过滤器）由 {@code @ConditionalOnWebApplication(type = SERVLET)} 在
 * reactive 环境自动跳过，无需 exclude。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
