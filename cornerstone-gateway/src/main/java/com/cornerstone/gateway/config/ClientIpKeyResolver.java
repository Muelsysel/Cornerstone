package com.cornerstone.gateway.config;

import java.util.List;
import java.util.Set;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 限流键解析器：默认按直连客户端 IP 限流；当直连对端属于受信代理（如 nginx 容器）时，取 X-Forwarded-For 第一个值还原真实客户端 IP。
 *
 * <p>背景：生产部署经 nginx 反代（前端容器 8088 → 网关），网关看到的 remoteAddress 恒为 nginx 容器 IP——若直接用 remoteAddress
 * 限流，所有用户共享一个桶（登录爆破防护失效）。但直接信任 XFF 可被伪造（绕过限流）， 故仅在直连对端位于受信列表时采信 XFF。
 *
 * <p>配置：{@code cornerstone.gateway.trusted-proxy-ips}（逗号分隔，默认空 = 不信任任何代理，行为与旧版一致）。
 */
public final class ClientIpKeyResolver implements KeyResolver {

    private final Set<String> trustedProxies;

    public ClientIpKeyResolver(Set<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String remote =
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
        List<String> xff = exchange.getRequest().getHeaders().get("X-Forwarded-For");
        return Mono.just(resolveClientIp(remote, xff, trustedProxies));
    }

    /** 纯函数：受信代理场景取 XFF 首值，否则直连 IP。便于单测。 */
    static String resolveClientIp(
            String remoteAddress, List<String> xForwardedFor, Set<String> trustedProxies) {
        if (trustedProxies.contains(remoteAddress)
                && xForwardedFor != null
                && !xForwardedFor.isEmpty()) {
            String first = xForwardedFor.get(0).split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return remoteAddress;
    }
}
