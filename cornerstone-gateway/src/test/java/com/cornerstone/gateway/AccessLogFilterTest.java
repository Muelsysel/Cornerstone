package com.cornerstone.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cornerstone.gateway.filter.AccessLogFilter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/** 网关访问日志过滤器单元测试： 探针静默、常规请求记录方法/路径/状态、执行顺序最后。 */
class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> events() {
        return appender.list;
    }

    private static GatewayFilterChain passThroughChain() {
        return ex -> Mono.empty();
    }

    @Test
    void actuatorProbeIsSilent() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, passThroughChain()).block();

        assertThat(events()).isEmpty();
    }

    @Test
    void normalRequestLogsMethodPathAndStatus() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.method(HttpMethod.GET, "/system/user/list").build());
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        filter.filter(exchange, passThroughChain()).block();

        assertThat(events()).hasSize(1);
        ILoggingEvent event = events().get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("[gateway]")
                .contains("GET")
                .contains("/system/user/list")
                .contains("200 OK")
                .contains("ms");
    }

    @Test
    void filterRunsLast() {
        assertThat(filter.getOrder()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void logsClientIpFromXForwardedFor() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest.method(HttpMethod.GET, "/system/user/list")
                                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                                .build());
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        filter.filter(exchange, passThroughChain()).block();

        ILoggingEvent event = events().get(0);
        // 取 X-Forwarded-For 第一个 IP（真实客户端），而非反代/网关地址
        assertThat(event.getFormattedMessage()).contains("ip=203.0.113.7");
    }
}
