package com.featurehub.common.domain;

import java.util.List;

/**
 * 迁移记录实体类
 * <p>
 * 记录每次数据迁移操作的详细信息，包括迁移状态、进度、错误信息等。
 * 用于迁移流程的监控、审计和故障排查。
 * </p>
 * 
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>跟踪迁移任务的执行状态</li>
 *   <li>记录迁移的源和目标存储类型</li>
 *   <li>统计迁移成功和失败的数量</li>
 *   <li>保存失败的特征Key和错误信息</li>
 * </ul>
 * 
 * <h3>迁移状态:</h3>
 * <ul>
 *   <li>RUNNING: 迁移正在进行中</li>
 *   <li>COMPLETED: 迁移已完成</li>
 *   <li>FAILED: 迁移失败</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 创建迁移记录
 * MigrationRecord record = new MigrationRecord();
 * record.setTaskId("task_001");
 * record.setStatus("RUNNING");
 * record.setSourceStorageType(StorageType.REDIS);
 * record.setTargetStorageType(StorageType.KEEWIDB);
 * 
 * // 迁移完成后更新状态
 * record.setStatus("COMPLETED");
 * record.setEndTime(System.currentTimeMillis());
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see StorageType
 * @see MigrationStatus
 */
public class MigrationRecord {
    
    /**
     * 迁移任务ID
     * <p>唯一标识一次迁移任务，通常格式为: "task_" + 时间戳</p>
     */
    private String taskId;
    
    /**
     * 迁移状态
     * <p>可选值: RUNNING, COMPLETED, FAILED</p>
     */
    private String status; // RUNNING, COMPLETED, FAILED
    
    /**
     * 源存储类型
     * <p>数据迁移前的存储位置</p>
     */
    private StorageType sourceStorageType;
    
    /**
     * 目标存储类型  
     * <p>数据迁移后的存储位置</p>
     */
    private StorageType targetStorageType;
    
    /**
     * 待迁移的特征Key列表
     * <p>本次迁移任务涉及的所有特征Key</p>
     */
    private List<String> keys;
    
    /**
     * 迁移开始时间戳（毫秒）
     */
    private long startTime;
    
    /**
     * 迁移结束时间戳（毫秒）
     */
    private long endTime;
    
    /**
     * 成功迁移的数量
     */
    private int successCount;
    
    /**
     * 失败迁移的数量
     */
    private int failCount;
    
    /**
     * 迁移失败的特征Key列表
     * <p>用于重试和故障排查</p>
     */
    private List<String> failedKeys;
    
    /**
     * 错误信息
     * <p>记录迁移过程中遇到的错误描述</p>
     */
    private String errorMessage;

    /**
     * 获取迁移任务ID
     * 
     * @return 任务ID，可能为null
     */
    public String getTaskId() { 
        return taskId; 
    }
    
    /**
     * 设置迁移任务ID
     * 
     * @param taskId 任务ID，建议使用唯一标识符
     */
    public void setTaskId(String taskId) { 
        this.taskId = taskId; 
    }

    /**
     * 获取迁移状态
     * 
     * @return 当前迁移状态，可能为null
     */
    public String getStatus() { 
        return status; 
    }
    
    /**
     * 设置迁移状态
     * 
     * @param status 迁移状态，建议使用标准值: RUNNING, COMPLETED, FAILED
     */
    public void setStatus(String status) { 
        this.status = status; 
    }

    /**
     * 获取源存储类型
     * 
     * @return 迁移前的存储类型
     */
    public StorageType getSourceStorageType() { 
        return sourceStorageType; 
    }
    
    /**
     * 设置源存储类型
     * 
     * @param sourceStorageType 迁移前的存储类型，不应为null
     */
    public void setSourceStorageType(StorageType sourceStorageType) { 
        this.sourceStorageType = sourceStorageType; 
    }

    /**
     * 获取目标存储类型
     * 
     * @return 迁移后的存储类型
     */
    public StorageType getTargetStorageType() { 
        return targetStorageType; 
    }
    
    /**
     * 设置目标存储类型
     * 
     * @param targetStorageType 迁移后的存储类型，不应为null
     */
    public void setTargetStorageType(StorageType targetStorageType) { 
        this.targetStorageType = targetStorageType; 
    }

    /**
     * 获取待迁移的特征Key列表
     * 
     * @return 特征Key列表，可能为null或空列表
     */
    public List<String> getKeys() { 
        return keys; 
    }
    
    /**
     * 设置待迁移的特征Key列表
     * 
     * @param keys 特征Key列表
     */
    public void setKeys(List<String> keys) { 
        this.keys = keys; 
    }

    /**
     * 获取迁移开始时间
     * 
     * @return 开始时间戳（毫秒）
     */
    public long getStartTime() { 
        return startTime; 
    }
    
    /**
     * 设置迁移开始时间
     * 
     * @param startTime 开始时间戳（毫秒）
     */
    public void setStartTime(long startTime) { 
        this.startTime = startTime; 
    }

    /**
     * 获取迁移结束时间
     * 
     * @return 结束时间戳（毫秒）
     */
    public long getEndTime() { 
        return endTime; 
    }
    
    /**
     * 设置迁移结束时间
     * 
     * @param endTime 结束时间戳（毫秒）
     */
    public void setEndTime(long endTime) { 
        this.endTime = endTime; 
    }

    /**
     * 获取成功迁移的数量
     * 
     * @return 成功数量，默认为0
     */
    public int getSuccessCount() { 
        return successCount; 
    }
    
    /**
     * 设置成功迁移的数量
     * 
     * @param successCount 成功数量，应为非负数
     */
    public void setSuccessCount(int successCount) { 
        this.successCount = successCount; 
    }

    /**
     * 获取失败迁移的数量
     * 
     * @return 失败数量，默认为0
     */
    public int getFailCount() { 
        return failCount; 
    }
    
    /**
     * 设置失败迁移的数量
     * 
     * @param failCount 失败数量，应为非负数
     */
    public void setFailCount(int failCount) { 
        this.failCount = failCount; 
    }

    /**
     * 获取迁移失败的特征Key列表
     * 
     * @return 失败的Key列表，可能为null或空列表
     */
    public List<String> getFailedKeys() { 
        return failedKeys; 
    }
    
    /**
     * 设置迁移失败的特征Key列表
     * 
     * @param failedKeys 失败的Key列表，用于重试处理
     */
    public void setFailedKeys(List<String> failedKeys) { 
        this.failedKeys = failedKeys; 
    }

    /**
     * 获取错误信息
     * 
     * @return 错误描述信息，可能为null
     */
    public String getErrorMessage() { 
        return errorMessage; 
    }
    
    /**
     * 设置错误信息
     * 
     * @param errorMessage 错误描述信息，用于故障排查
     */
    public void setErrorMessage(String errorMessage) { 
        this.errorMessage = errorMessage; 
    }
    
    /**
     * 计算迁移耗时
     * 
     * @return 迁移耗时（毫秒），如果尚未结束则返回当前耗时
     */
    public long getDurationMs() {
        if (endTime > 0) {
            return endTime - startTime;
        } else {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * 计算迁移成功率
     * 
     * @return 成功率（0.0 - 1.0），如果总数为0则返回0.0
     */
    public double getSuccessRate() {
        int total = successCount + failCount;
        return total > 0 ? (double) successCount / total : 0.0;
    }
    
    /**
     * 判断迁移是否已完成（成功或失败）
     * 
     * @return true 如果迁移已完成，false 如果仍在进行中
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }
} 