package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.constant.CacheConstants;
import com.cornerstone.system.domain.entity.SysConfig;
import com.cornerstone.system.domain.mapper.SysConfigMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysConfigService;
import com.cornerstone.system.util.JsonCache;
import org.springframework.stereotype.Service;

/** 参数配置服务实现。参数值缓存 key: cornerstone:config:{configKey}。 */
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;
    private final JsonCache jsonCache;

    public SysConfigServiceImpl(SysConfigMapper configMapper, JsonCache jsonCache) {
        this.configMapper = configMapper;
        this.jsonCache = jsonCache;
    }

    @Override
    public Page<SysConfig> page(
            long current, long size, String configName, String configKey, String configType) {
        LambdaQueryWrapper<SysConfig> wrapper =
                new LambdaQueryWrapper<SysConfig>()
                        .like(hasText(configName), SysConfig::getConfigName, configName)
                        .like(hasText(configKey), SysConfig::getConfigKey, configKey)
                        .eq(hasText(configType), SysConfig::getConfigType, configType)
                        .orderByAsc(SysConfig::getId);
        Page<SysConfig> page = new Page<>(current, size);
        configMapper.selectPage(page, wrapper);
        return page;
    }

    @Override
    public String getValueByKey(String configKey) {
        String cacheKey = String.format(CacheConstants.CONFIG_KEY, configKey);
        String cached = jsonCache.getString(cacheKey);
        if (cached != null) {
            return cached;
        }
        SysConfig config =
                configMapper.selectOne(
                        new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey));
        String value = config != null ? config.getConfigValue() : null;
        if (value != null) {
            jsonCache.setString(cacheKey, value);
        }
        return value;
    }

    @Override
    public SysConfig add(SysConfig config) {
        long exists =
                configMapper.selectCount(
                        new LambdaQueryWrapper<SysConfig>()
                                .eq(SysConfig::getConfigKey, config.getConfigKey()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.CONFIG_KEY_EXISTS);
        }
        configMapper.insert(config);
        // 仅非空值写缓存；空值由 getValueByKey 回源 DB，避免缓存污染
        if (config.getConfigValue() != null) {
            jsonCache.setString(
                    String.format(CacheConstants.CONFIG_KEY, config.getConfigKey()),
                    config.getConfigValue());
        }
        return config;
    }

    @Override
    public SysConfig update(SysConfig config) {
        SysConfig exist = configMapper.selectById(config.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        configMapper.updateById(config);
        // 缓存同步：configKey 可能被修改 → 先清旧 key 缓存，再写新值（null 值不写，避免读到过期旧值）
        jsonCache.evict(String.format(CacheConstants.CONFIG_KEY, exist.getConfigKey()));
        if (config.getConfigKey() != null && config.getConfigValue() != null) {
            jsonCache.setString(
                    String.format(CacheConstants.CONFIG_KEY, config.getConfigKey()),
                    config.getConfigValue());
        }
        return configMapper.selectById(config.getId());
    }

    @Override
    public void delete(Long configId) {
        SysConfig config = configMapper.selectById(configId);
        if (config != null) {
            configMapper.deleteById(configId);
            jsonCache.evict(String.format(CacheConstants.CONFIG_KEY, config.getConfigKey()));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
