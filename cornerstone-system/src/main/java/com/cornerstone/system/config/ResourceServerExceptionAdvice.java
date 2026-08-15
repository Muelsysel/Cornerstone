package com.cornerstone.system.config;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 资源服务器安全异常处理。 方法级 {@code @PreAuthorize} 拒绝抛出的 {@link AccessDeniedException} 映射为 HTTP 403， 认证异常映射为
 * HTTP 401——否则会被 common 的 {@code Exception} 兜底转成 200。 {@code @Order(HIGHEST_PRECEDENCE)} 优先于 common
 * 兜底。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResourceServerExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(ResourceServerExceptionAdvice.class);

    /** 无权限：HTTP 403 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("无权限访问: {}", e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN);
    }

    /** 未认证：HTTP 401 */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(AuthenticationException e) {
        log.warn("未认证访问: {}", e.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED);
    }

    /** 认证服务异常兜底：HTTP 403 */
    @ExceptionHandler(AuthorizationServiceException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAuthorizationService(AuthorizationServiceException e) {
        log.warn("授权服务异常: {}", e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN);
    }
}
