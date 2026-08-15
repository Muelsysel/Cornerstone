package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysDictData;
import com.cornerstone.system.domain.entity.SysDictType;
import java.util.List;

/** 字典服务接口（类型 + 数据，数据查询写入 Redis 缓存）。 */
public interface SysDictService {

    /** 字典类型分页查询 */
    Page<SysDictType> pageType(
            long current, long size, String dictName, String dictType, String status);

    /** 新增字典类型 */
    SysDictType addType(SysDictType type);

    /** 编辑字典类型 */
    SysDictType updateType(SysDictType type);

    /** 删除字典类型（连同其数据） */
    void deleteType(Long typeId);

    /** 按字典类型查询数据（走 Redis 缓存） */
    List<SysDictData> listData(String dictType);

    /** 字典数据分页查询 */
    Page<SysDictData> pageData(
            long current, long size, String dictType, String dictLabel, String status);

    /** 新增字典数据 */
    SysDictData addData(SysDictData data);

    /** 编辑字典数据 */
    SysDictData updateData(SysDictData data);

    /** 删除字典数据 */
    void deleteData(Long dataId);
}
