package com.featurehub.common.domain;

/**
 * 查询日志实体
 * 用于记录特征查询日志，供冷热数据分析使用
 */
public class QueryLog {
    
    /**
     * 被查询的特征Key
     */
    private String key;
    
    /**
     * 查询发生的时间戳
     */
    private Long timestamp;
    
    /**
     * 本次查询实际命中的存储（Redis或KeeWiDB）
     */
    private StorageType sourceStorage;
    
    /**
     * 客户端IP地址（可选）
     */
    private String clientIp;
    
    /**
     * 用户ID（可选）
     */
    private String userId;
    
    /**
     * 查询是否成功
     */
    private boolean success;
    
    /**
     * 查询耗时（毫秒）
     */
    private Long queryTimeMs;
    
    /**
     * 错误信息（如果查询失败）
     */
    private String errorMessage;
    
    /**
     * 业务标签
     */
    private String businessTag;
    
    public QueryLog() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public QueryLog(String key, StorageType sourceStorage) {
        this();
        this.key = key;
        this.sourceStorage = sourceStorage;
        this.success = true;
    }
    
    public QueryLog(String key, StorageType sourceStorage, String clientIp, String userId) {
        this(key, sourceStorage);
        this.clientIp = clientIp;
        this.userId = userId;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public StorageType getSourceStorage() {
        return sourceStorage;
    }
    
    public void setSourceStorage(StorageType sourceStorage) {
        this.sourceStorage = sourceStorage;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Long getQueryTimeMs() {
        return queryTimeMs;
    }
    
    public void setQueryTimeMs(Long queryTimeMs) {
        this.queryTimeMs = queryTimeMs;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getBusinessTag() {
        return businessTag;
    }
    
    public void setBusinessTag(String businessTag) {
        this.businessTag = businessTag;
    }
    
    /**
     * 创建成功查询日志
     */
    public static QueryLog success(String key, StorageType sourceStorage, Long queryTimeMs) {
        QueryLog log = new QueryLog(key, sourceStorage);
        log.setQueryTimeMs(queryTimeMs);
        log.setSuccess(true);
        return log;
    }
    
    /**
     * 创建失败查询日志
     */
    public static QueryLog failure(String key, String errorMessage, Long queryTimeMs) {
        QueryLog log = new QueryLog();
        log.setKey(key);
        log.setSuccess(false);
        log.setErrorMessage(errorMessage);
        log.setQueryTimeMs(queryTimeMs);
        return log;
    }
    
    @Override
    public String toString() {
        return "QueryLog{" +
                "key='" + key + '\'' +
                ", timestamp=" + timestamp +
                ", sourceStorage=" + sourceStorage +
                ", clientIp='" + clientIp + '\'' +
                ", userId='" + userId + '\'' +
                ", success=" + success +
                ", queryTimeMs=" + queryTimeMs +
                ", errorMessage='" + errorMessage + '\'' +
                ", businessTag='" + businessTag + '\'' +
                '}';
    }
} 