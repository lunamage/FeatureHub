package com.featurehub.common.domain;

/**
 * 数据迁移状态枚举
 */
public enum MigrationStatus {
    /**
     * 稳定状态（未在迁移中）
     */
    STABLE("stable"),
    
    /**
     * 迁移中
     */
    MIGRATING("migrating"),
    
    /**
     * 迁移失败
     */
    FAILED("failed");
    
    private final String value;
    
    MigrationStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取枚举
     */
    public static MigrationStatus fromValue(String value) {
        for (MigrationStatus status : MigrationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown migration status: " + value);
    }
    
    /**
     * 判断是否可以进行迁移操作
     */
    public boolean canMigrate() {
        return this == STABLE || this == FAILED;
    }
    
    /**
     * 判断是否在迁移中
     */
    public boolean isMigrating() {
        return this == MIGRATING;
    }
} 