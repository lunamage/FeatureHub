package com.featurehub.cleaner.service;

import com.featurehub.cleaner.client.MetadataServiceClient;
import com.featurehub.cleaner.client.RedisClient;
import com.featurehub.cleaner.client.KeeWiDbClient;
import com.featurehub.cleaner.config.CleanerConfig;
import com.featurehub.cleaner.domain.CleanupRecord;
import com.featurehub.cleaner.publisher.CleanupEventPublisher;
import com.featurehub.common.domain.FeatureMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据清理核心服务
 * 负责清理过期、无效和冗余的特征数据
 */
@Service
public class DataCleanerService {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanerService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private KeeWiDbClient keeWiDbClient;

    @Autowired
    private MetadataServiceClient metadataServiceClient;

    @Autowired
    private CleanupEventPublisher eventPublisher;

    @Autowired
    private CleanerConfig cleanerConfig;

    /**
     * 定时清理过期数据
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleExpiredDataCleanup() {
        logger.info("开始执行过期数据清理任务");
        
        CleanupRecord record = new CleanupRecord();
        record.setTaskId(UUID.randomUUID().toString());
        record.setCleanupType("EXPIRED_DATA");
        record.setStartTime(System.currentTimeMillis());
        
        try {
            // 1. 从元数据服务获取过期数据列表
            List<String> expiredKeys = metadataServiceClient.getExpiredKeys();
            
            if (CollectionUtils.isEmpty(expiredKeys)) {
                logger.info("没有找到过期数据");
                record.setStatus("COMPLETED");
                record.setEndTime(System.currentTimeMillis());
                return;
            }
            
            logger.info("找到 {} 个过期Key，开始清理", expiredKeys.size());
            
            // 2. 分批清理过期数据
            int batchSize = cleanerConfig.getBatchSize();
            int totalBatches = (expiredKeys.size() + batchSize - 1) / batchSize;
            
            AtomicInteger totalCleaned = new AtomicInteger(0);
            AtomicInteger totalFailed = new AtomicInteger(0);
            
            for (int i = 0; i < totalBatches; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, expiredKeys.size());
                List<String> batch = expiredKeys.subList(start, end);
                
                cleanupExpiredDataBatch(batch, totalCleaned, totalFailed);
                
                logger.info("过期数据清理进度: {}/{} 批次完成", i + 1, totalBatches);
            }
            
            // 3. 清理元数据
            int metadataCleanedCount = metadataServiceClient.cleanupExpiredMetadata();
            
            record.setStatus("COMPLETED");
            record.setCleanedCount(totalCleaned.get());
            record.setFailedCount(totalFailed.get());
            record.setEndTime(System.currentTimeMillis());
            
            logger.info("过期数据清理完成，清理数据: {}, 失败: {}, 元数据清理: {}",
                    totalCleaned.get(), totalFailed.get(), metadataCleanedCount);
            
        } catch (Exception e) {
            logger.error("过期数据清理任务失败", e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setEndTime(System.currentTimeMillis());
        }
        
        // 发送清理完成事件
        eventPublisher.publishCleanupResult(record);
    }

    /**
     * 定时清理孤儿数据
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void scheduleOrphanDataCleanup() {
        logger.info("开始执行孤儿数据清理任务");
        
        CleanupRecord record = new CleanupRecord();
        record.setTaskId(UUID.randomUUID().toString());
        record.setCleanupType("ORPHAN_DATA");
        record.setStartTime(System.currentTimeMillis());
        
        try {
            // 1. 查找Redis中的孤儿数据（有数据但没有元数据）
            List<String> redisOrphanKeys = findRedisOrphanKeys();
            
            // 2. 查找KeeWiDB中的孤儿数据
            List<String> keewidbOrphanKeys = findKeeWiDbOrphanKeys();
            
            int totalOrphanKeys = redisOrphanKeys.size() + keewidbOrphanKeys.size();
            
            if (totalOrphanKeys == 0) {
                logger.info("没有找到孤儿数据");
                record.setStatus("COMPLETED");
                record.setEndTime(System.currentTimeMillis());
                return;
            }
            
            logger.info("找到孤儿数据 - Redis: {}, KeeWiDB: {}", 
                    redisOrphanKeys.size(), keewidbOrphanKeys.size());
            
            AtomicInteger totalCleaned = new AtomicInteger(0);
            AtomicInteger totalFailed = new AtomicInteger(0);
            
            // 3. 清理Redis孤儿数据
            cleanupOrphanDataBatch(redisOrphanKeys, "REDIS", totalCleaned, totalFailed);
            
            // 4. 清理KeeWiDB孤儿数据
            cleanupOrphanDataBatch(keewidbOrphanKeys, "KEEWIDB", totalCleaned, totalFailed);
            
            record.setStatus("COMPLETED");
            record.setCleanedCount(totalCleaned.get());
            record.setFailedCount(totalFailed.get());
            record.setEndTime(System.currentTimeMillis());
            
            logger.info("孤儿数据清理完成，清理: {}, 失败: {}", totalCleaned.get(), totalFailed.get());
            
        } catch (Exception e) {
            logger.error("孤儿数据清理任务失败", e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setEndTime(System.currentTimeMillis());
        }
        
        // 发送清理完成事件
        eventPublisher.publishCleanupResult(record);
    }

    /**
     * 手动触发数据清理
     */
    public CleanupRecord triggerCleanup(String cleanupType, List<String> keys) {
        logger.info("手动触发数据清理，类型: {}, Key数量: {}", cleanupType, keys.size());
        
        CleanupRecord record = new CleanupRecord();
        record.setTaskId(UUID.randomUUID().toString());
        record.setCleanupType(cleanupType);
        record.setStartTime(System.currentTimeMillis());
        
        try {
            AtomicInteger totalCleaned = new AtomicInteger(0);
            AtomicInteger totalFailed = new AtomicInteger(0);
            
            if ("EXPIRED_DATA".equals(cleanupType)) {
                cleanupExpiredDataBatch(keys, totalCleaned, totalFailed);
            } else if ("ORPHAN_DATA".equals(cleanupType)) {
                cleanupOrphanDataBatch(keys, "MIXED", totalCleaned, totalFailed);
            } else {
                throw new IllegalArgumentException("不支持的清理类型: " + cleanupType);
            }
            
            record.setStatus("COMPLETED");
            record.setCleanedCount(totalCleaned.get());
            record.setFailedCount(totalFailed.get());
            record.setEndTime(System.currentTimeMillis());
            
            logger.info("手动清理完成，清理: {}, 失败: {}", totalCleaned.get(), totalFailed.get());
            
        } catch (Exception e) {
            logger.error("手动清理失败", e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setEndTime(System.currentTimeMillis());
        }
        
        return record;
    }

    /**
     * 清理过期数据批次
     */
    private void cleanupExpiredDataBatch(List<String> keys, AtomicInteger totalCleaned, AtomicInteger totalFailed) {
        for (String key : keys) {
            try {
                // 获取元数据确定存储位置
                FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
                if (metadata == null) {
                    logger.warn("元数据不存在，跳过Key: {}", key);
                    continue;
                }
                
                boolean cleaned = false;
                
                // 根据存储类型清理数据
                switch (metadata.getStorageType()) {
                    case REDIS:
                        cleaned = redisClient.delete(key);
                        break;
                    case KEEWIDB:
                        cleaned = keeWiDbClient.delete(key);
                        break;
                    default:
                        logger.warn("未知存储类型: {}, Key: {}", metadata.getStorageType(), key);
                        break;
                }
                
                if (cleaned) {
                    totalCleaned.incrementAndGet();
                    logger.debug("成功清理过期数据，Key: {}", key);
                    
                    // 发送清理成功事件
                    eventPublisher.publishDataCleaned(key, metadata.getStorageType().name(), "EXPIRED");
                } else {
                    totalFailed.incrementAndGet();
                    logger.warn("清理过期数据失败，Key: {}", key);
                }
                
            } catch (Exception e) {
                totalFailed.incrementAndGet();
                logger.error("清理过期数据异常，Key: {}", key, e);
            }
        }
    }

    /**
     * 清理孤儿数据批次
     */
    private void cleanupOrphanDataBatch(List<String> keys, String storageType, 
                                      AtomicInteger totalCleaned, AtomicInteger totalFailed) {
        for (String key : keys) {
            try {
                boolean cleaned = false;
                
                if ("REDIS".equals(storageType)) {
                    cleaned = redisClient.delete(key);
                } else if ("KEEWIDB".equals(storageType)) {
                    cleaned = keeWiDbClient.delete(key);
                } else {
                    // 混合模式，尝试从两个存储中删除
                    boolean redisDeleted = redisClient.delete(key);
                    boolean keewidbDeleted = keeWiDbClient.delete(key);
                    cleaned = redisDeleted || keewidbDeleted;
                }
                
                if (cleaned) {
                    totalCleaned.incrementAndGet();
                    logger.debug("成功清理孤儿数据，Key: {}, Storage: {}", key, storageType);
                    
                    // 发送清理成功事件
                    eventPublisher.publishDataCleaned(key, storageType, "ORPHAN");
                } else {
                    totalFailed.incrementAndGet();
                    logger.warn("清理孤儿数据失败，Key: {}, Storage: {}", key, storageType);
                }
                
            } catch (Exception e) {
                totalFailed.incrementAndGet();
                logger.error("清理孤儿数据异常，Key: {}, Storage: {}", key, storageType, e);
            }
        }
    }

    /**
     * 查找Redis中的孤儿数据
     */
    private List<String> findRedisOrphanKeys() {
        List<String> orphanKeys = new ArrayList<>();
        
        try {
            // 获取Redis中的所有特征Key
            Set<String> redisKeys = redisClient.getAllFeatureKeys();
            
            // 检查每个Key是否在元数据中存在
            for (String key : redisKeys) {
                FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
                if (metadata == null) {
                    orphanKeys.add(key);
                }
            }
            
        } catch (Exception e) {
            logger.error("查找Redis孤儿数据失败", e);
        }
        
        return orphanKeys;
    }

    /**
     * 查找KeeWiDB中的孤儿数据
     */
    private List<String> findKeeWiDbOrphanKeys() {
        List<String> orphanKeys = new ArrayList<>();
        
        try {
            // 获取KeeWiDB中的所有特征Key
            Set<String> keewidbKeys = keeWiDbClient.getAllFeatureKeys();
            
            // 检查每个Key是否在元数据中存在
            for (String key : keewidbKeys) {
                FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
                if (metadata == null) {
                    orphanKeys.add(key);
                }
            }
            
        } catch (Exception e) {
            logger.error("查找KeeWiDB孤儿数据失败", e);
        }
        
        return orphanKeys;
    }

    /**
     * 获取清理统计信息
     */
    public Map<String, Object> getCleanupStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取各种统计数据
            stats.put("redis_key_count", redisClient.getKeyCount());
            stats.put("keewidb_key_count", keeWiDbClient.getKeyCount());
            stats.put("metadata_count", metadataServiceClient.getTotalMetadataCount());
            
            // 计算存储占用
            stats.put("redis_memory_usage", redisClient.getMemoryUsage());
            stats.put("keewidb_memory_usage", keeWiDbClient.getMemoryUsage());
            
            // 最近清理记录
            // TODO: 从数据库或缓存中获取最近的清理记录
            
            stats.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("获取清理统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
} 