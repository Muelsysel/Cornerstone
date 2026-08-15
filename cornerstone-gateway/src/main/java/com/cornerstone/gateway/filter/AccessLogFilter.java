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
                            String ip =
                                    request.getRemoteAddress() != null
                                            ? request.getRemoteAddress()
                                                    .getAddress()
                                                    .getHostAddress()
                                            : "unknown";
                            log.info(
                                    "[gateway] {} {} -> {} {}ms ip={}",
                                    request.getMethod(),
                                    request.getPath(),
                                    response.getStatusCode(),
                                    cost,
                                    ip);
                        });
    }

    @Override
    public int getOrder() {
        // 最后执行：记录包括限流/鉴权在内的完整处理结果
        return Integer.MAX_VALUE;
    }
}
