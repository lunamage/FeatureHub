package com.featurehub.cleaner.domain;

public class CleanupRecord {
    private String taskId;
    private String cleanupType;
    private String status;
    private long startTime;
    private long endTime;
    private int cleanedCount;
    private int failedCount;
    private String errorMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCleanupType() {
        return cleanupType;
    }

    public void setCleanupType(String cleanupType) {
        this.cleanupType = cleanupType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getCleanedCount() {
        return cleanedCount;
    }

    public void setCleanedCount(int cleanedCount) {
        this.cleanedCount = cleanedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 