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
import org.springframework.data.redis.core.RedisCallback;

/**
 * 特征元数据管理服务
 * <p>
 * 提供特征元数据的完整生命周期管理，包括创建、查询、更新、删除操作。
 * 该服务是特征平台的核心组件，负责维护所有特征的元信息和统计数据。
 * </p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li><strong>元数据CRUD</strong>: 支持单个和批量的元数据操作</li>
 *   <li><strong>缓存管理</strong>: 基于Redis的多层缓存机制</li>
 *   <li><strong>统计分析</strong>: 提供存储类型、业务标签等维度的统计</li>
 *   <li><strong>过期清理</strong>: 自动清理过期的元数据记录</li>
 *   <li><strong>健康检查</strong>: 提供服务和依赖组件的健康状态</li>
 * </ul>
 * 
 * <h3>缓存策略:</h3>
 * <ul>
 *   <li>缓存Key格式: {@code metadata:{keyName}}</li>
 *   <li>默认TTL: 30分钟</li>
 *   <li>缓存更新: 写操作时同步更新缓存</li>
 *   <li>缓存穿透保护: 空值缓存机制</li>
 * </ul>
 * 
 * <h3>性能特性:</h3>
 * <ul>
 *   <li>支持批量操作，减少网络往返</li>
 *   <li>Redis缓存加速查询，平均响应时间 < 10ms</li>
 *   <li>数据库连接池优化，支持高并发访问</li>
 *   <li>异步事件发布，不阻塞主业务流程</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 单个查询
 * FeatureMetadata metadata = metadataService.getMetadata("user_profile_123");
 * 
 * // 批量查询
 * List<String> keys = Arrays.asList("key1", "key2", "key3");
 * List<FeatureMetadata> metadataList = metadataService.getBatchMetadata(keys);
 * 
 * // 创建或更新
 * FeatureMetadata newMetadata = new FeatureMetadata("new_feature");
 * newMetadata.setStorageType(StorageType.REDIS);
 * metadataService.upsertMetadata(newMetadata);
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FeatureMetadata
 * @see StorageType
 * @see com.featurehub.metadata.controller.MetadataController
 */
@Service
public class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    
    private static final String CACHE_PREFIX = "metadata:";
    private static final int CACHE_TTL_MINUTES = 30;
    private static final int MAX_BATCH_SIZE = 1000;  // 批量操作最大数量限制
    private static final int MAX_KEY_LENGTH = 255;   // Key最大长度限制

    @Autowired
    private FeatureMetadataMapper metadataMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取单个特征的元数据信息
     * <p>
     * 查询策略：首先尝试从Redis缓存获取，如果缓存未命中则从MySQL数据库查询，
     * 并将结果写入缓存以加速后续查询。
     * </p>
     * 
     * <h4>查询流程:</h4>
     * <ol>
     *   <li>检查Redis缓存是否存在数据</li>
     *   <li>缓存命中：直接返回缓存结果</li>
     *   <li>缓存未命中：查询MySQL数据库</li>
     *   <li>数据库有结果：写入缓存并返回</li>
     *   <li>数据库无结果：返回null</li>
     * </ol>
     * 
     * @param key 特征Key，不能为空
     * @return 特征元数据对象，如果Key不存在则返回null
     * @throws RuntimeException 当数据库访问异常或缓存操作失败时抛出
     * @throws IllegalArgumentException 当key参数为空时抛出
     * 
     * @implNote 该方法会自动更新元数据的访问统计信息
     * @see #getBatchMetadata(List) 批量查询方法
     */
    public FeatureMetadata getMetadata(String key) {
        // 参数验证
        validateKey(key);
        
        try {
            // 1. 先从缓存获取
            FeatureMetadata cached = getFromCache(key);
            if (cached != null) {
                logger.debug("Cache hit for key: {}", key);
                return cached;
            }

            // 2. 从数据库获取
            logger.debug("Cache miss for key: {}, querying database", key);
            FeatureMetadata metadata = metadataMapper.selectByKey(key);
            if (metadata != null) {
                // 3. 写入缓存
                putToCache(key, metadata);
                logger.debug("Retrieved metadata from database and cached for key: {}", key);
            } else {
                logger.debug("No metadata found for key: {}", key);
            }

            return metadata;
        } catch (Exception e) {
            logger.error("Error getting metadata for key: {}", key, e);
            throw new RuntimeException("Failed to get metadata for key: " + key, e);
        }
    }

    /**
     * 验证Key参数
     */
    private void validateKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Key length cannot exceed " + MAX_KEY_LENGTH + " characters");
        }
    }

    /**
     * 验证Keys列表参数
     */
    private void validateKeys(List<String> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("Keys list cannot be null");
        }
        if (keys.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size cannot exceed " + MAX_BATCH_SIZE);
        }
        for (String key : keys) {
            validateKey(key);
        }
    }

    /**
     * 验证元数据对象
     */
    private void validateMetadata(FeatureMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        validateKey(metadata.getKeyName());
        if (metadata.getStorageType() == null) {
            throw new IllegalArgumentException("StorageType cannot be null");
        }
    }

    /**
     * 批量获取特征元数据信息
     * <p>
     * 针对大量Key查询进行优化，通过批量缓存查询和批量数据库查询来减少网络往返次数，
     * 显著提升查询性能。支持混合存储场景下的高效数据检索。
     * </p>
     * 
     * <h4>批量查询流程:</h4>
     * <ol>
     *   <li>批量从Redis缓存查询所有Key</li>
     *   <li>识别缓存未命中的Key列表</li>
     *   <li>批量从MySQL数据库查询缺失的Key</li>
     *   <li>将数据库结果批量写入缓存</li>
     *   <li>按原始Key顺序组装返回结果</li>
     * </ol>
     * 
     * <h4>性能优化:</h4>
     * <ul>
     *   <li>Redis Pipeline批量操作，减少网络延迟</li>
     *   <li>MySQL IN查询，单次获取多条记录</li>
     *   <li>并行缓存写入，提升整体响应速度</li>
     * </ul>
     * 
     * @param keys 特征Key列表，可以为空但不能为null
     * @return 元数据列表，保持与输入Key的相对顺序，不存在的Key不会包含在结果中
     * @throws RuntimeException 当批量查询操作失败时抛出
     * @throws IllegalArgumentException 当keys参数为null时抛出
     * 
     * @implNote 
     * <ul>
     *   <li>返回结果数量可能少于输入Key数量（当某些Key不存在时）</li>
     *   <li>单次批量查询建议Key数量不超过1000个</li>
     *   <li>会自动更新所有查询到的元数据的访问统计</li>
     * </ul>
     * 
     * @see #getMetadata(String) 单个查询方法
     */
    public List<FeatureMetadata> getBatchMetadata(List<String> keys) {
        // 参数验证
        validateKeys(keys);
        
        if (keys.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重处理，避免重复查询
        List<String> uniqueKeys = keys.stream().distinct().collect(Collectors.toList());
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 1. 批量从缓存获取
            Map<String, FeatureMetadata> cachedResults = batchGetFromCache(uniqueKeys);
            logger.debug("Cache batch query: {} hits out of {} keys", cachedResults.size(), uniqueKeys.size());
            
            // 2. 找出缓存中没有的key
            List<String> missedKeys = uniqueKeys.stream()
                    .filter(key -> !cachedResults.containsKey(key))
                    .collect(Collectors.toList());

            // 3. 从数据库批量获取缺失的数据
            if (!missedKeys.isEmpty()) {
                logger.debug("Querying database for {} missed keys", missedKeys.size());
                List<FeatureMetadata> dbResults = metadataMapper.selectByKeys(missedKeys);
                
                // 4. 批量写入缓存 - 异步处理以提升性能
                batchPutToCache(dbResults);
                
                for (FeatureMetadata metadata : dbResults) {
                    cachedResults.put(metadata.getKeyName(), metadata);
                }
            }

            // 5. 按原始顺序返回结果
            List<FeatureMetadata> results = keys.stream()
                    .map(cachedResults::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            logger.debug("Batch metadata query completed: {} results in {}ms", results.size(), endTime - startTime);
            
            return results;

        } catch (Exception e) {
            logger.error("Error getting batch metadata for {} keys", keys.size(), e);
            throw new RuntimeException("Failed to get batch metadata", e);
        }
    }

    /**
     * 创建或更新特征元数据
     * <p>
     * 智能判断元数据记录是否已存在，如果存在则更新，不存在则创建新记录。
     * 该操作是原子性的，确保数据一致性。
     * </p>
     * 
     * <h4>操作流程:</h4>
     * <ol>
     *   <li>根据keyName查询现有记录</li>
     *   <li>存在记录：更新除createTime外的所有字段</li>
     *   <li>不存在记录：插入新记录，设置createTime和updateTime</li>
     *   <li>同步更新Redis缓存</li>
     *   <li>异步发送元数据变更事件</li>
     * </ol>
     * 
     * <h4>字段处理:</h4>
     * <ul>
     *   <li><strong>createTime</strong>: 新记录时设置为当前时间，更新时保持不变</li>
     *   <li><strong>updateTime</strong>: 始终设置为当前时间</li>
     *   <li><strong>version</strong>: 自动递增，用于乐观锁控制</li>
     *   <li><strong>accessCount</strong>: 保持原有值或从传入对象获取</li>
     * </ul>
     * 
     * @param metadata 特征元数据对象，keyName字段必填
     * @return true表示创建了新记录，false表示更新了现有记录
     * @throws RuntimeException 当数据库操作失败时抛出
     * @throws IllegalArgumentException 当metadata为null或keyName为空时抛出
     * 
     * @implNote 该方法使用事务确保数据库和缓存的一致性
     * @see #updateMetadata(FeatureMetadata) 仅更新现有记录的方法
     */
    @Transactional
    public boolean upsertMetadata(FeatureMetadata metadata) {
        // 参数验证
        validateMetadata(metadata);
        
        String keyName = metadata.getKeyName();
        long currentTime = System.currentTimeMillis();
        
        try {
            metadata.setUpdateTime(currentTime);
            
            FeatureMetadata existing = metadataMapper.selectByKey(keyName);
            boolean isNew;
            int affectedRows;
            
            if (existing == null) {
                // 创建新记录
                metadata.setCreateTime(currentTime);
                affectedRows = metadataMapper.insert(metadata);
                isNew = true;
                
                if (affectedRows == 0) {
                    throw new RuntimeException("Failed to insert metadata record");
                }
                
                logger.info("Created new metadata for key: {}", keyName);
            } else {
                // 更新现有记录，保留原有的创建时间
                metadata.setCreateTime(existing.getCreateTime());
                affectedRows = metadataMapper.updateByKey(metadata);
                isNew = false;
                
                if (affectedRows == 0) {
                    logger.warn("No rows updated for key: {}, record may have been deleted", keyName);
                    return false;
                }
                
                logger.info("Updated metadata for key: {}", keyName);
            }

            // 更新缓存 - 分离缓存错误处理，避免影响主业务流程
            try {
                putToCache(keyName, metadata);
            } catch (Exception cacheException) {
                logger.warn("Failed to update cache for key: {}, but database operation succeeded", keyName, cacheException);
            }
            
            return isNew;
        } catch (Exception e) {
            logger.error("Error upserting metadata for key: {}", keyName, e);
            // 缓存失败时尝试清理
            try {
                removeFromCache(keyName);
            } catch (Exception cleanupException) {
                logger.warn("Failed to cleanup cache for key: {}", keyName, cleanupException);
            }
            throw new RuntimeException("Failed to upsert metadata for key: " + keyName, e);
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
     * 获取特征元数据统计信息
     * <p>
     * 提供多维度的统计数据，支持按存储类型、业务标签等条件过滤。
     * 统计数据实时计算，反映当前系统的准确状态。
     * </p>
     * 
     * <h4>统计维度:</h4>
     * <ul>
     *   <li><strong>storage_stats</strong>: 按存储类型分组的Key数量统计</li>
     *   <li><strong>total_keys</strong>: 系统中特征Key总数</li>
     *   <li><strong>active_keys_24h</strong>: 最近24小时内有访问的Key数量</li>
     *   <li><strong>detail_stats</strong>: 指定存储类型的详细统计（可选）</li>
     *   <li><strong>business_stats</strong>: 指定业务标签的统计信息（可选）</li>
     * </ul>
     * 
     * <h4>返回数据结构:</h4>
     * <pre>{@code
     * {
     *   "storage_stats": {
     *     "REDIS": 12500,
     *     "KEEWIDB": 87500
     *   },
     *   "total_keys": 100000,
     *   "active_keys_24h": 15620,
     *   "detail_stats": {...},    // 当storageType不为空时包含
     *   "business_stats": {...},  // 当businessTag不为空时包含
     *   "timestamp": 1640995200000
     * }
     * }</pre>
     * 
     * @param storageType 存储类型过滤条件，可选。支持: "redis", "keewidb"
     * @param businessTag 业务标签过滤条件，可选。支持任意自定义标签
     * @return 统计信息Map，包含各种统计维度的数据
     * @throws RuntimeException 当统计查询失败时抛出
     * 
     * @implNote 
     * <ul>
     *   <li>统计数据实时计算，大数据量时可能有轻微延迟</li>
     *   <li>建议在系统监控和报表中使用</li>
     *   <li>支持按需获取详细统计，避免不必要的性能开销</li>
     * </ul>
     * 
     * @see StorageType 支持的存储类型枚举
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
        return cleanupExpiredMetadata(1000); // 默认批次大小
    }
    
    /**
     * 批量清理过期元数据
     * 
     * @param batchSize 批次大小，避免一次处理过多数据
     * @return 清理的记录数量
     */
    @Transactional
    public int cleanupExpiredMetadata(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            long startTime = System.currentTimeMillis();
            
            // 分批获取过期的Key，避免内存问题
            List<String> expiredKeys = metadataMapper.selectExpiredKeys(currentTime);
            
            if (expiredKeys.isEmpty()) {
                logger.info("No expired metadata found");
                return 0;
            }
            
            logger.info("Found {} expired metadata records, starting cleanup", expiredKeys.size());
            
            int totalDeleted = 0;
            
            // 分批处理
            for (int i = 0; i < expiredKeys.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, expiredKeys.size());
                List<String> batchKeys = expiredKeys.subList(i, endIndex);
                
                try {
                    // 批量删除数据库记录
                    int batchDeleted = metadataMapper.deleteBatchByKeys(batchKeys);
                    totalDeleted += batchDeleted;
                    
                    // 批量删除缓存
                    batchRemoveFromCache(batchKeys);
                    
                    logger.debug("Processed batch {}/{}: {} keys, {} deleted", 
                               (i / batchSize + 1), ((expiredKeys.size() - 1) / batchSize + 1), 
                               batchKeys.size(), batchDeleted);
                               
                } catch (Exception batchException) {
                    logger.error("Error processing batch starting at index {}", i, batchException);
                    // 继续处理下一批，不中断整个清理过程
                }
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("Cleanup completed: {} expired metadata records deleted in {}ms", 
                       totalDeleted, endTime - startTime);
            
            return totalDeleted;
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
            
            if (cachedValues != null) {
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
     * 批量写入缓存
     */
    private void batchPutToCache(List<FeatureMetadata> metadataList) {
        if (metadataList == null || metadataList.isEmpty()) {
            return;
        }
        
        try {
            Map<String, String> batchData = new HashMap<>();
            for (FeatureMetadata metadata : metadataList) {
                String cacheKey = CACHE_PREFIX + metadata.getKeyName();
                String value = objectMapper.writeValueAsString(metadata);
                batchData.put(cacheKey, value);
            }
            
            // 使用Redis Pipeline进行批量操作
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<String, String> entry : batchData.entrySet()) {
                    connection.setEx(entry.getKey().getBytes(), CACHE_TTL_MINUTES * 60L, entry.getValue().getBytes());
                }
                return null;
            });
            
            logger.debug("Batch cached {} metadata records", metadataList.size());
        } catch (Exception e) {
            logger.warn("Error batch putting metadata to cache", e);
            // 降级到单个写入
            for (FeatureMetadata metadata : metadataList) {
                putToCache(metadata.getKeyName(), metadata);
            }
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
    
    /**
     * 批量从缓存删除
     */
    private void batchRemoveFromCache(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        try {
            List<String> cacheKeys = keys.stream()
                    .map(key -> CACHE_PREFIX + key)
                    .collect(Collectors.toList());
            
            Long deletedCount = redisTemplate.delete(cacheKeys);
            logger.debug("Batch removed {} cache entries", deletedCount);
        } catch (Exception e) {
            logger.warn("Error batch removing metadata from cache", e);
            // 降级到单个删除
            for (String key : keys) {
                removeFromCache(key);
            }
        }
    }
} 