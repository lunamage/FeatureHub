package com.featurehub.hotcold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "migration")
public class MigrationConfig {
    
    private int batchSize = 100;
    private long batchIntervalMs = 1000;
    private int hotToColdDays = 7;
    private int coldToHotDays = 1;
    private int maxMigrationSize = 10000;
    private int maxRecallSize = 1000;
    private int accessCountThreshold = 10;
    
    // Getters and setters
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public long getBatchIntervalMs() { return batchIntervalMs; }
    public void setBatchIntervalMs(long batchIntervalMs) { this.batchIntervalMs = batchIntervalMs; }
    
    public int getHotToColdDays() { return hotToColdDays; }
    public void setHotToColdDays(int hotToColdDays) { this.hotToColdDays = hotToColdDays; }
    
    public int getColdToHotDays() { return coldToHotDays; }
    public void setColdToHotDays(int coldToHotDays) { this.coldToHotDays = coldToHotDays; }
    
    public int getMaxMigrationSize() { return maxMigrationSize; }
    public void setMaxMigrationSize(int maxMigrationSize) { this.maxMigrationSize = maxMigrationSize; }
    
    public int getMaxRecallSize() { return maxRecallSize; }
    public void setMaxRecallSize(int maxRecallSize) { this.maxRecallSize = maxRecallSize; }
    
    public int getAccessCountThreshold() { return accessCountThreshold; }
    public void setAccessCountThreshold(int accessCountThreshold) { this.accessCountThreshold = accessCountThreshold; }
} 