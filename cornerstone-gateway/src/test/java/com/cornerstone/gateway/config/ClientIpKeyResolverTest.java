package com.cornerstone.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 限流键解析单测：默认直连 IP；受信代理场景取 XFF 首值（防伪造与防多用户共桶）。 */
class ClientIpKeyResolverTest {

    @Test
    void untrustedProxyKeepsRemoteAddress() {
        // 直连对端不在受信列表：即使带 XFF 也不采信（防伪造绕过限流）
        String key = ClientIpKeyResolver.resolveClientIp("1.2.3.4", List.of("9.9.9.9"), Set.of());
        assertThat(key).isEqualTo("1.2.3.4");
    }

    @Test
    void trustedProxyUsesFirstXffValue() {
        // 受信代理场景：nginx 追加的真实客户端 IP 在 XFF 首值
        String key =
                ClientIpKeyResolver.resolveClientIp(
                        "172.17.0.2", List.of("203.0.113.7, 172.17.0.1"), Set.of("172.17.0.2"));
        assertThat(key).isEqualTo("203.0.113.7");
    }

    @Test
    void trustedProxyWithoutXffFallsBackToRemote() {
        // 受信代理但无 XFF（直连内部调用）：回退直连 IP，不产生空键
        String key = ClientIpKeyResolver.resolveClientIp("172.17.0.2", null, Set.of("172.17.0.2"));
        assertThat(key).isEqualTo("172.17.0.2");
    }

    @Test
    void trustedProxyWithBlankXffFallsBackToRemote() {
        // XFF 首值为空串（畸形头）：回退直连 IP
        String key =
                ClientIpKeyResolver.resolveClientIp(
                        "172.17.0.2", List.of("  "), Set.of("172.17.0.2"));
        assertThat(key).isEqualTo("172.17.0.2");
    }
}
