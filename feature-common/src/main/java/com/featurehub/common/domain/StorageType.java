package com.featurehub.common.domain;

/**
 * 存储类型枚举
 * <p>
 * 定义了特征平台支持的存储类型，用于区分热存储和冷存储。
 * 系统根据访问频率自动在不同存储类型间迁移数据，以优化成本和性能。
 * </p>
 * 
 * <h3>存储策略:</h3>
 * <ul>
 *   <li>REDIS: 高频访问的热数据，提供毫秒级响应</li>
 *   <li>KEEWIDB: 低频访问的冷数据，提供成本优化的存储</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 根据字符串创建枚举
 * StorageType type = StorageType.fromValue("redis");
 * 
 * // 判断存储类型
 * if (type.isHotStorage()) {
 *     // 热数据处理逻辑
 * }
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FeatureMetadata
 */
public enum StorageType {
    /**
     * Redis热数据存储
     * <p>
     * 用于存储高频访问的特征数据，提供毫秒级的查询响应。
     * 适合实时推荐、用户画像等场景。
     * </p>
     */
    REDIS("redis"),
    
    /**
     * KeeWiDB冷数据存储  
     * <p>
     * 用于存储低频访问的特征数据，提供成本优化的存储方案。
     * 适合历史数据、归档数据等场景。
     * </p>
     */
    KEEWIDB("keewidb");
    
    /**
     * 存储类型的字符串值
     */
    private final String value;
    
    /**
     * 构造函数
     * 
     * @param value 存储类型的字符串值
     */
    StorageType(String value) {
        this.value = value;
    }
    
    /**
     * 获取存储类型的字符串值
     * 
     * @return 存储类型的字符串表示
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取对应的存储类型枚举
     * <p>
     * 支持的值: "redis", "keewidb"
     * </p>
     * 
     * @param value 存储类型的字符串值，不区分大小写
     * @return 对应的存储类型枚举
     * @throws IllegalArgumentException 如果值不被支持
     * 
     * @see #getValue()
     */
    public static StorageType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Storage type value cannot be null");
        }
        
        for (StorageType type : StorageType.values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage type: " + value);
    }
    
    /**
     * 判断是否为热数据存储
     * <p>
     * 热数据存储通常具有更高的性能，但成本也更高。
     * </p>
     * 
     * @return true 如果是热数据存储，false 否则
     */
    public boolean isHotStorage() {
        return this == REDIS;
    }
    
    /**
     * 判断是否为冷数据存储
     * <p>
     * 冷数据存储成本较低，但访问性能相对较低。
     * </p>
     * 
     * @return true 如果是冷数据存储，false 否则
     */
    public boolean isColdStorage() {
        return this == KEEWIDB;
    }
} 