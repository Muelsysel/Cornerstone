package com.cornerstone.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理器契约测试：统一错误响应的 code/message 映射（所有服务共用）。 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionPropagatesCodeAndMessage() {
        Result<Void> result =
                handler.handleBusinessException(new BusinessException(1001, "用户名已存在"));

        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("用户名已存在");
    }

    @Test
    void fieldValidationErrorReturnsFirstMessageWithBadRequest() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(binding.getFieldErrors())
                .thenReturn(List.of(new FieldError("obj", "username", "用户名不能为空")));
        when(e.getBindingResult()).thenReturn(binding);

        Result<Void> result = handler.handleMethodArgumentNotValid(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo("用户名不能为空");
    }

    @Test
    void constraintViolationReturnsFirstMessage() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数不合法");
        ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

        Result<Void> result = handler.handleConstraintViolation(e);

        assertThat(result.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo("参数不合法");
    }

    @Test
    void genericExceptionMasksInternalDetails() {
        Result<Void> result =
                handler.handleException(new IllegalStateException("db connection refused"));

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        // 兜底不泄露内部细节
        assertThat(result.getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
        assertThat(result.getMessage()).doesNotContain("db connection");
    }

    @Test
    void notFoundReturns404Code() {
        Result<Void> result =
                handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "/nope"));

        assertThat(result.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        Result<Void> result =
                handler.handleMessageNotReadable(
                        new org.springframework.http.converter.HttpMessageNotReadableException(
                                "JSON parse error: unexpected character"));

        assertThat(result.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    @Test
    void methodNotSupportedReturns405() {
        Result<Void> result =
                handler.handleMethodNotSupported(
                        new org.springframework.web.HttpRequestMethodNotSupportedException(
                                "POST", List.of("GET")));

        assertThat(result.getCode()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    void mediaTypeNotSupportedReturns415() {
        Result<Void> result =
                handler.handleMediaTypeNotSupported(
                        new org.springframework.web.HttpMediaTypeNotSupportedException(
                                "text/plain"));

        assertThat(result.getCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
    }
}
