package com.cornerstone.demo.domain;

import com.cornerstone.common.core.IErrorCode;

/** 演示模块业务错误码。约定：模块错误码从 1000 起，避免与内置 {@link com.cornerstone.common.core.ErrorCode} 冲突。 */
public enum DemoErrorCode implements IErrorCode {

    /** 公告数据不存在 */
    ANNOUNCEMENT_NOT_FOUND(1000, "公告不存在"),
    /** 公告状态流转非法 */
    ANNOUNCEMENT_STATUS_ILLEGAL(1001, "公告状态流转非法"),
    /** 公告标题不能为空 */
    ANNOUNCEMENT_TITLE_REQUIRED(1002, "公告标题不能为空");

    private final int code;
    private final String message;

    DemoErrorCode(int code, String message) {
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
