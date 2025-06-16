package com.featurehub.common.domain;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 迁移任务实体
 * 描述一个数据迁移任务的配置信息
 */
public class MigrationTask {
    
    @NotNull
    private StorageType sourceStorageType;
    
    @NotNull
    private StorageType targetStorageType;
    
    @NotEmpty
    private List<String> keys;
    
    private String businessTag;
    private boolean dryRun = false; // 是否为试运行模式
    private int priority = 0; // 任务优先级，数字越大优先级越高

    public StorageType getSourceStorageType() { return sourceStorageType; }
    public void setSourceStorageType(StorageType sourceStorageType) { this.sourceStorageType = sourceStorageType; }

    public StorageType getTargetStorageType() { return targetStorageType; }
    public void setTargetStorageType(StorageType targetStorageType) { this.targetStorageType = targetStorageType; }

    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }

    public String getBusinessTag() { return businessTag; }
    public void setBusinessTag(String businessTag) { this.businessTag = businessTag; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "MigrationTask{" +
                "sourceStorageType=" + sourceStorageType +
                ", targetStorageType=" + targetStorageType +
                ", keys=" + keys +
                ", businessTag='" + businessTag + '\'' +
                ", dryRun=" + dryRun +
                ", priority=" + priority +
                '}';
    }
} 