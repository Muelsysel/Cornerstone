package com.cornerstone.common.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;

/**
 * 统一返回结构。所有服务接口的响应体契约，禁止另起炉灶。
 *
 * @param <T> 数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码，见 {@link ErrorCode} */
    private int code;

    /** 提示信息 */
    private String message;

    /** 数据 */
    private T data;

    public Result() {}

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.INTERNAL_ERROR.getCode(), message, null);
    }

    /** 便捷判断，不参与 JSON 序列化（避免污染响应契约） */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
