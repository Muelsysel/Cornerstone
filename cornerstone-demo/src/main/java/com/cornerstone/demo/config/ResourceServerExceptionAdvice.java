package com.cornerstone.demo.config;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 资源服务器安全异常处理：方法级 {@code @PreAuthorize} 拒绝时抛出的 {@link AccessDeniedException} 需映射为 HTTP 403（而不是被
 * common 兜底为 200）。 用 {@code @Order(HIGHEST_PRECEDENCE)} 优先于 common 的 {@code Exception} 兜底。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResourceServerExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(ResourceServerExceptionAdvice.class);

    /** 无权限：映射为 HTTP 403 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("无权限访问: {}", e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN);
    }
}
