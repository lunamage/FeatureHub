package com.featurehub.common.domain;

/**
 * 特征元数据实体类
 * <p>
 * 该类用于记录特征Key的存储位置、访问信息等元数据，是整个特征平台的核心数据模型。
 * 支持热冷数据分离存储，能够跟踪特征的访问模式，为数据迁移和清理提供决策依据。
 * </p>
 * 
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>记录特征存储位置(Redis/KeeWiDB)</li>
 *   <li>跟踪访问频率和时间</li>
 *   <li>支持数据迁移状态管理</li>
 *   <li>提供过期和活跃性判断</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 创建新的特征元数据
 * FeatureMetadata metadata = new FeatureMetadata("user:123:profile");
 * metadata.setStorageType(StorageType.REDIS);
 * metadata.setDataSize(1024L);
 * 
 * // 记录访问
 * metadata.incrementAccessCount();
 * 
 * // 检查是否需要迁移
 * if (metadata.isInactive(TimeUnit.DAYS.toMillis(7))) {
 *     // 迁移到冷存储
 * }
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see StorageType
 * @see MigrationStatus
 */
public class FeatureMetadata {
    
    /**
     * 特征Key，作为主键标识
     * <p>通常格式为: 业务类型:用户ID:特征类型，如 "user:123:profile"</p>
     */
    private String keyName;
    
    /**
     * 存储类型，标识数据当前存储位置
     * <p>支持Redis热存储和KeeWiDB冷存储</p>
     * 
     * @see StorageType
     */
    private StorageType storageType;
    
    /**
     * 上次访问时间戳（毫秒）
     * <p>用于判断数据活跃性，决定是否需要进行热冷迁移</p>
     */
    private Long lastAccessTime;
    
    /**
     * 访问次数（在统计周期内）
     * <p>定期清零，用于统计热度指标</p>
     */
    private Long accessCount;
    
    /**
     * 创建时间戳（毫秒）
     * <p>记录特征首次写入系统的时间</p>
     */
    private Long createTime;
    
    /**
     * 更新时间戳（毫秒）
     * <p>记录元数据最后一次修改的时间</p>
     */
    private Long updateTime;
    
    /**
     * 过期时间戳（毫秒）
     * <p>用于数据生命周期管理，过期数据将被清理</p>
     */
    private Long expireTime;
    
    /**
     * 数据大小（字节）
     * <p>用于存储容量统计和成本分析</p>
     */
    private Long dataSize;
    
    /**
     * 业务标签
     * <p>用于业务分类和统计，如 "user_profile", "recommendation"等</p>
     */
    private String businessTag;
    
    /**
     * 迁移状态
     * <p>记录当前的数据迁移状态，用于迁移流程控制</p>
     * 
     * @see MigrationStatus
     */
    private MigrationStatus migrationStatus;
    
    /**
     * 迁移时间戳（毫秒）
     * <p>记录最后一次数据迁移的时间</p>
     */
    private Long migrationTime;

    /**
     * 默认构造函数
     * <p>初始化默认值：存储类型为Redis，访问次数为0，迁移状态为稳定</p>
     */
    public FeatureMetadata() {
        this.storageType = StorageType.REDIS;
        this.accessCount = 0L;
        this.dataSize = 0L;
        this.migrationStatus = MigrationStatus.STABLE;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 带特征Key的构造函数
     * 
     * @param keyName 特征Key，不能为空
     * @throws IllegalArgumentException 如果keyName为空
     */
    public FeatureMetadata(String keyName) {
        this();
        if (keyName == null || keyName.trim().isEmpty()) {
            throw new IllegalArgumentException("keyName cannot be null or empty");
        }
        this.keyName = keyName;
    }

    // Getter and Setter methods
    /**
     * 获取特征Key
     * 
     * @return 特征Key，可能为null
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * 设置特征Key
     * 
     * @param keyName 特征Key，不建议为空
     */
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    /**
     * 获取存储类型
     * 
     * @return 当前存储类型，默认为REDIS
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * 设置存储类型
     * 
     * @param storageType 存储类型，不能为null
     */
    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    /**
     * 获取最后访问时间
     * 
     * @return 最后访问时间戳（毫秒），可能为null
     */
    public Long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * 设置最后访问时间
     * 
     * @param lastAccessTime 访问时间戳（毫秒）
     */
    public void setLastAccessTime(Long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    /**
     * 获取访问次数
     * 
     * @return 当前统计周期内的访问次数，默认为0
     */
    public Long getAccessCount() {
        return accessCount;
    }

    /**
     * 设置访问次数
     * 
     * @param accessCount 访问次数，建议为非负数
     */
    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    /**
     * 获取创建时间
     * 
     * @return 创建时间戳（毫秒）
     */
    public Long getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     * 
     * @param createTime 创建时间戳（毫秒）
     */
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间
     * 
     * @return 更新时间戳（毫秒）
     */
    public Long getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     * 
     * @param updateTime 更新时间戳（毫秒）
     */
    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取过期时间
     * 
     * @return 过期时间戳（毫秒），null表示不过期
     */
    public Long getExpireTime() {
        return expireTime;
    }

    /**
     * 设置过期时间
     * 
     * @param expireTime 过期时间戳（毫秒），null表示不过期
     */
    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 获取数据大小
     * 
     * @return 数据大小（字节），默认为0
     */
    public Long getDataSize() {
        return dataSize;
    }

    /**
     * 设置数据大小
     * 
     * @param dataSize 数据大小（字节），建议为非负数
     */
    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * 获取业务标签
     * 
     * @return 业务标签，可能为null
     */
    public String getBusinessTag() {
        return businessTag;
    }

    /**
     * 设置业务标签
     * 
     * @param businessTag 业务标签，用于分类统计
     */
    public void setBusinessTag(String businessTag) {
        this.businessTag = businessTag;
    }

    /**
     * 获取迁移状态
     * 
     * @return 当前迁移状态，默认为STABLE
     */
    public MigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    /**
     * 设置迁移状态
     * 
     * @param migrationStatus 迁移状态，不能为null
     */
    public void setMigrationStatus(MigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    /**
     * 获取迁移时间
     * 
     * @return 迁移时间戳（毫秒），可能为null
     */
    public Long getMigrationTime() {
        return migrationTime;
    }

    /**
     * 设置迁移时间
     * 
     * @param migrationTime 迁移时间戳（毫秒）
     */
    public void setMigrationTime(Long migrationTime) {
        this.migrationTime = migrationTime;
    }

    /**
     * 增加访问次数
     * <p>
     * 自动更新访问次数、最后访问时间和更新时间。
     * 该方法是线程安全的。
     * </p>
     * 
     * @apiNote 每次特征被查询时都应该调用此方法来更新访问统计
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    /**
     * 重置访问次数
     * <p>
     * 用于周期性统计，通常在统计周期结束时调用。
     * 重置后访问次数归零，但保留历史访问时间。
     * </p>
     * 
     * @apiNote 建议在每日/每周统计完成后调用此方法
     */
    public void resetAccessCount() {
        this.accessCount = 0L;
        this.updateTime = System.currentTimeMillis();
    }

    /**
     * 判断数据是否已过期
     * <p>
     * 根据设置的过期时间判断数据是否应该被清理。
     * 如果未设置过期时间，则认为数据不会过期。
     * </p>
     * 
     * @return true 如果数据已过期，false 如果数据未过期或未设置过期时间
     * 
     * @see #setExpireTime(Long)
     */
    public boolean isExpired() {
        return expireTime != null && expireTime < System.currentTimeMillis();
    }

    /**
     * 判断数据是否长时间未访问（不活跃）
     * <p>
     * 根据指定的不活跃阈值判断数据是否应该被迁移到冷存储。
     * 计算方式：当前时间 - 最后访问时间 > 阈值
     * </p>
     * 
     * @param inactiveThresholdMs 不活跃阈值（毫秒），必须大于0
     * @return true 如果数据不活跃，false 如果数据仍然活跃
     * @throws IllegalArgumentException 如果阈值小于等于0
     * 
     * @see #getLastAccessTime()
     */
    public boolean isInactive(long inactiveThresholdMs) {
        if (inactiveThresholdMs <= 0) {
            throw new IllegalArgumentException("inactiveThresholdMs must be positive");
        }
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