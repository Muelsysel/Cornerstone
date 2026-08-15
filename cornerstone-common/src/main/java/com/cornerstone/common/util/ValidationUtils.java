package com.cornerstone.common.util;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.exception.BusinessException;

/**
 * 参数校验工具。字段长度上限与 DB 列定义（varchar(n)）保持一致： 超长会触发 MySQL DataTruncation → 500，业务层先校验返回友好 400。null
 * 视为合法（允许部分更新/可选字段），由 {@link #required} 单独约束必填。
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /** 字段长度上限校验：{@code value.length() > max} 时抛 400。 */
    public static void maxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "不能超过 " + max + " 个字符");
        }
    }

    /** 必填校验：空白值抛 400。 */
    public static void required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "不能为空");
        }
    }
}
