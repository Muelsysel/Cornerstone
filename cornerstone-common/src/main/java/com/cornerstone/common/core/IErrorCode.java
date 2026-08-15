package com.cornerstone.common.core;

/** 错误码契约。业务模块可自定义枚举实现本接口，扩展错误码（从 1000 起，避免与内置码冲突）。 */
public interface IErrorCode {

    /** 错误码 */
    int getCode();

    /** 错误信息 */
    String getMessage();
}
