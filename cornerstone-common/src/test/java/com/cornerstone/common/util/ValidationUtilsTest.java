package com.cornerstone.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cornerstone.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

/** 校验工具单测：null/边界值放行，超长/空白拒绝且消息友好。 */
class ValidationUtilsTest {

    @Test
    void maxLengthAllowsNullAndBoundary() {
        assertThatCode(() -> ValidationUtils.maxLength(null, 100, "标题")).doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.maxLength("", 100, "标题")).doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.maxLength("x".repeat(100), 100, "标题"))
                .doesNotThrowAnyException();
    }

    @Test
    void maxLengthRejectsOversizedWithFriendlyMessage() {
        assertThatThrownBy(() -> ValidationUtils.maxLength("x".repeat(101), 100, "标题"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("标题不能超过 100 个字符");
    }

    @Test
    void requiredRejectsBlank() {
        assertThatThrownBy(() -> ValidationUtils.required(" ", "名称"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("名称不能为空");
    }

    @Test
    void requiredAllowsText() {
        assertThatCode(() -> ValidationUtils.required("ok", "名称")).doesNotThrowAnyException();
        BusinessException e =
                org.junit.jupiter.api.Assertions.assertThrows(
                        BusinessException.class, () -> ValidationUtils.required(null, "名称"));
        assertThat(e.getCode()).isEqualTo(400);
    }

    @Test
    void oneOfAllowsNullAndAllowedValues() {
        assertThatCode(() -> ValidationUtils.oneOf(null, "状态值", "0", "1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.oneOf("0", "状态值", "0", "1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidationUtils.oneOf("3", "数据范围", "1", "2", "3", "4", "5"))
                .doesNotThrowAnyException();
    }

    @Test
    void oneOfRejectsDisallowedWithFriendlyMessage() {
        assertThatThrownBy(() -> ValidationUtils.oneOf("9", "数据范围", "1", "2", "3", "4", "5"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("数据范围非法（仅 1/2/3/4/5）");
    }
}
