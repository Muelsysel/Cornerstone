package com.cornerstone.system.aspect;

import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysOperLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 操作日志切面：拦截 {@link OperLog} 注解，记录操作日志。 操作人取自 {@link UserContextHolder}（网关透传）。 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    private final SysOperLogService operLogService;
    private final ObjectMapper objectMapper;

    public OperLogAspect(SysOperLogService operLogService, ObjectMapper objectMapper) {
        this.operLogService = operLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        SysOperLog record = new SysOperLog();
        buildBefore(record, point, operLog);
        Object result;
        try {
            result = point.proceed();
            record.setStatus(0);
            record.setJsonResult(serialize(result));
            return result;
        } catch (Throwable t) {
            record.setStatus(1);
            record.setErrorMsg(truncate(t.getMessage()));
            throw t;
        } finally {
            String methodName =
                    point.getSignature().getDeclaringTypeName()
                            + "."
                            + point.getSignature().getName();
            record.setMethod(methodName);
            record.setOperTime(LocalDateTime.now());
            try {
                operLogService.record(record);
            } catch (Exception e) {
                // 日志记录失败不影响主流程
                log.warn("操作日志写入失败 method={}", methodName, e);
            }
        }
    }

    private void buildBefore(SysOperLog record, ProceedingJoinPoint point, OperLog operLog) {
        record.setTitle(operLog.title());
        record.setBusinessType(operLog.businessType().getCode());
        record.setRequestMethod(((MethodSignature) point.getSignature()).getMethod().getName());
        record.setOperName(currentOperName());
        record.setOperParam(serialize(point.getArgs()));
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            record.setOperUrl(request.getRequestURI());
            record.setRequestMethod(request.getMethod());
            record.setOperIp(clientIp(request));
        }
    }

    private String currentOperName() {
        UserContext context = UserContextHolder.get();
        return context != null ? context.getUsername() : "";
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
