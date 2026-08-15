package com.cornerstone.api;

/** 服务名常量。Feign 客户端与网关路由统一引用，禁止硬编码服务名。 */
public final class ServiceConstants {

    /** 网关服务 */
    public static final String GATEWAY_SERVICE = "cornerstone-gateway";

    /** 认证中心 */
    public static final String AUTH_SERVICE = "cornerstone-auth";

    /** 系统服务 */
    public static final String SYSTEM_SERVICE = "cornerstone-system";

    /** 演示服务 */
    public static final String DEMO_SERVICE = "cornerstone-demo";

    private ServiceConstants() {}
}
