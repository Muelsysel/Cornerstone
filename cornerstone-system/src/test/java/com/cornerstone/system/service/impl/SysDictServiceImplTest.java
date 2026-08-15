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
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import com.cornerstone.system.domain.mapper.SysDictDataMapper;
import com.cornerstone.system.domain.mapper.SysDictTypeMapper;
import com.cornerstone.system.util.JsonCache;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 字典服务单测：列表缓存命中/回源、数据变更后缓存失效（字典缓存一致性）。 */
class SysDictServiceImplTest {

    private final SysDictTypeMapper typeMapper = mock(SysDictTypeMapper.class);
    private final SysDictDataMapper dataMapper = mock(SysDictDataMapper.class);
    private final JsonCache jsonCache = mock(JsonCache.class);
    private final SysDictServiceImpl service =
            new SysDictServiceImpl(typeMapper, dataMapper, jsonCache);

    private String dictKey(String type) {
        return String.format(CacheConstants.DICT_KEY, type);
    }

    private SysDictData data(Long id, String type, String label) {
        SysDictData d = new SysDictData();
        d.setId(id);
        d.setDictType(type);
        d.setDictLabel(label);
        d.setStatus("0");
        return d;
    }

    @BeforeEach
    void setUp() {
        when(jsonCache.getList(anyString(), eq(SysDictData.class))).thenReturn(null);
        when(dataMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void listDataHitsCache() {
        when(jsonCache.getList(dictKey("gender"), SysDictData.class))
                .thenReturn(List.of(data(1L, "gender", "男")));

        List<SysDictData> result = service.listData("gender");

        assertThat(result).hasSize(1);
        verify(dataMapper, never()).selectList(any());
    }

    @Test
    void listDataMissesAndCaches() {
        when(dataMapper.selectList(any())).thenReturn(List.of(data(1L, "gender", "男")));

        List<SysDictData> result = service.listData("gender");

        assertThat(result).hasSize(1);
        verify(jsonCache).setList(eq(dictKey("gender")), any());
    }

    @Test
    void addTypeRejectsOversizedDictName() {
        // 回归：超长字典名称曾触发 DB varchar(100) DataTruncation → 500；现业务层返回友好 400
        SysDictType type = new SysDictType();
        type.setDictName("n".repeat(101));
        type.setDictType("ok");

        assertThatThrownBy(() -> service.addType(type)).isInstanceOf(BusinessException.class);
        verify(typeMapper, never()).insert(any(SysDictType.class));
    }

    @Test
    void addDataRejectsOversizedDictValue() {
        // 回归：超长字典键值曾触发 DB varchar(100) DataTruncation → 500
        SysDictData d = data(null, "gender", "男");
        d.setDictValue("v".repeat(101));

        assertThatThrownBy(() -> service.addData(d)).isInstanceOf(BusinessException.class);
        verify(dataMapper, never()).insert(any(SysDictData.class));
    }

    @Test
    void addTypeRejectsDuplicate() {
        when(typeMapper.selectCount(any())).thenReturn(1L);
        SysDictType type = new SysDictType();
        type.setDictType("gender");
        assertThatThrownBy(() -> service.addType(type)).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateTypeRejectsDuplicateDictType() {
        SysDictType exist = new SysDictType();
        exist.setId(1L);
        exist.setDictType("gender");
        SysDictType patch = new SysDictType();
        patch.setId(1L);
        patch.setDictType("status"); // 改名为已存在的类型
        when(typeMapper.selectById(1L)).thenReturn(exist);
        when(typeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateType(patch)).isInstanceOf(BusinessException.class);
        verify(typeMapper, never()).updateById(any(SysDictType.class));
    }

    @Test
    void updateTypeWithSameDictTypeSkipsCheck() {
        SysDictType exist = new SysDictType();
        exist.setId(1L);
        exist.setDictType("gender");
        SysDictType patch = new SysDictType();
        patch.setId(1L);
        patch.setDictType("gender"); // 类型不变（只改名称）
        when(typeMapper.selectById(1L)).thenReturn(exist);

        service.updateType(patch);

        // 类型未变不触发唯一性 count
        verify(typeMapper, never()).selectCount(any());
        verify(typeMapper).updateById(any(SysDictType.class));
    }

    @Test
    void addDataEvictsCache() {
        SysDictData d = data(null, "gender", "男");
        service.addData(d);
        verify(jsonCache).evict(dictKey("gender"));
    }

    @Test
    void updateDataEvictsCache() {
        when(dataMapper.selectById(1L)).thenReturn(data(1L, "gender", "男"));
        SysDictData d = data(1L, "gender", "女");
        service.updateData(d);
        verify(jsonCache).evict(dictKey("gender"));
    }

    @Test
    void updateDataWithChangedTypeEvictsBothCaches() {
        when(dataMapper.selectById(1L)).thenReturn(data(1L, "gender", "男"));
        SysDictData d = data(1L, "status", "女"); // dictType 变更
        service.updateData(d);
        // 新旧类型缓存都必须清除
        verify(jsonCache).evict(dictKey("gender"));
        verify(jsonCache).evict(dictKey("status"));
    }

    @Test
    void deleteTypeRemovesDataAndEvictsCache() {
        SysDictType type = new SysDictType();
        type.setId(1L);
        type.setDictType("gender");
        when(typeMapper.selectById(1L)).thenReturn(type);

        service.deleteType(1L);

        verify(dataMapper).delete(any());
        verify(jsonCache).evict(dictKey("gender"));
    }

    @Test
    void deleteDataEvictsCache() {
        when(dataMapper.selectById(1L)).thenReturn(data(1L, "gender", "男"));
        service.deleteData(1L);
        verify(jsonCache).evict(dictKey("gender"));
    }
}
