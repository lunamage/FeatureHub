package com.featurehub.common.domain;

/**
 * 特征元数据实体
 * 记录特征Key的存储位置、访问信息等元数据
 */
public class FeatureMetadata {
    
    /**
     * 特征Key，主键
     */
    private String keyName;
    
    /**
     * 存储类型：redis 或 keewidb
     */
    private StorageType storageType;
    
    /**
     * 上次访问时间戳
     */
    private Long lastAccessTime;
    
    /**
     * 访问次数（周期性清零）
     */
    private Long accessCount;
    
    /**
     * 创建时间戳
     */
    private Long createTime;
    
    /**
     * 更新时间戳
     */
    private Long updateTime;
    
    /**
     * 过期时间戳（用于冷数据清理）
     */
    private Long expireTime;
    
    /**
     * 数据大小（字节）
     */
    private Long dataSize;
    
    /**
     * 业务标签
     */
    private String businessTag;
    
    /**
     * 迁移状态
     */
    private MigrationStatus migrationStatus;
    
    /**
     * 迁移时间戳
     */
    private Long migrationTime;

    // 构造函数
    public FeatureMetadata() {
        this.storageType = StorageType.REDIS;
        this.accessCount = 0L;
        this.dataSize = 0L;
        this.migrationStatus = MigrationStatus.STABLE;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.lastAccessTime = System.currentTimeMillis();
    }

    public FeatureMetadata(String keyName) {
        this();
        this.keyName = keyName;
    }

    // Getter and Setter methods
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public Long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public Long getDataSize() {
        return dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    public String getBusinessTag() {
        return businessTag;
    }

    public void setBusinessTag(String businessTag) {
        this.businessTag = businessTag;
    }

    public MigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(MigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    public Long getMigrationTime() {
        return migrationTime;
    }

    public void setMigrationTime(Long migrationTime) {
        this.migrationTime = migrationTime;
    }

    /**
     * 增加访问次数
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    /**
     * 重置访问次数（用于周期性统计）
     */
    public void resetAccessCount() {
        this.accessCount = 0L;
        this.updateTime = System.currentTimeMillis();
    }

    /**
     * 判断是否过期
     */
    public boolean isExpired() {
        return expireTime != null && expireTime < System.currentTimeMillis();
    }

    /**
     * 判断是否长时间未访问
     */
    public boolean isInactive(long inactiveThresholdMs) {
        return System.currentTimeMillis() - lastAccessTime > inactiveThresholdMs;
    }

    @Override
    public String toString() {
        return "FeatureMetadata{" +
                "keyName='" + keyName + '\'' +
                ", storageType=" + storageType +
                ", lastAccessTime=" + lastAccessTime +
                ", accessCount=" + accessCount +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", expireTime=" + expireTime +
                ", dataSize=" + dataSize +
                ", businessTag='" + businessTag + '\'' +
                ", migrationStatus=" + migrationStatus +
                ", migrationTime=" + migrationTime +
                '}';
    }
} 