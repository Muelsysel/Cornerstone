package com.cornerstone.system.aspect;

import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysOperLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            record.setJsonResult(truncate(serializeMasked(result), MAX_PARAM_CHARS));
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
        record.setOperName(truncate(currentOperName(), 50));
        record.setOperParam(truncate(serializeMasked(point.getArgs()), MAX_PARAM_CHARS));
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            record.setOperUrl(truncate(request.getRequestURI(), 255));
            record.setRequestMethod(request.getMethod());
            record.setOperIp(truncate(clientIp(request), 128));
        }
    }

    /** 参数字符上限：MySQL TEXT 列字节上限 64KB，UTF-8 中文 3 字节/字——20000 字符即使全中文（60000 字节）也不超限 */
    private static final int MAX_PARAM_CHARS = 20000;

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

    /** 脱敏序列化：递归屏蔽敏感字段（password/secret/token 等）后再输出 JSON。 防止修改密码/用户管理/重置密码等操作把明文口令写入操作日志。 */
    private String serializeMasked(Object value) {
        try {
            return objectMapper.writeValueAsString(maskSensitive(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Object maskSensitive(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new HashMap<>();
            map.forEach(
                    (k, v) -> {
                        String key = String.valueOf(k);
                        masked.put(key, isSensitiveKey(key) ? "***" : maskSensitive(v));
                    });
            return masked;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::maskSensitive).toList();
        }
        if (value.getClass().isArray()) {
            List<?> list =
                    java.util.stream.IntStream.range(0, java.lang.reflect.Array.getLength(value))
                            .mapToObj(i -> java.lang.reflect.Array.get(value, i))
                            .toList();
            return maskSensitive(list);
        }
        // 普通 Bean：先转 Map 再递归脱敏；转换失败则原样返回
        try {
            return maskSensitive(
                    objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            return value;
        }
    }

    private static boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("credential");
    }

    private String truncate(String message) {
        return truncate(message, 500);
    }

    private String truncate(String message, int max) {
        if (message == null || message.length() <= max) {
            return message;
        }
        return message.substring(0, max);
    }
}
