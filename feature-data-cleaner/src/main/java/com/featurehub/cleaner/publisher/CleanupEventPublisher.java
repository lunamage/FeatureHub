package com.featurehub.cleaner.publisher;

import com.featurehub.cleaner.domain.CleanupRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CleanupEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CleanupEventPublisher.class);

    public void publishCleanupResult(CleanupRecord record) {
        // TODO: 发布清理结果到Kafka
        logger.info("发布清理结果: {}", record.getTaskId());
    }

    public void publishDataCleaned(String key, String storageType, String cleanupType) {
        // TODO: 发布数据清理事件到Kafka
        logger.debug("发布数据清理事件: key={}, storage={}, type={}", key, storageType, cleanupType);
    }
} 