package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.domain.mapper.SysLoginLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 登录日志服务单测：记录状态映射、分页条件、删除与清空。 */
class SysLoginLogServiceImplTest {

    private final SysLoginLogMapper mapper = mock(SysLoginLogMapper.class);
    private SysLoginLogServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        SysLoginLogServiceImpl impl = new SysLoginLogServiceImpl();
        java.lang.reflect.Field field =
                com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, mapper);
        service = Mockito.spy(impl);
    }

    @Test
    void recordSuccessMapsStatusZero() {
        doReturn(true).when(service).save(any());

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
        doReturn(true).when(service).save(any());

        service.record("test", "10.0.0.1", false, "密码错误");

        verify(service)
                .save(argThat(log -> "1".equals(log.getStatus()) && "密码错误".equals(log.getMsg())));
    }

    @Test
    void pagePassesConditions() {
        doReturn(new Page<SysLoginLog>()).when(service).page(any(), any());

        Page<SysLoginLog> result = service.page(2, 10, "adm", "1");

        assertThat(result).isNotNull();
        verify(service)
                .page(argThat(page -> page.getCurrent() == 2 && page.getSize() == 10), any());
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
