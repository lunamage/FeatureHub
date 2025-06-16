package com.featurehub.hotcold.publisher;

import com.featurehub.common.domain.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MigrationEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationEventPublisher.class);
    
    public void publishMigrationSuccess(String key, StorageType from, StorageType to) {
        logger.info("Migration success: {} from {} to {}", key, from, to);
    }
    
    public void publishMigrationFailure(String key, StorageType from, StorageType to, String error) {
        logger.error("Migration failure: {} from {} to {}, error: {}", key, from, to, error);
    }
} 