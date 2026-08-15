package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.domain.mapper.SysLoginLogMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 登录日志服务单测：记录状态映射、分页条件、删除与清空。 */
class SysLoginLogServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单测无 Spring 上下文：LambdaQueryWrapper 需要显式注册实体元数据
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                assistant, SysLoginLog.class);
    }

    private final SysLoginLogMapper mapper = mock(SysLoginLogMapper.class);
    private SysLoginLogServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        SysLoginLogServiceImpl impl = new SysLoginLogServiceImpl();
        java.lang.reflect.Field field =
                com.baomidou.mybatisplus.spring.repository.CrudRepository.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, mapper);
        service = Mockito.spy(impl);
    }

    @Test
    void recordSuccessMapsStatusZero() {
        doReturn(true).when(service).save(any(SysLoginLog.class));

        service.record("admin", "127.0.0.1", true, "登录成功");

        verify(service)
                .save(
                        argThat(
                                log ->
                                        "0".equals(log.getStatus())
                                                && "admin".equals(log.getUsername())
                                                && "127.0.0.1".equals(log.getIpaddr())
                                                && "登录成功".equals(log.getMsg())
                                                && log.getLoginTime() != null));
    }

    @Test
    void recordFailureMapsStatusOne() {
        doReturn(true).when(service).save(any(SysLoginLog.class));

        service.record("test", "10.0.0.1", false, "密码错误");

        verify(service)
                .save(argThat(log -> "1".equals(log.getStatus()) && "密码错误".equals(log.getMsg())));
    }

    @Test
    void recordTruncatesOversizedUsername() {
        // 回归：登录接口允许 64 字符用户名，日志列 varchar(50)——超长会 DataTruncation → 审计记录丢失
        doReturn(true).when(service).save(any(SysLoginLog.class));
        String longName = "u".repeat(64);

        service.record(longName, "127.0.0.1", false, "用户名或密码错误");

        verify(service).save(argThat(log -> log.getUsername().length() == 50));
    }

    @Test
    void pagePassesConditions() {
        doReturn(new Page<SysLoginLog>()).when(service).page(any(), any());

        Page<SysLoginLog> result =
                service.page(2, 10, "adm", "1", "2026-08-01 00:00:00", "2026-08-16 23:59:59");

        assertThat(result).isNotNull();
        verify(service)
                .page(argThat(page -> page.getCurrent() == 2 && page.getSize() == 10), any());
    }

    @Test
    void pageWithDateRangeAddsTimeConditions() {
        // 回归：日志时间区间过滤（beginTime/endTime → oper_time ge/le）
        doReturn(new Page<SysLoginLog>()).when(service).page(any(), any());
        service.page(1, 10, null, null, "2026-08-01 00:00:00", "2026-08-16 23:59:59");
        @SuppressWarnings({"unchecked", "rawtypes"})
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<SysLoginLog>> captor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(service).page(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("login_time").contains(">=").contains("<=");
    }

    @Test
    void deleteRemovesById() {
        doReturn(true).when(service).removeById(7L);

        service.delete(7L);

        verify(service).removeById(7L);
    }

    @Test
    void cleanRemovesAll() {
        doReturn(true).when(service).remove(null);

        service.clean();

        verify(service).remove(null);
    }
}
