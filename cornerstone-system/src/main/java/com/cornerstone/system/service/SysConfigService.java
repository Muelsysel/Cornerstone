package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysConfig;

/** 参数配置服务接口（CRUD + Redis 缓存）。 */
public interface SysConfigService {

    /** 参数分页查询 */
    Page<SysConfig> page(
            long current, long size, String configName, String configKey, String configType);

    /** 按键名查询参数值（走 Redis 缓存，可降级） */
    String getValueByKey(String configKey);

    /** 新增参数 */
    SysConfig add(SysConfig config);

    /** 编辑参数 */
    SysConfig update(SysConfig config);

    /** 删除参数 */
    void delete(Long configId);
}
