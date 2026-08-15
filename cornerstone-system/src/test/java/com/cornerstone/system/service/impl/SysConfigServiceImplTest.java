package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.constant.CacheConstants;
import com.cornerstone.system.domain.entity.SysConfig;
import com.cornerstone.system.domain.mapper.SysConfigMapper;
import com.cornerstone.system.util.JsonCache;
import org.junit.jupiter.api.Test;

/** 参数服务单测：缓存读写/命中回源、更新清旧 key、null 值不写缓存（缓存一致性）。 */
class SysConfigServiceImplTest {

    private final SysConfigMapper configMapper = mock(SysConfigMapper.class);
    private final JsonCache jsonCache = mock(JsonCache.class);
    private final SysConfigServiceImpl service = new SysConfigServiceImpl(configMapper, jsonCache);

    private SysConfig config(Long id, String key, String value) {
        SysConfig c = new SysConfig();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    private String key(String k) {
        return String.format(CacheConstants.CONFIG_KEY, k);
    }

    @Test
    void getValueByKeyHitsCache() {
        when(jsonCache.getString(key("a"))).thenReturn("cached");
        assertThat(service.getValueByKey("a")).isEqualTo("cached");
        verify(configMapper, never()).selectOne(any());
    }

    @Test
    void getValueByKeyMissesAndBackfills() {
        when(jsonCache.getString(key("a"))).thenReturn(null);
        when(configMapper.selectOne(any())).thenReturn(config(1L, "a", "db"));

        assertThat(service.getValueByKey("a")).isEqualTo("db");
        verify(jsonCache).setString(key("a"), "db");
    }

    @Test
    void getValueByKeyMissWithNullDoesNotCache() {
        when(jsonCache.getString(key("a"))).thenReturn(null);
        when(configMapper.selectOne(any())).thenReturn(null);

        assertThat(service.getValueByKey("a")).isNull();
        verify(jsonCache, never()).setString(anyString(), anyString());
    }

    @Test
    void updateEvictsOldKeyAndWritesNewValue() {
        when(configMapper.selectById(1L)).thenReturn(config(1L, "old", "v1"));
        // configKey 变更触发唯一性检查：无冲突
        when(configMapper.selectCount(any())).thenReturn(0L);

        SysConfig patch = config(1L, "new", "v2");
        service.update(patch);

        // 旧 key 缓存必须清除（key 可能被修改）
        verify(jsonCache).evict(key("old"));
        verify(jsonCache).setString(key("new"), "v2");
    }

    @Test
    void updateRejectsDuplicateConfigKey() {
        when(configMapper.selectById(1L)).thenReturn(config(1L, "old", "v1"));
        when(configMapper.selectCount(any())).thenReturn(1L);

        SysConfig patch = config(1L, "taken", "v2");

        assertThatThrownBy(() -> service.update(patch)).isInstanceOf(BusinessException.class);
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void updateWithNullValueOnlyEvicts() {
        when(configMapper.selectById(1L)).thenReturn(config(1L, "old", "v1"));

        SysConfig patch = config(1L, "old", null);
        service.update(patch);

        verify(jsonCache).evict(key("old"));
        verify(jsonCache, never()).setString(anyString(), anyString());
    }

    @Test
    void updateRejectsMissing() {
        when(configMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(config(99L, "x", "v")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addWritesCacheOnlyForNonNullValue() {
        when(configMapper.selectCount(any())).thenReturn(0L);
        service.add(config(null, "a", "v"));
        verify(jsonCache).setString(key("a"), "v");

        service.add(config(null, "b", null));
        verify(jsonCache, never()).setString(eq(key("b")), anyString());
    }

    @Test
    void addRejectsOversizedConfigName() {
        // 回归：超长参数名曾触发 DB varchar(100) DataTruncation → 500；现业务层返回友好 400
        SysConfig c = config(null, "a", "v");
        c.setConfigName("n".repeat(101));

        assertThatThrownBy(() -> service.add(c)).isInstanceOf(BusinessException.class);
        verify(configMapper, never()).insert(any(SysConfig.class));
    }

    @Test
    void addRejectsOversizedConfigValue() {
        // 回归：超长参数值曾触发 DB varchar(500) DataTruncation → 500
        SysConfig c = config(null, "a", "v".repeat(501));

        assertThatThrownBy(() -> service.add(c)).isInstanceOf(BusinessException.class);
        verify(configMapper, never()).insert(any(SysConfig.class));
    }

    @Test
    void updateRejectsOversizedConfigKey() {
        SysConfig patch = config(1L, "k".repeat(101), "v");
        patch.setConfigName("ok");

        assertThatThrownBy(() -> service.update(patch)).isInstanceOf(BusinessException.class);
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void deleteEvictsCache() {
        when(configMapper.selectById(1L)).thenReturn(config(1L, "a", "v"));
        service.delete(1L);
        verify(jsonCache).evict(key("a"));
    }
}
