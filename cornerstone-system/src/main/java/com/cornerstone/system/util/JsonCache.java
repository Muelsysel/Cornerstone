package com.cornerstone.system.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis JSON 缓存工具。任何 Redis 异常都被吞掉并记日志， 使缓存成为可降级的增强而非硬依赖（无 Redis 时直查库仍可用）。 */
@Component
public class JsonCache {

    private static final Logger log = LoggerFactory.getLogger(JsonCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 读取对象列表，无缓存或异常时返回 null（调用方回源 DB） */
    public <T> List<T> getList(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败 key={}, 降级直查", key);
            return null;
        }
    }

    /** 写入对象列表，异常静默降级 */
    public void setList(String key, List<?> value) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.warn("Redis 写入缓存失败 key={}", key);
        }
    }

    /** 读取字符串，异常静默降级返回 null */
    public String getString(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败 key={}", key);
            return null;
        }
    }

    /** 写入字符串，异常静默降级 */
    public void setString(String key, String value) {
        try {
            redis.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis 写入缓存失败 key={}", key);
        }
    }

    /** 删除缓存键，异常静默降级 */
    public void evict(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除缓存失败 key={}", key);
        }
    }
}
