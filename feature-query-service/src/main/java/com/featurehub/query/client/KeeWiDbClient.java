package com.featurehub.query.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * KeeWiDB客户端封装
 * KeeWiDB兼容Redis协议，用作冷数据存储
 */
@Component
public class KeeWiDbClient {

    private static final Logger logger = LoggerFactory.getLogger(KeeWiDbClient.class);

    @Autowired
    @Qualifier("keewidbRedisTemplate")
    private RedisTemplate<String, String> keewidbTemplate;

    /**
     * 获取单个Key的值
     */
    public String get(String key) {
        try {
            return keewidbTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("KeeWiDB get error for key: {}", key, e);
            throw new RuntimeException("KeeWiDB get operation failed", e);
        }
    }

    /**
     * 设置Key-Value
     */
    public boolean set(String key, String value) {
        try {
            keewidbTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("KeeWiDB set error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置Key-Value with TTL
     */
    public boolean set(String key, String value, Long ttlSeconds) {
        try {
            if (ttlSeconds != null && ttlSeconds > 0) {
                keewidbTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            } else {
                keewidbTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("KeeWiDB set with TTL error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量获取多个Key的值
     */
    public Map<String, String> mget(List<String> keys) {
        Map<String, String> result = new HashMap<>();
        try {
            List<String> values = keewidbTemplate.opsForValue().multiGet(keys);
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
            logger.error("KeeWiDB mget error for keys: {}", keys, e);
            throw new RuntimeException("KeeWiDB mget operation failed", e);
        }
    }

    /**
     * 删除Key
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = keewidbTemplate.delete(key);
            return deleted != null && deleted;
        } catch (Exception e) {
            logger.error("KeeWiDB delete error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量删除Key
     */
    public long delete(List<String> keys) {
        try {
            Long deleted = keewidbTemplate.delete(keys);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            logger.error("KeeWiDB batch delete error for keys: {}", keys, e);
            return 0;
        }
    }

    /**
     * 检查Key是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = keewidbTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("KeeWiDB exists error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置Key的过期时间
     */
    public boolean expire(String key, long timeoutSeconds) {
        try {
            Boolean result = keewidbTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
            return result != null && result;
        } catch (Exception e) {
            logger.error("KeeWiDB expire error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取Key的剩余生存时间
     */
    public long ttl(String key) {
        try {
            Long ttl = keewidbTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            logger.error("KeeWiDB ttl error for key: {}", key, e);
            return -1;
        }
    }

    /**
     * 数据迁移：从Redis复制数据到KeeWiDB
     */
    public boolean migrateFromRedis(String key, String value, Long ttlSeconds) {
        try {
            return set(key, value, ttlSeconds);
        } catch (Exception e) {
            logger.error("KeeWiDB migration error for key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量数据迁移
     */
    public Map<String, Boolean> batchMigrateFromRedis(Map<String, String> keyValues, Map<String, Long> keyTtls) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Long ttl = keyTtls.get(key);
            
            boolean success = migrateFromRedis(key, value, ttl);
            results.put(key, success);
        }
        
        return results;
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            // 执行一个简单的ping操作
            String testKey = "keewidb_health_check_" + System.currentTimeMillis();
            String testValue = "test";
            
            set(testKey, testValue);
            String retrieved = get(testKey);
            delete(testKey);
            
            return testValue.equals(retrieved);
        } catch (Exception e) {
            logger.warn("KeeWiDB health check failed", e);
            return false;
        }
    }

    /**
     * 获取连接信息
     */
    public Map<String, Object> getConnectionInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("type", "keewidb");
            info.put("healthy", isHealthy());
            info.put("timestamp", System.currentTimeMillis());
            info.put("description", "KeeWiDB - Redis-compatible disk storage for cold data");
        } catch (Exception e) {
            logger.error("Error getting KeeWiDB connection info", e);
            info.put("error", e.getMessage());
        }
        return info;
    }

    /**
     * 获取存储统计信息
     */
    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 这里可以添加KeeWiDB特有的统计信息
            stats.put("storage_type", "disk");
            stats.put("performance_tier", "cold");
            stats.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Error getting KeeWiDB storage stats", e);
            stats.put("error", e.getMessage());
        }
        return stats;
    }
} 