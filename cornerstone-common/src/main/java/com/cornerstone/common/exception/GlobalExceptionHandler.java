package com.cornerstone.common.exception;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理器：所有服务的统一错误响应契约。 由 common 自动配置注册，服务无需额外引入。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按业务错误码返回 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@RequestBody 对象校验） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message =
                e.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(FieldError::getDefaultMessage)
                        .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /** 参数校验失败（方法级 @Validated 参数校验） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message =
                e.getConstraintViolations().stream()
                        .findFirst()
                        .map(v -> v.getMessage())
                        .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /** 请求体 JSON 解析失败（格式错误）：客户端问题，返回 400 而非 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /** 缺少必填请求参数（如未传 @RequestParam(required=true)）：客户端问题，返回 400 而非 500 */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException e) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少必填参数: " + e.getParameterName());
    }

    /** 参数类型不匹配（如 pageNum=abc 传给 long）：客户端问题，返回 400 而非 500 */
    @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(org.springframework.beans.TypeMismatchException e) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "参数格式错误: " + e.getPropertyName());
    }

    /** HTTP 方法不支持（如对只读端点用 POST）：返回 405 而非 500 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /** Content-Type 不支持：返回 415 而非 500 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return Result.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /** 资源不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    /** 兜底异常：不泄露内部细节，只记日志（含请求路径便于定位） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(
            Exception e, jakarta.servlet.http.HttpServletRequest request) {
        log.error("系统异常 uri={} {}", request.getRequestURI(), request.getMethod(), e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
