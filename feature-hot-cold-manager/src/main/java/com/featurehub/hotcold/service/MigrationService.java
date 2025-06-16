package com.featurehub.hotcold.service;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.common.domain.MigrationStatus;
import com.featurehub.common.domain.StorageType;
import com.featurehub.hotcold.client.KeeWiDbClient;
import com.featurehub.hotcold.client.MetadataServiceClient;
import com.featurehub.hotcold.client.RedisClient;
import com.featurehub.hotcold.config.MigrationConfig;
import com.featurehub.hotcold.domain.MigrationRecord;
import com.featurehub.hotcold.domain.MigrationTask;
import com.featurehub.hotcold.publisher.MigrationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 数据迁移核心服务
 * 负责冷热数据之间的智能迁移策略
 */
@Service
public class MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private KeeWiDbClient keeWiDbClient;

    @Autowired
    private MetadataServiceClient metadataServiceClient;

    @Autowired
    private MigrationEventPublisher eventPublisher;

    @Autowired
    private MigrationConfig migrationConfig;

    /**
     * 定时执行热转冷迁移任务
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void scheduleHotToColdMigration() {
        logger.info("开始执行热转冷迁移任务");
        
        try {
            // 1. 查找需要从热存储迁移到冷存储的数据
            List<FeatureMetadata> hotDataForMigration = findHotDataForMigration();
            
            if (CollectionUtils.isEmpty(hotDataForMigration)) {
                logger.info("没有需要执行热转冷迁移的数据");
                return;
            }
            
            logger.info("找到 {} 个需要热转冷迁移的特征Key", hotDataForMigration.size());
            
            // 2. 分批执行迁移
            List<List<FeatureMetadata>> batches = partitionList(hotDataForMigration, 
                    migrationConfig.getBatchSize());
            
            int totalBatches = batches.size();
            int completedBatches = 0;
            
            for (List<FeatureMetadata> batch : batches) {
                CompletableFuture<Void> future = executeHotToColdMigrationBatch(batch);
                future.join(); // 等待当前批次完成
                
                completedBatches++;
                logger.info("热转冷迁移进度: {}/{} 批次完成", completedBatches, totalBatches);
                
                // 批次间暂停，避免对系统造成过大压力
                if (completedBatches < totalBatches) {
                    Thread.sleep(migrationConfig.getBatchIntervalMs());
                }
            }
            
            logger.info("热转冷迁移任务完成，共迁移 {} 个特征Key", hotDataForMigration.size());
            
        } catch (Exception e) {
            logger.error("热转冷迁移任务执行失败", e);
        }
    }

    /**
     * 定时执行冷转热召回任务
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    public void scheduleColdToHotRecall() {
        logger.info("开始执行冷转热召回任务");
        
        try {
            // 1. 查找需要从冷存储召回到热存储的数据
            List<FeatureMetadata> coldDataForRecall = findColdDataForRecall();
            
            if (CollectionUtils.isEmpty(coldDataForRecall)) {
                logger.info("没有需要执行冷转热召回的数据");
                return;
            }
            
            logger.info("找到 {} 个需要冷转热召回的特征Key", coldDataForRecall.size());
            
            // 2. 分批执行召回
            List<List<FeatureMetadata>> batches = partitionList(coldDataForRecall, 
                    migrationConfig.getBatchSize());
            
            int totalBatches = batches.size();
            int completedBatches = 0;
            
            for (List<FeatureMetadata> batch : batches) {
                CompletableFuture<Void> future = executeColdToHotRecallBatch(batch);
                future.join(); // 等待当前批次完成
                
                completedBatches++;
                logger.info("冷转热召回进度: {}/{} 批次完成", completedBatches, totalBatches);
                
                // 批次间暂停
                if (completedBatches < totalBatches) {
                    Thread.sleep(migrationConfig.getBatchIntervalMs());
                }
            }
            
            logger.info("冷转热召回任务完成，共召回 {} 个特征Key", coldDataForRecall.size());
            
        } catch (Exception e) {
            logger.error("冷转热召回任务执行失败", e);
        }
    }

    /**
     * 手动触发迁移任务
     */
    public MigrationRecord triggerMigration(MigrationTask task) {
        logger.info("手动触发迁移任务: {}", task);
        
        MigrationRecord record = new MigrationRecord(task.getTaskType());
        record.setId(UUID.randomUUID().toString());
        record.setTotalKeys(task.getKeys() != null ? task.getKeys().size() : 0);
        
        try {
            // 根据迁移方向执行不同逻辑
            if ("HOT_TO_COLD".equals(task.getTaskType())) {
                // 热转冷
                record = executeHotToColdMigration(task.getKeys(), record);
            } else if ("COLD_TO_HOT".equals(task.getTaskType())) {
                // 冷转热
                record = executeColdToHotRecall(task.getKeys(), record);
            } else {
                throw new IllegalArgumentException("不支持的迁移类型: " + task.getTaskType());
            }
            
            record.setStatus("COMPLETED");
            record.setEndTime(java.time.LocalDateTime.now());
            
            logger.info("手动迁移任务完成: {}", record.getId());
            
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setEndTime(java.time.LocalDateTime.now());
            record.setErrorMessage(e.getMessage());
            
            logger.error("手动迁移任务失败: {}", record.getId(), e);
        }
        
        return record;
    }

    /**
     * 查找需要从热存储迁移到冷存储的数据
     */
    private List<FeatureMetadata> findHotDataForMigration() {
        try {
            // 计算访问时间阈值（当前时间 - 冷迁移天数）
            long accessTimeThreshold = System.currentTimeMillis() - 
                (migrationConfig.getHotToColdDays() * 24 * 60 * 60 * 1000L);
            
            // 从元数据服务获取需要迁移的热数据
            return metadataServiceClient.getDataForMigration(
                StorageType.REDIS, 
                accessTimeThreshold, 
                migrationConfig.getMaxMigrationSize()
            );
        } catch (Exception e) {
            logger.error("查找热数据迁移候选失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查找需要从冷存储召回到热存储的数据
     */
    private List<FeatureMetadata> findColdDataForRecall() {
        try {
            // 计算访问时间阈值（最近召回天数内有访问的数据）
            long recentAccessTime = System.currentTimeMillis() - 
                (migrationConfig.getColdToHotDays() * 24 * 60 * 60 * 1000L);
            
            // 从元数据服务获取需要召回的冷数据
            return metadataServiceClient.getDataForRecall(
                StorageType.KEEWIDB,
                migrationConfig.getAccessCountThreshold(),
                recentAccessTime,
                migrationConfig.getMaxRecallSize()
            );
        } catch (Exception e) {
            logger.error("查找冷数据召回候选失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 异步执行热转冷迁移批次
     */
    @Async
    public CompletableFuture<Void> executeHotToColdMigrationBatch(List<FeatureMetadata> batch) {
        return CompletableFuture.runAsync(() -> {
            List<String> keys = batch.stream()
                    .map(FeatureMetadata::getKeyName)
                    .collect(Collectors.toList());
            
            executeHotToColdMigration(keys, null);
        });
    }

    /**
     * 异步执行冷转热召回批次
     */
    @Async
    public CompletableFuture<Void> executeColdToHotRecallBatch(List<FeatureMetadata> batch) {
        return CompletableFuture.runAsync(() -> {
            List<String> keys = batch.stream()
                    .map(FeatureMetadata::getKeyName)
                    .collect(Collectors.toList());
            
            executeColdToHotRecall(keys, null);
        });
    }

    /**
     * 执行热转冷迁移
     */
    private MigrationRecord executeHotToColdMigration(List<String> keys, MigrationRecord record) {
        logger.info("开始执行热转冷迁移，Key数量: {}", keys.size());
        
        int successCount = 0;
        int failCount = 0;
        List<String> failedKeys = new ArrayList<>();
        
        for (String key : keys) {
            try {
                // 1. 更新元数据状态为迁移中
                updateMigrationStatus(key, MigrationStatus.MIGRATING);
                
                // 2. 从Redis读取数据
                String value = redisClient.get(key);
                if (value == null) {
                    logger.warn("Redis中不存在Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    continue;
                }
                
                // 3. 写入KeeWiDB
                boolean writeSuccess = keeWiDbClient.set(key, value);
                if (!writeSuccess) {
                    logger.error("写入KeeWiDB失败，Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    updateMigrationStatus(key, MigrationStatus.FAILED);
                    continue;
                }
                
                // 4. 验证数据一致性
                String verifyValue = keeWiDbClient.get(key);
                if (!value.equals(verifyValue)) {
                    logger.error("数据一致性验证失败，Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    updateMigrationStatus(key, MigrationStatus.FAILED);
                    continue;
                }
                
                // 5. 从Redis删除数据
                redisClient.delete(key);
                
                // 6. 更新元数据：存储类型改为KeeWiDB，状态改为稳定
                updateMetadataAfterMigration(key, StorageType.KEEWIDB, MigrationStatus.STABLE);
                
                successCount++;
                logger.debug("热转冷迁移成功，Key: {}", key);
                
                // 7. 发送迁移成功事件
                eventPublisher.publishMigrationSuccess(key, StorageType.REDIS, StorageType.KEEWIDB);
                
            } catch (Exception e) {
                logger.error("热转冷迁移失败，Key: {}", key, e);
                failedKeys.add(key);
                failCount++;
                updateMigrationStatus(key, MigrationStatus.FAILED);
                
                // 发送迁移失败事件
                eventPublisher.publishMigrationFailure(key, StorageType.REDIS, StorageType.KEEWIDB, e.getMessage());
            }
        }
        
        logger.info("热转冷迁移完成，成功: {}, 失败: {}", successCount, failCount);
        
        if (record != null) {
            record.setSuccessCount(successCount);
            record.setFailCount(failCount);
            record.setFailedKeys(failedKeys);
        }
        
        return record;
    }

    /**
     * 执行冷转热召回
     */
    private MigrationRecord executeColdToHotRecall(List<String> keys, MigrationRecord record) {
        logger.info("开始执行冷转热召回，Key数量: {}", keys.size());
        
        int successCount = 0;
        int failCount = 0;
        List<String> failedKeys = new ArrayList<>();
        
        for (String key : keys) {
            try {
                // 1. 更新元数据状态为迁移中
                updateMigrationStatus(key, MigrationStatus.MIGRATING);
                
                // 2. 从KeeWiDB读取数据
                String value = keeWiDbClient.get(key);
                if (value == null) {
                    logger.warn("KeeWiDB中不存在Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    continue;
                }
                
                // 3. 写入Redis
                boolean writeSuccess = redisClient.set(key, value);
                if (!writeSuccess) {
                    logger.error("写入Redis失败，Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    updateMigrationStatus(key, MigrationStatus.FAILED);
                    continue;
                }
                
                // 4. 验证数据一致性
                String verifyValue = redisClient.get(key);
                if (!value.equals(verifyValue)) {
                    logger.error("数据一致性验证失败，Key: {}", key);
                    failedKeys.add(key);
                    failCount++;
                    updateMigrationStatus(key, MigrationStatus.FAILED);
                    continue;
                }
                
                // 5. 从KeeWiDB删除数据
                keeWiDbClient.delete(key);
                
                // 6. 更新元数据：存储类型改为Redis，状态改为稳定
                updateMetadataAfterMigration(key, StorageType.REDIS, MigrationStatus.STABLE);
                
                successCount++;
                logger.debug("冷转热召回成功，Key: {}", key);
                
                // 7. 发送召回成功事件
                eventPublisher.publishMigrationSuccess(key, StorageType.KEEWIDB, StorageType.REDIS);
                
            } catch (Exception e) {
                logger.error("冷转热召回失败，Key: {}", key, e);
                failedKeys.add(key);
                failCount++;
                updateMigrationStatus(key, MigrationStatus.FAILED);
                
                // 发送召回失败事件
                eventPublisher.publishMigrationFailure(key, StorageType.KEEWIDB, StorageType.REDIS, e.getMessage());
            }
        }
        
        logger.info("冷转热召回完成，成功: {}, 失败: {}", successCount, failCount);
        
        if (record != null) {
            record.setSuccessCount(successCount);
            record.setFailCount(failCount);
            record.setFailedKeys(failedKeys);
        }
        
        return record;
    }

    /**
     * 更新迁移状态
     */
    private void updateMigrationStatus(String key, MigrationStatus status) {
        try {
            FeatureMetadata metadata = new FeatureMetadata();
            metadata.setKeyName(key);
            metadata.setMigrationStatus(status);
            metadata.setMigrationTime(System.currentTimeMillis());
            
            metadataServiceClient.updateMetadata(metadata);
        } catch (Exception e) {
            logger.error("更新迁移状态失败，Key: {}, Status: {}", key, status, e);
        }
    }

    /**
     * 迁移完成后更新元数据
     */
    private void updateMetadataAfterMigration(String key, StorageType newStorageType, MigrationStatus status) {
        try {
            FeatureMetadata metadata = new FeatureMetadata();
            metadata.setKeyName(key);
            metadata.setStorageType(newStorageType);
            metadata.setMigrationStatus(status);
            metadata.setMigrationTime(System.currentTimeMillis());
            metadata.setUpdateTime(System.currentTimeMillis());
            
            metadataServiceClient.updateMetadata(metadata);
        } catch (Exception e) {
            logger.error("迁移后更新元数据失败，Key: {}, StorageType: {}", key, newStorageType, e);
        }
    }

    /**
     * 列表分片工具方法
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
}