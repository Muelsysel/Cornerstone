package com.cornerstone.common.config;

import com.cornerstone.common.exception.GlobalExceptionHandler;
import com.cornerstone.common.security.UserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Import;

/**
 * common 模块自动配置：全局异常处理 + 用户上下文过滤器。
 *
 * <p>仅对 Servlet Web 应用生效（Type.SERVLET）：
 *
 * <ul>
 *   <li>全局异常处理基于 Spring MVC（@RestControllerAdvice），Reactive（如网关）环境不适用；
 *   <li>UserContextFilter 是 Servlet Filter，Reactive 环境不注册。
 * </ul>
 *
 * 通过 AutoConfiguration.imports 注册，Servlet 服务依赖 common 即自动生效，Reactive 服务自动跳过、无需 exclude。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({GlobalExceptionHandler.class, UserContextFilter.class})
public class CornerstoneCommonAutoConfiguration {}
