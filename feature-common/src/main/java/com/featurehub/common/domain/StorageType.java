package com.featurehub.common.domain;

/**
 * 存储类型枚举
 */
public enum StorageType {
    /**
     * Redis热数据存储
     */
    REDIS("redis"),
    
    /**
     * KeeWiDB冷数据存储
     */
    KEEWIDB("keewidb");
    
    private final String value;
    
    StorageType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取枚举
     */
    public static StorageType fromValue(String value) {
        for (StorageType type : StorageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage type: " + value);
    }
    
    /**
     * 判断是否为热数据存储
     */
    public boolean isHotStorage() {
        return this == REDIS;
    }
    
    /**
     * 判断是否为冷数据存储
     */
    public boolean isColdStorage() {
        return this == KEEWIDB;
    }
} 