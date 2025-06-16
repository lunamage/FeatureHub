package com.featurehub.common.domain;

import java.util.List;

/**
 * 迁移记录实体
 * 记录每次数据迁移的详细信息
 */
public class MigrationRecord {
    
    private String taskId;
    private String status; // RUNNING, COMPLETED, FAILED
    private StorageType sourceStorageType;
    private StorageType targetStorageType;
    private List<String> keys;
    private long startTime;
    private long endTime;
    private int successCount;
    private int failCount;
    private List<String> failedKeys;
    private String errorMessage;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public StorageType getSourceStorageType() { return sourceStorageType; }
    public void setSourceStorageType(StorageType sourceStorageType) { this.sourceStorageType = sourceStorageType; }

    public StorageType getTargetStorageType() { return targetStorageType; }
    public void setTargetStorageType(StorageType targetStorageType) { this.targetStorageType = targetStorageType; }

    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public List<String> getFailedKeys() { return failedKeys; }
    public void setFailedKeys(List<String> failedKeys) { this.failedKeys = failedKeys; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
} 