package com.featurehub.query.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis客户端封装
 * 提供Redis操作的统一接口
 */
@Component
public class RedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取单个Key的值
     */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Redis get error for key: {}", key, e);
            throw new RuntimeException("Redis get operation failed", e);
        }
    }

    /**
     * 设置Key-Value
     */
    public boolean set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("Redis set error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置Key-Value with TTL
     */
    public boolean set(String key, String value, Long ttlSeconds) {
        try {
            if (ttlSeconds != null && ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("Redis set with TTL error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量获取多个Key的值
     */
    public Map<String, String> mget(List<String> keys) {
        Map<String, String> result = new HashMap<>();
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (int i = 0; i < keys.size(); i++) {
                    String key = keys.get(i);
                    String value = (i < values.size()) ? values.get(i) : null;
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Redis mget error for keys: {}", keys, e);
            throw new RuntimeException("Redis mget operation failed", e);
        }
    }

    /**
     * 删除Key
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            return deleted != null && deleted;
        } catch (Exception e) {
            logger.error("Redis delete error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量删除Key
     */
    public long delete(List<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            logger.error("Redis batch delete error for keys: {}", keys, e);
            return 0;
        }
    }

    /**
     * 检查Key是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("Redis exists error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置Key的过期时间
     */
    public boolean expire(String key, long timeoutSeconds) {
        try {
            Boolean result = redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
            return result != null && result;
        } catch (Exception e) {
            logger.error("Redis expire error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取Key的剩余生存时间
     */
    public long ttl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            logger.error("Redis ttl error for key: {}", key, e);
            return -1;
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            // 执行一个简单的ping操作
            String testKey = "health_check_" + System.currentTimeMillis();
            String testValue = "test";
            
            set(testKey, testValue);
            String retrieved = get(testKey);
            delete(testKey);
            
            return testValue.equals(retrieved);
        } catch (Exception e) {
            logger.warn("Redis health check failed", e);
            return false;
        }
    }

    /**
     * 获取连接信息
     */
    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 这里可以添加更多Redis连接信息
            info.put("type", "redis");
            info.put("healthy", isHealthy());
            info.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Error getting Redis connection info", e);
            info.put("error", e.getMessage());
        }
        return info;
    }
} 