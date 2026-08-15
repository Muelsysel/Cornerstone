package com.cornerstone.common.core;

/** 内置错误码。业务错误码从 1000 起，由各模块自定义枚举实现 {@link IErrorCode}。 */
public enum ErrorCode implements IErrorCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 请求参数错误（含校验失败） */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未认证或令牌无效 */
    UNAUTHORIZED(401, "未认证或令牌无效"),
    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    /** 不支持的媒体类型 */
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    /** 系统内部错误 */
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
