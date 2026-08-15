package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.domain.mapper.SysOperLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 操作日志服务单测：记录、分页条件、删除与清空。 */
class SysOperLogServiceImplTest {

    private final SysOperLogMapper mapper = mock(SysOperLogMapper.class);
    private SysOperLogServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        SysOperLogServiceImpl impl = new SysOperLogServiceImpl();
        java.lang.reflect.Field field =
                com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, mapper);
        service = Mockito.spy(impl);
    }

    @Test
    void recordSavesLog() {
        SysOperLog log = new SysOperLog();
        log.setTitle("删除用户");
        doReturn(true).when(service).save(log);

        service.record(log);

        verify(service).save(log);
    }

    @Test
    void pagePassesConditions() {
        doReturn(new Page<SysOperLog>()).when(service).page(any(), any());

        Page<SysOperLog> result = service.page(1, 20, "用户", "admin", "0");

        assertThat(result).isNotNull();
        verify(service)
                .page(argThat(page -> page.getCurrent() == 1 && page.getSize() == 20), any());
    }

    @Test
    void pageWithoutFiltersStillSorts() {
        doReturn(new Page<SysOperLog>()).when(service).page(any(), any());

        service.page(1, 20, null, null, null);

        verify(service).page(argThat(page -> page.getCurrent() == 1), any());
    }

    @Test
    void deleteRemovesById() {
        doReturn(true).when(service).removeById(9L);

        service.delete(9L);

        verify(service).removeById(9L);
    }

    @Test
    void cleanRemovesAll() {
        doReturn(true).when(service).remove(null);

        service.clean();

        verify(service).remove(null);
    }
}
