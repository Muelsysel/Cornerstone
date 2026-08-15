package com.cornerstone.gateway.config;

import com.cornerstone.api.ServiceConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 网关路由：按前缀转发到 Nacos 服务，全部启用基于 Redis 的 IP 限流。服务名统一引用 {@link ServiceConstants}。 */
@Configuration
public class GatewayRouteConfig {

    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver ipKeyResolver;

    public GatewayRouteConfig(RedisRateLimiter redisRateLimiter, KeyResolver ipKeyResolver) {
        this.redisRateLimiter = redisRateLimiter;
        this.ipKeyResolver = ipKeyResolver;
    }

    @Bean
    public RouteLocator cornerstoneRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // 认证中心：端点本身无 /auth 前缀（/oauth2/token），故剥离前缀转发
                .route(
                        "auth",
                        r ->
                                r.path("/auth/**")
                                        .filters(
                                                f ->
                                                        f.rewritePath("/auth/(?<seg>.*)", "/${seg}")
                                                                .requestRateLimiter(
                                                                        c ->
                                                                                c.setRateLimiter(
                                                                                                redisRateLimiter)
                                                                                        .setKeyResolver(
                                                                                                ipKeyResolver)))
                                        .uri("lb://" + ServiceConstants.AUTH_SERVICE))
                // 系统服务：接口自带 /system 前缀，原样转发
                .route(
                        "system",
                        r ->
                                r.path("/system/**")
                                        .filters(
                                                f ->
                                                        f.requestRateLimiter(
                                                                c ->
                                                                        c.setRateLimiter(
                                                                                        redisRateLimiter)
                                                                                .setKeyResolver(
                                                                                        ipKeyResolver)))
                                        .uri("lb://" + ServiceConstants.SYSTEM_SERVICE))
                // 演示服务：接口自带 /demo 前缀，原样转发
                .route(
                        "demo",
                        r ->
                                r.path("/demo/**")
                                        .filters(
                                                f ->
                                                        f.requestRateLimiter(
                                                                c ->
                                                                        c.setRateLimiter(
                                                                                        redisRateLimiter)
                                                                                .setKeyResolver(
                                                                                        ipKeyResolver)))
                                        .uri("lb://" + ServiceConstants.DEMO_SERVICE))
                .build();
    }
}
