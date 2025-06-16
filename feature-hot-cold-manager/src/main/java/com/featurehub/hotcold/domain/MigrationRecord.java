package com.featurehub.hotcold.domain;

import java.time.LocalDateTime;

/**
 * 迁移记录实体
 */
public class MigrationRecord {
    private String id;
    private String taskType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalKeys;
    private int successCount;
    private int failCount;
    private String errorMessage;
    
    // 构造函数
    public MigrationRecord() {}
    
    public MigrationRecord(String taskType) {
        this.taskType = taskType;
        this.startTime = LocalDateTime.now();
        this.status = "RUNNING";
    }
    
    // Getter和Setter方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public int getTotalKeys() { return totalKeys; }
    public void setTotalKeys(int totalKeys) { this.totalKeys = totalKeys; }
    
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // 失败key列表相关方法
    private java.util.List<String> failedKeys = new java.util.ArrayList<>();
    
    public java.util.List<String> getFailedKeys() { return failedKeys; }
    public void setFailedKeys(java.util.List<String> failedKeys) { this.failedKeys = failedKeys; }
} 