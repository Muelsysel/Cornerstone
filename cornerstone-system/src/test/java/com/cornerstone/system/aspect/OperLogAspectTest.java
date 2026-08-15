package com.cornerstone.system.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cornerstone.system.annotation.OperLog;
import com.cornerstone.system.constant.BusinessType;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.service.SysOperLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

/** 操作日志切面脱敏回归测试：修改密码等操作不得把明文口令写入操作日志（安全基线）。 */
class OperLogAspectTest {

    /** 模拟修改密码请求体（含敏感字段）。 */
    private record Credentials(String username, String oldPassword, String newPassword) {}

    @Test
    void masksPasswordFieldsInOperParam() throws Throwable {
        SysOperLogService service = mock(SysOperLogService.class);
        AtomicReference<SysOperLog> captured = new AtomicReference<>();
        doAnswer(
                        inv -> {
                            captured.set(inv.getArgument(0));
                            return null;
                        })
                .when(service)
                .record(any(SysOperLog.class));

        OperLogAspect aspect = new OperLogAspect(service, new ObjectMapper());

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method =
                OperLogAspect.class.getMethod("around", ProceedingJoinPoint.class, OperLog.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(OperLogAspect.class.getName());
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs())
                .thenReturn(new Object[] {new Credentials("admin", "old-pass", "new-pass")});
        when(point.proceed()).thenReturn(null);

        OperLog operLog = mock(OperLog.class);
        when(operLog.title()).thenReturn("修改密码");
        when(operLog.businessType()).thenReturn(BusinessType.UPDATE);

        aspect.around(point, operLog);

        String operParam = captured.get().getOperParam();
        // 明文口令不得出现，且敏感字段被屏蔽为 ***；非敏感字段（username）保留
        assertThat(operParam).doesNotContain("old-pass", "new-pass").contains("***");
        assertThat(operParam).contains("admin");
    }
}
