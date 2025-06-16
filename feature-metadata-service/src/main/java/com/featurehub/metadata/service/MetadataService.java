package com.featurehub.metadata.service;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.common.domain.StorageType;
import com.featurehub.metadata.mapper.FeatureMetadataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 元数据服务业务逻辑
 * 负责特征元数据的CRUD操作和缓存管理
 */
@Service
public class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    
    private static final String CACHE_PREFIX = "metadata:";
    private static final int CACHE_TTL_MINUTES = 30;

    @Autowired
    private FeatureMetadataMapper metadataMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取单个元数据
     */
    public FeatureMetadata getMetadata(String key) {
        try {
            // 1. 先从缓存获取
            FeatureMetadata cached = getFromCache(key);
            if (cached != null) {
                return cached;
            }

            // 2. 从数据库获取
            FeatureMetadata metadata = metadataMapper.selectByKey(key);
            if (metadata != null) {
                // 3. 写入缓存
                putToCache(key, metadata);
            }

            return metadata;
        } catch (Exception e) {
            logger.error("Error getting metadata for key: {}", key, e);
            throw new RuntimeException("Failed to get metadata", e);
        }
    }

    /**
     * 批量获取元数据
     */
    public List<FeatureMetadata> getBatchMetadata(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 1. 批量从缓存获取
            Map<String, FeatureMetadata> cachedResults = batchGetFromCache(keys);
            
            // 2. 找出缓存中没有的key
            List<String> missedKeys = keys.stream()
                    .filter(key -> !cachedResults.containsKey(key))
                    .collect(Collectors.toList());

            // 3. 从数据库批量获取缺失的数据
            if (!missedKeys.isEmpty()) {
                List<FeatureMetadata> dbResults = metadataMapper.selectByKeys(missedKeys);
                
                // 4. 批量写入缓存
                for (FeatureMetadata metadata : dbResults) {
                    cachedResults.put(metadata.getKeyName(), metadata);
                    putToCache(metadata.getKeyName(), metadata);
                }
            }

            // 5. 按原始顺序返回结果
            return keys.stream()
                    .map(cachedResults::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting batch metadata for keys: {}", keys, e);
            throw new RuntimeException("Failed to get batch metadata", e);
        }
    }

    /**
     * 创建或更新元数据
     */
    @Transactional
    public boolean upsertMetadata(FeatureMetadata metadata) {
        try {
            metadata.setUpdateTime(System.currentTimeMillis());
            
            FeatureMetadata existing = metadataMapper.selectByKey(metadata.getKeyName());
            boolean isNew;
            
            if (existing == null) {
                // 创建新记录
                metadata.setCreateTime(System.currentTimeMillis());
                metadataMapper.insert(metadata);
                isNew = true;
                logger.info("Created new metadata for key: {}", metadata.getKeyName());
            } else {
                // 更新现有记录
                metadata.setCreateTime(existing.getCreateTime());
                metadataMapper.updateByKey(metadata);
                isNew = false;
                logger.info("Updated metadata for key: {}", metadata.getKeyName());
            }

            // 更新缓存
            putToCache(metadata.getKeyName(), metadata);
            
            return isNew;
        } catch (Exception e) {
            logger.error("Error upserting metadata for key: {}", metadata.getKeyName(), e);
            throw new RuntimeException("Failed to upsert metadata", e);
        }
    }

    /**
     * 更新元数据
     */
    @Transactional
    public boolean updateMetadata(FeatureMetadata metadata) {
        try {
            metadata.setUpdateTime(System.currentTimeMillis());
            
            int updated = metadataMapper.updateByKey(metadata);
            if (updated > 0) {
                // 更新缓存
                putToCache(metadata.getKeyName(), metadata);
                logger.info("Updated metadata for key: {}", metadata.getKeyName());
                return true;
            } else {
                logger.warn("No metadata found to update for key: {}", metadata.getKeyName());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error updating metadata for key: {}", metadata.getKeyName(), e);
            throw new RuntimeException("Failed to update metadata", e);
        }
    }

    /**
     * 批量更新元数据
     */
    @Transactional
    public Map<String, Boolean> batchUpdateMetadata(List<Map<String, Object>> updates) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map<String, Object> updateData : updates) {
            try {
                FeatureMetadata metadata = objectMapper.convertValue(updateData, FeatureMetadata.class);
                boolean success = updateMetadata(metadata);
                results.put(metadata.getKeyName(), success);
            } catch (Exception e) {
                String keyName = (String) updateData.get("keyName");
                logger.error("Error updating metadata for key: {}", keyName, e);
                results.put(keyName, false);
            }
        }
        
        return results;
    }

    /**
     * 删除元数据
     */
    @Transactional
    public boolean deleteMetadata(String key) {
        try {
            int deleted = metadataMapper.deleteByKey(key);
            if (deleted > 0) {
                // 删除缓存
                removeFromCache(key);
                logger.info("Deleted metadata for key: {}", key);
                return true;
            } else {
                logger.warn("No metadata found to delete for key: {}", key);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting metadata for key: {}", key, e);
            throw new RuntimeException("Failed to delete metadata", e);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats(String storageType, String businessTag) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 按存储类型统计
            Map<String, Integer> storageStats = metadataMapper.countByStorageType();
            stats.put("storage_stats", storageStats);
            
            // 总体统计
            int totalKeys = storageStats.values().stream().mapToInt(Integer::intValue).sum();
            stats.put("total_keys", totalKeys);
            
            // 如果指定了存储类型，获取详细统计
            if (StringUtils.hasText(storageType)) {
                StorageType type = StorageType.fromValue(storageType);
                Map<String, Object> detailStats = metadataMapper.getDetailStatsByStorageType(type);
                stats.put("detail_stats", detailStats);
            }
            
            // 如果指定了业务标签，获取业务统计
            if (StringUtils.hasText(businessTag)) {
                Map<String, Object> businessStats = metadataMapper.getStatsByBusinessTag(businessTag);
                stats.put("business_stats", businessStats);
            }
            
            // 最近24小时的活跃统计
            long yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
            int activeKeys = metadataMapper.countActiveKeys(yesterday);
            stats.put("active_keys_24h", activeKeys);
            
            stats.put("timestamp", System.currentTimeMillis());
            
            return stats;
        } catch (Exception e) {
            logger.error("Error getting metadata stats", e);
            throw new RuntimeException("Failed to get stats", e);
        }
    }

    /**
     * 清理过期元数据
     */
    @Transactional
    public int cleanupExpiredMetadata() {
        try {
            long currentTime = System.currentTimeMillis();
            List<String> expiredKeys = metadataMapper.selectExpiredKeys(currentTime);
            
            if (expiredKeys.isEmpty()) {
                logger.info("No expired metadata found");
                return 0;
            }
            
            // 批量删除过期数据
            int deleted = metadataMapper.deleteExpiredKeys(currentTime);
            
            // 批量删除缓存
            for (String key : expiredKeys) {
                removeFromCache(key);
            }
            
            logger.info("Cleaned up {} expired metadata records", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error cleaning up expired metadata", e);
            throw new RuntimeException("Failed to cleanup expired metadata", e);
        }
    }

    /**
     * 获取健康状态信息
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查数据库连接
            int totalCount = metadataMapper.countTotal();
            health.put("database_status", "healthy");
            health.put("total_metadata_count", totalCount);
            
            // 检查Redis连接
            try {
                redisTemplate.opsForValue().set("health_check", "test", 10, TimeUnit.SECONDS);
                String retrieved = redisTemplate.opsForValue().get("health_check");
                health.put("redis_status", "test".equals(retrieved) ? "healthy" : "unhealthy");
            } catch (Exception e) {
                health.put("redis_status", "unhealthy");
                health.put("redis_error", e.getMessage());
            }
            
            health.put("status", "healthy");
            health.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("database_status", "unhealthy");
        }
        
        return health;
    }

    /**
     * 从缓存获取元数据
     */
    private FeatureMetadata getFromCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, FeatureMetadata.class);
            }
        } catch (Exception e) {
            logger.warn("Error getting metadata from cache for key: {}", key, e);
        }
        return null;
    }

    /**
     * 批量从缓存获取元数据
     */
    private Map<String, FeatureMetadata> batchGetFromCache(List<String> keys) {
        Map<String, FeatureMetadata> results = new HashMap<>();
        try {
            List<String> cacheKeys = keys.stream()
                    .map(key -> CACHE_PREFIX + key)
                    .collect(Collectors.toList());
            
            List<String> cachedValues = redisTemplate.opsForValue().multiGet(cacheKeys);
            
            for (int i = 0; i < keys.size(); i++) {
                if (i < cachedValues.size() && cachedValues.get(i) != null) {
                    try {
                        FeatureMetadata metadata = objectMapper.readValue(cachedValues.get(i), FeatureMetadata.class);
                        results.put(keys.get(i), metadata);
                    } catch (Exception e) {
                        logger.warn("Error parsing cached metadata for key: {}", keys.get(i), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error batch getting metadata from cache", e);
        }
        return results;
    }

    /**
     * 写入缓存
     */
    private void putToCache(String key, FeatureMetadata metadata) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String value = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(cacheKey, value, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Error putting metadata to cache for key: {}", key, e);
        }
    }

    /**
     * 从缓存删除
     */
    private void removeFromCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("Error removing metadata from cache for key: {}", key, e);
        }
    }
} 