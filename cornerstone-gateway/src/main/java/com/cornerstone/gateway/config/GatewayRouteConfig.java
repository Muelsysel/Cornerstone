package com.cornerstone.gateway.config;

import com.cornerstone.api.ServiceConstants;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 网关路由：按前缀转发到 Nacos 服务。服务名统一引用 {@link ServiceConstants}。 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator cornerstoneRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // 认证中心：端点本身无 /auth 前缀（/oauth2/token），故剥离前缀转发
                .route(
                        "auth",
                        r ->
                                r.path("/auth/**")
                                        .filters(f -> f.rewritePath("/auth/(?<seg>.*)", "/${seg}"))
                                        .uri("lb://" + ServiceConstants.AUTH_SERVICE))
                // 系统服务：接口自带 /system 前缀，原样转发
                .route(
                        "system",
                        r -> r.path("/system/**").uri("lb://" + ServiceConstants.SYSTEM_SERVICE))
                // 演示服务：接口自带 /demo 前缀，原样转发
                .route("demo", r -> r.path("/demo/**").uri("lb://" + ServiceConstants.DEMO_SERVICE))
                .build();
    }
}
