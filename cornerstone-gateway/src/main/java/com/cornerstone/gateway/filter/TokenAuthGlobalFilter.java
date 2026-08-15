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

    /**
     * 白名单前缀：认证、演示服务、Actuator、OpenAPI 文档路径免令牌。
     *
     * <p>/demo/ 整体放行——演示服务内含公开接口，其"公开还是受保护"由服务层资源服务器自行裁决 （双保险设计）；/system/ 等其余服务路径由网关统一校验。
     */
    static final List<String> WHITELIST =
            List.of("/auth/", "/demo/", "/actuator/", "/swagger-ui/", "/v3/api-docs/", "/webjars/");

    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public TokenAuthGlobalFilter(ReactiveJwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 统一剥除外部传入的透传头（防身份伪造）：透传头只允许由本过滤器按 JWT 重建，
        // 否则攻击者可携带 X-Cornerstone-User-Id/Roles 等伪造身份（白名单路径同样清洗）。
        ServerHttpRequest request = stripPassthroughHeaders(exchange.getRequest());
        ServerWebExchange stripped = exchange.mutate().request(request).build();
        if (isWhitelisted(request.getPath().value())) {
            // 白名单路径保持公开：无/无效 token 原样放行（下游服务自行裁决）；
            // 但若携带有效 token，则同样重建透传上下文头——否则白名单内的受保护操作
            // （如 demo 公告管理的 @PreAuthorize 接口）拿不到用户身份，审计/作者为空。
            String token = extractBearerToken(request);
            if (token == null) {
                return chain.filter(stripped);
            }
            return jwtDecoder
                    .decode(token)
                    .flatMap(jwt -> chain.filter(forwardWithHeaders(stripped, jwt)))
                    .onErrorResume(e -> chain.filter(stripped));
        }

        String token = extractBearerToken(request);
        if (token == null) {
            return unauthorized(stripped);
        }

        return jwtDecoder
                .decode(token)
                .flatMap(jwt -> chain.filter(forwardWithHeaders(stripped, jwt)))
                .onErrorResume(e -> unauthorized(stripped));
    }

    /** 剥除客户端可控的透传上下文头（X-Cornerstone-*），防止伪造身份透传到下游服务 */
    private ServerHttpRequest stripPassthroughHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(
                        h -> {
                            h.remove(UserContext.HEADER_USER_ID);
                            h.remove(UserContext.HEADER_USERNAME);
                            h.remove(UserContext.HEADER_DEPT_ID);
                            h.remove(UserContext.HEADER_ROLES);
                        })
                .build();
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
                                        // 用户令牌：sub 为数字用户 ID；client_credentials 令牌：sub 为
                                        // client_id（透传为用户名）
                                        if (sub.matches("\\d+")) {
                                            h.add(UserContext.HEADER_USER_ID, sub);
                                        } else {
                                            h.add(UserContext.HEADER_USERNAME, sub);
                                        }
                                    }
                                    String username =
                                            firstClaim(jwt, "username", "preferred_username");
                                    if (username != null) {
                                        h.add(UserContext.HEADER_USERNAME, username);
                                    }
                                    // 角色透传：优先 JWT 的 roles 声明（真实角色，供数据权限/审计按角色解析），
                                    // client_credentials 令牌无 roles 时兜底用 scope（兼容服务身份）
                                    Object roles = jwt.getClaim("roles");
                                    if (roles != null) {
                                        h.add(UserContext.HEADER_ROLES, flatten(roles));
                                    } else {
                                        Object scope = jwt.getClaim("scope");
                                        if (scope != null) {
                                            h.add(UserContext.HEADER_ROLES, flatten(scope));
                                        }
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
