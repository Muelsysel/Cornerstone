package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.constant.CacheConstants;
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import com.cornerstone.system.domain.mapper.SysDictDataMapper;
import com.cornerstone.system.domain.mapper.SysDictTypeMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysDictService;
import com.cornerstone.system.util.JsonCache;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 字典服务实现。 字典数据按 dict_type 缓存到 Redis（key: cornerstone:dict:{dictType}）。 Redis 不可用时降级直查库，保证功能可用。 */
@Service
public class SysDictServiceImpl implements SysDictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final JsonCache jsonCache;

    public SysDictServiceImpl(
            SysDictTypeMapper typeMapper, SysDictDataMapper dataMapper, JsonCache jsonCache) {
        this.typeMapper = typeMapper;
        this.dataMapper = dataMapper;
        this.jsonCache = jsonCache;
    }

    @Override
    public Page<SysDictType> pageType(
            long current, long size, String dictName, String dictType, String status) {
        LambdaQueryWrapper<SysDictType> wrapper =
                new LambdaQueryWrapper<SysDictType>()
                        .like(hasText(dictName), SysDictType::getDictName, dictName)
                        .like(hasText(dictType), SysDictType::getDictType, dictType)
                        .eq(hasText(status), SysDictType::getStatus, status)
                        .orderByAsc(SysDictType::getId);
        Page<SysDictType> page = new Page<>(current, size);
        typeMapper.selectPage(page, wrapper);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictType addType(SysDictType type) {
        long exists =
                typeMapper.selectCount(
                        new LambdaQueryWrapper<SysDictType>()
                                .eq(SysDictType::getDictType, type.getDictType()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_EXISTS);
        }
        try {
            typeMapper.insert(type);
        } catch (DuplicateKeyException e) {
            // 并发同类型：唯一索引兜底，转为业务错误而非裸 500
            throw new BusinessException(SystemErrorCode.DICT_TYPE_EXISTS);
        }
        return type;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictType updateType(SysDictType type) {
        SysDictType exist = typeMapper.selectById(type.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        // 修改 dictType 时校验唯一性（排除自己；并发撞唯一索引由 DuplicateKeyException 兜底）
        if (hasText(type.getDictType()) && !type.getDictType().equals(exist.getDictType())) {
            long exists =
                    typeMapper.selectCount(
                            new LambdaQueryWrapper<SysDictType>()
                                    .eq(SysDictType::getDictType, type.getDictType())
                                    .ne(SysDictType::getId, type.getId()));
            if (exists > 0) {
                throw new BusinessException(SystemErrorCode.DICT_TYPE_EXISTS);
            }
        }
        try {
            typeMapper.updateById(type);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_EXISTS);
        }
        return typeMapper.selectById(type.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long typeId) {
        SysDictType type = typeMapper.selectById(typeId);
        if (type == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        typeMapper.deleteById(typeId);
        dataMapper.delete(
                new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, type.getDictType()));
        jsonCache.evict(String.format(CacheConstants.DICT_KEY, type.getDictType()));
    }

    @Override
    public List<SysDictData> listData(String dictType) {
        String key = String.format(CacheConstants.DICT_KEY, dictType);
        List<SysDictData> cached = jsonCache.getList(key, SysDictData.class);
        if (cached != null) {
            return cached;
        }
        List<SysDictData> data =
                dataMapper.selectList(
                        new LambdaQueryWrapper<SysDictData>()
                                .eq(SysDictData::getDictType, dictType)
                                .eq(SysDictData::getStatus, "0")
                                // 确定性排序：sort 可重复，按 id 升序兜底
                                .orderByAsc(SysDictData::getDictSort)
                                .orderByAsc(SysDictData::getId));
        jsonCache.setList(key, data);
        return data;
    }

    @Override
    public Page<SysDictData> pageData(
            long current, long size, String dictType, String dictLabel, String status) {
        LambdaQueryWrapper<SysDictData> wrapper =
                new LambdaQueryWrapper<SysDictData>()
                        .eq(hasText(dictType), SysDictData::getDictType, dictType)
                        .like(hasText(dictLabel), SysDictData::getDictLabel, dictLabel)
                        .eq(hasText(status), SysDictData::getStatus, status)
                        // 确定性排序：sort 可重复，按 id 升序兜底，保证翻页不重不漏
                        .orderByAsc(SysDictData::getDictSort)
                        .orderByAsc(SysDictData::getId);
        Page<SysDictData> page = new Page<>(current, size);
        dataMapper.selectPage(page, wrapper);
        return page;
    }

    @Override
    public SysDictData addData(SysDictData data) {
        dataMapper.insert(data);
        jsonCache.evict(String.format(CacheConstants.DICT_KEY, data.getDictType()));
        return data;
    }

    @Override
    public SysDictData updateData(SysDictData data) {
        SysDictData exist = dataMapper.selectById(data.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        dataMapper.updateById(data);
        // 缓存一致性：dictType 可能被修改 → 新旧类型缓存都清
        jsonCache.evict(String.format(CacheConstants.DICT_KEY, exist.getDictType()));
        if (data.getDictType() != null && !data.getDictType().equals(exist.getDictType())) {
            jsonCache.evict(String.format(CacheConstants.DICT_KEY, data.getDictType()));
        }
        return dataMapper.selectById(data.getId());
    }

    @Override
    public void deleteData(Long dataId) {
        SysDictData data = dataMapper.selectById(dataId);
        if (data != null) {
            dataMapper.deleteById(dataId);
            jsonCache.evict(String.format(CacheConstants.DICT_KEY, data.getDictType()));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
