package com.cornerstone.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关访问日志：记录每次请求的方法、路径、响应状态、耗时与来源 IP。
 *
 * <p>order 取最大（最后执行），doFinally 在响应完成后记录，覆盖成功与异常路径。
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 健康检查/探针高频请求不入访问日志（避免噪音刷屏）；其余请求正常记录
        if (exchange.getRequest().getPath().value().startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        long start = System.currentTimeMillis();
        return chain.filter(exchange)
                .doFinally(
                        signal -> {
                            ServerHttpRequest request = exchange.getRequest();
                            ServerHttpResponse response = exchange.getResponse();
                            long cost = System.currentTimeMillis() - start;
                            String ip = clientIp(request);
                            // 操作人：令牌过滤器写入 exchange 属性（无令牌/白名单请求为 anonymous）
                            String user =
                                    String.valueOf(
                                            exchange.getAttributeOrDefault(
                                                    TokenAuthGlobalFilter.ATTR_USERNAME,
                                                    "anonymous"));
                            log.info(
                                    "[gateway] {} {} -> {} {}ms ip={} user={}",
                                    request.getMethod(),
                                    request.getPath(),
                                    response.getStatusCode(),
                                    cost,
                                    ip,
                                    user);
                        });
    }

    /** 客户端 IP：优先 X-Forwarded-For 第一个（经 nginx 反代时记录真实客户端），缺省回退远端地址 */
    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        // 最后执行：记录包括限流/鉴权在内的完整处理结果
        return Integer.MAX_VALUE;
    }
}
