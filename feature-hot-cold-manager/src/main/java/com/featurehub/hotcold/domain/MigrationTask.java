package com.featurehub.hotcold.domain;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 迁移任务实体
 */
public class MigrationTask {
    @NotEmpty(message = "任务类型不能为空")
    private String taskType; // HOT_TO_COLD, COLD_TO_HOT
    
    @NotEmpty(message = "源存储类型不能为空")
    private String sourceStorage;
    
    @NotEmpty(message = "目标存储类型不能为空")
    private String targetStorage;
    
    private List<String> keys;
    private String businessTag;
    private boolean async = true;
    
    // 构造函数
    public MigrationTask() {}
    
    public MigrationTask(String taskType, String sourceStorage, String targetStorage) {
        this.taskType = taskType;
        this.sourceStorage = sourceStorage;
        this.targetStorage = targetStorage;
    }
    
    // Getter和Setter方法
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public String getSourceStorage() { return sourceStorage; }
    public void setSourceStorage(String sourceStorage) { this.sourceStorage = sourceStorage; }
    
    public String getTargetStorage() { return targetStorage; }
    public void setTargetStorage(String targetStorage) { this.targetStorage = targetStorage; }
    
    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }
    
    public String getBusinessTag() { return businessTag; }
    public void setBusinessTag(String businessTag) { this.businessTag = businessTag; }
    
    public boolean isAsync() { return async; }
    public void setAsync(boolean async) { this.async = async; }
    
    @Override
    public String toString() {
        return "MigrationTask{" +
                "taskType='" + taskType + '\'' +
                ", sourceStorage='" + sourceStorage + '\'' +
                ", targetStorage='" + targetStorage + '\'' +
                ", keysCount=" + (keys != null ? keys.size() : 0) +
                ", businessTag='" + businessTag + '\'' +
                ", async=" + async +
                '}';
    }
} 