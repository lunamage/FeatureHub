package com.featurehub.query.service;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.common.domain.QueryLog;
import com.featurehub.common.domain.StorageType;
import com.featurehub.common.dto.FeatureQueryRequest;
import com.featurehub.common.dto.FeatureQueryResponse;
import com.featurehub.query.client.KeeWiDbClient;
import com.featurehub.query.client.MetadataServiceClient;
import com.featurehub.query.client.RedisClient;
import com.featurehub.query.publisher.QueryLogPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 特征查询服务核心业务逻辑
 * 负责特征数据的查询、路由和日志记录
 */
@Service
public class FeatureQueryService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureQueryService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private KeeWiDbClient keeWiDbClient;

    @Autowired
    private MetadataServiceClient metadataServiceClient;

    @Autowired
    private QueryLogPublisher queryLogPublisher;

    // 监控指标
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong redisRequests = new AtomicLong(0);
    private final AtomicLong keewidbRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    /**
     * 查询单个特征
     */
    public FeatureQueryResponse queryFeature(FeatureQueryRequest request) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        try {
            String key = request.getKey();
            
            // 1. 查询元数据，确定存储位置
            FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
            if (metadata == null) {
                // 如果元数据不存在，默认从Redis查询
                metadata = new FeatureMetadata(key);
                metadata.setStorageType(StorageType.REDIS);
            }

            // 2. 根据存储位置查询数据
            FeatureQueryResponse.FeatureResult result = queryFromStorage(key, metadata.getStorageType());
            result.setQueryTimeMs(System.currentTimeMillis() - startTime);

            // 3. 记录查询日志
            recordQueryLog(key, metadata.getStorageType(), result, request.getOptions());

            // 4. 异步更新元数据访问信息
            updateMetadataAsync(key, metadata);

            if (result.isFound()) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }

            return new FeatureQueryResponse(result);
            
        } catch (Exception e) {
            logger.error("Error querying feature: {}", request.getKey(), e);
            failedRequests.incrementAndGet();
            
            FeatureQueryResponse.FeatureResult errorResult = 
                new FeatureQueryResponse.FeatureResult(request.getKey(), e.getMessage());
            errorResult.setQueryTimeMs(System.currentTimeMillis() - startTime);
            
            return new FeatureQueryResponse(errorResult);
        }
    }

    /**
     * 批量查询特征
     */
    public FeatureQueryResponse queryBatchFeatures(FeatureQueryRequest request) {
        long startTime = System.currentTimeMillis();
        List<String> keys = request.getKeys();
        totalRequests.addAndGet(keys.size());

        try {
            // 1. 批量查询元数据
            Map<String, FeatureMetadata> metadataMap = metadataServiceClient.getBatchMetadata(keys);

            // 2. 按存储类型分组
            Map<StorageType, List<String>> storageGroups = groupKeysByStorage(keys, metadataMap);

            // 3. 并行查询不同存储
            List<CompletableFuture<Map<String, FeatureQueryResponse.FeatureResult>>> futures = new ArrayList<>();
            
            if (storageGroups.containsKey(StorageType.REDIS)) {
                futures.add(CompletableFuture.supplyAsync(() -> 
                    queryBatchFromRedis(storageGroups.get(StorageType.REDIS))));
            }
            
            if (storageGroups.containsKey(StorageType.KEEWIDB)) {
                futures.add(CompletableFuture.supplyAsync(() -> 
                    queryBatchFromKeeWiDb(storageGroups.get(StorageType.KEEWIDB))));
            }

            // 4. 合并结果
            Map<String, FeatureQueryResponse.FeatureResult> allResults = new HashMap<>();
            for (CompletableFuture<Map<String, FeatureQueryResponse.FeatureResult>> future : futures) {
                try {
                    allResults.putAll(future.get());
                } catch (Exception e) {
                    throw new RuntimeException("Error in concurrent query execution", e);
                }
            }

            // 5. 构建响应结果
            List<FeatureQueryResponse.FeatureResult> results = new ArrayList<>();
            int found = 0, notFound = 0, redisHits = 0, keewidbHits = 0;

            for (String key : keys) {
                FeatureQueryResponse.FeatureResult result = allResults.get(key);
                if (result == null) {
                    result = new FeatureQueryResponse.FeatureResult(key, "Key not found in any storage");
                }
                results.add(result);

                if (result.isFound()) {
                    found++;
                    if (result.getSource() == StorageType.REDIS) {
                        redisHits++;
                    } else if (result.getSource() == StorageType.KEEWIDB) {
                        keewidbHits++;
                    }
                } else {
                    notFound++;
                }

                // 记录查询日志
                recordQueryLog(key, result.getSource(), result, request.getOptions());
                
                // 异步更新元数据
                FeatureMetadata metadata = metadataMap.get(key);
                if (metadata != null) {
                    updateMetadataAsync(key, metadata);
                }
            }

            long totalQueryTime = System.currentTimeMillis() - startTime;
            FeatureQueryResponse.QuerySummary summary = new FeatureQueryResponse.QuerySummary(
                keys.size(), found, notFound, redisHits, keewidbHits, totalQueryTime);

            successfulRequests.addAndGet(found);
            failedRequests.addAndGet(notFound);

            return new FeatureQueryResponse(results, summary);
            
        } catch (Exception e) {
            logger.error("Error in batch query", e);
            failedRequests.addAndGet(keys.size());
            throw e;
        }
    }

    /**
     * 写入特征数据
     */
    public Map<String, Object> putFeature(String key, String value, Long ttl, String storageHint) {
        try {
            // 默认写入Redis（热数据）
            StorageType targetStorage = StorageType.REDIS;
            if ("cold".equals(storageHint)) {
                targetStorage = StorageType.KEEWIDB;
            }

            boolean success;
            if (targetStorage == StorageType.REDIS) {
                success = redisClient.set(key, value, ttl);
            } else {
                success = keeWiDbClient.set(key, value, ttl);
            }

            if (success) {
                // 创建或更新元数据
                FeatureMetadata metadata = new FeatureMetadata(key);
                metadata.setStorageType(targetStorage);
                metadata.setDataSize((long) value.getBytes().length);
                if (ttl != null) {
                    metadata.setExpireTime(System.currentTimeMillis() + ttl * 1000);
                }
                
                metadataServiceClient.upsertMetadata(metadata);

                Map<String, Object> result = new HashMap<>();
                result.put("key", key);
                result.put("value", value);
                result.put("storage", targetStorage.getValue());
                result.put("created", true);
                if (ttl != null) {
                    result.put("ttl", ttl);
                }
                return result;
            } else {
                throw new RuntimeException("Failed to write to storage");
            }
            
        } catch (Exception e) {
            logger.error("Error putting feature: {}", key, e);
            throw e;
        }
    }

    /**
     * 获取健康状态信息
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("redis", redisClient.isHealthy() ? "healthy" : "unhealthy");
        dependencies.put("keewidb", keeWiDbClient.isHealthy() ? "healthy" : "unhealthy");
        dependencies.put("metadata_service", metadataServiceClient.isHealthy() ? "healthy" : "unhealthy");
        dependencies.put("kafka", queryLogPublisher.isHealthy() ? "healthy" : "unhealthy");
        
        health.put("dependencies", dependencies);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_requests", totalRequests.get());
        metrics.put("redis_requests", redisRequests.get());
        metrics.put("keewidb_requests", keewidbRequests.get());
        metrics.put("successful_requests", successfulRequests.get());
        metrics.put("failed_requests", failedRequests.get());
        
        health.put("metrics", metrics);

        return health;
    }

    /**
     * 获取监控指标
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_requests", totalRequests.get());
        metrics.put("redis_requests", redisRequests.get());
        metrics.put("keewidb_requests", keewidbRequests.get());
        metrics.put("successful_requests", successfulRequests.get());
        metrics.put("failed_requests", failedRequests.get());
        
        double successRate = totalRequests.get() > 0 ? 
            (double) successfulRequests.get() / totalRequests.get() * 100 : 0;
        metrics.put("success_rate_percent", successRate);
        
        return metrics;
    }

    /**
     * 从指定存储查询数据
     */
    private FeatureQueryResponse.FeatureResult queryFromStorage(String key, StorageType storageType) {
        try {
            String value;
            if (storageType == StorageType.REDIS) {
                redisRequests.incrementAndGet();
                value = redisClient.get(key);
            } else {
                keewidbRequests.incrementAndGet();
                value = keeWiDbClient.get(key);
            }

            if (value != null) {
                return new FeatureQueryResponse.FeatureResult(key, value, storageType);
            } else {
                return new FeatureQueryResponse.FeatureResult(key, "Key not found");
            }
            
        } catch (Exception e) {
            logger.error("Error querying from storage {}: {}", storageType, key, e);
            return new FeatureQueryResponse.FeatureResult(key, e.getMessage());
        }
    }

    /**
     * 按存储类型分组Key
     */
    private Map<StorageType, List<String>> groupKeysByStorage(List<String> keys, 
                                                             Map<String, FeatureMetadata> metadataMap) {
        Map<StorageType, List<String>> groups = new HashMap<>();
        
        for (String key : keys) {
            StorageType storageType = StorageType.REDIS; // 默认Redis
            FeatureMetadata metadata = metadataMap.get(key);
            if (metadata != null) {
                storageType = metadata.getStorageType();
            }
            
            groups.computeIfAbsent(storageType, k -> new ArrayList<>()).add(key);
        }
        
        return groups;
    }

    /**
     * 从Redis批量查询
     */
    private Map<String, FeatureQueryResponse.FeatureResult> queryBatchFromRedis(List<String> keys) {
        Map<String, FeatureQueryResponse.FeatureResult> results = new HashMap<>();
        try {
            Map<String, String> values = redisClient.mget(keys);
            for (String key : keys) {
                String value = values.get(key);
                if (value != null) {
                    results.put(key, new FeatureQueryResponse.FeatureResult(key, value, StorageType.REDIS));
                } else {
                    results.put(key, new FeatureQueryResponse.FeatureResult(key, "Key not found in Redis"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in batch Redis query", e);
            for (String key : keys) {
                results.put(key, new FeatureQueryResponse.FeatureResult(key, e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 从KeeWiDb批量查询
     */
    private Map<String, FeatureQueryResponse.FeatureResult> queryBatchFromKeeWiDb(List<String> keys) {
        Map<String, FeatureQueryResponse.FeatureResult> results = new HashMap<>();
        try {
            Map<String, String> values = keeWiDbClient.mget(keys);
            for (String key : keys) {
                String value = values.get(key);
                if (value != null) {
                    results.put(key, new FeatureQueryResponse.FeatureResult(key, value, StorageType.KEEWIDB));
                } else {
                    results.put(key, new FeatureQueryResponse.FeatureResult(key, "Key not found in KeeWiDb"));
                }
            }
        } catch (Exception e) {
            logger.error("Error in batch KeeWiDb query", e);
            for (String key : keys) {
                results.put(key, new FeatureQueryResponse.FeatureResult(key, e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 记录查询日志
     */
    private void recordQueryLog(String key, StorageType sourceStorage, 
                               FeatureQueryResponse.FeatureResult result,
                               FeatureQueryRequest.QueryOptions options) {
        try {
            QueryLog queryLog = new QueryLog(key, sourceStorage);
            queryLog.setSuccess(result.isFound());
            queryLog.setQueryTimeMs(result.getQueryTimeMs());
            queryLog.setClientIp(options.getClientIp());
            queryLog.setUserId(options.getUserId());
            
            if (!result.isFound()) {
                queryLog.setErrorMessage(result.getError());
            }
            
            queryLogPublisher.publish(queryLog);
            
        } catch (Exception e) {
            logger.warn("Failed to record query log for key: {}", key, e);
        }
    }

    /**
     * 异步更新元数据访问信息
     */
    private void updateMetadataAsync(String key, FeatureMetadata metadata) {
        CompletableFuture.runAsync(() -> {
            try {
                metadata.incrementAccessCount();
                metadataServiceClient.updateMetadata(metadata);
            } catch (Exception e) {
                logger.warn("Failed to update metadata for key: {}", key, e);
            }
        });
    }
} 