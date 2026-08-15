package com.cornerstone.gateway.filter;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.security.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 全局令牌校验过滤器： 白名单路径放行；其余路径必须携带有效 JWT，校验通过后把声明映射为透传上下文头再转发； 校验失败返回 401 + {@link Result} JSON。 */
@Component
public class TokenAuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单前缀：认证、Actuator、OpenAPI 文档路径免令牌 */
    static final List<String> WHITELIST =
            List.of("/auth/", "/actuator/", "/swagger-ui/", "/v3/api-docs/", "/webjars/");

    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public TokenAuthGlobalFilter(ReactiveJwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isWhitelisted(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String token = extractBearerToken(request);
        if (token == null) {
            return unauthorized(exchange);
        }

        return jwtDecoder
                .decode(token)
                .flatMap(jwt -> chain.filter(forwardWithHeaders(exchange, jwt)))
                .onErrorResume(e -> unauthorized(exchange));
    }

    /** 白名单判断：路径前缀命中即放行 */
    private boolean isWhitelisted(String path) {
        for (String prefix : WHITELIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // 精确等于 auth 根路径也放行
        return path.matches("/auth(/)?");
    }

    /** 从 Authorization: Bearer xxx 提取令牌 */
    private String extractBearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }

    /** 把 JWT 声明映射为透传上下文头，构造新请求继续转发 */
    private ServerWebExchange forwardWithHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest mutated =
                exchange.getRequest()
                        .mutate()
                        .headers(
                                h -> {
                                    String sub = jwt.getSubject();
                                    if (sub != null) {
                                        h.add(UserContext.HEADER_USER_ID, sub);
                                    }
                                    String username =
                                            firstClaim(jwt, "username", "preferred_username");
                                    if (username != null) {
                                        h.add(UserContext.HEADER_USERNAME, username);
                                    }
                                    Object scope = jwt.getClaim("scope");
                                    if (scope != null) {
                                        h.add(UserContext.HEADER_ROLES, flatten(scope));
                                    }
                                })
                        .build();
        return exchange.mutate().request(mutated).build();
    }

    /** 取多个声明中第一个非空值 */
    private String firstClaim(Jwt jwt, String... names) {
        for (String name : names) {
            Object v = jwt.getClaim(name);
            if (v != null) {
                return v.toString();
            }
        }
        return null;
    }

    /** scope 声明（可能为逗号串或集合），统一逗号连接 */
    private String flatten(Object scope) {
        if (scope instanceof Iterable<?> it) {
            return StreamSupport.stream(it.spliterator(), false)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        return scope.toString();
    }

    /** 返回 401 + Result JSON */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Result.fail(ErrorCode.UNAUTHORIZED));
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":401}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在 CORS 之后、路由转发之前的令牌校验层
        return -100;
    }
}
