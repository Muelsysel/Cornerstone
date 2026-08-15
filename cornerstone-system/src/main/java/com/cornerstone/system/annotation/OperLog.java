package com.cornerstone.system.annotation;

import com.cornerstone.system.constant.BusinessType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 操作日志注解。标注在 Controller 方法上，由切面自动记录操作日志。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块标题 */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;
}
