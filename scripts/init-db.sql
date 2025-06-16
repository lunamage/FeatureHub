 -- 特征中心数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS featurehub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE featurehub;

-- 创建特征元数据表
CREATE TABLE IF NOT EXISTS feature_metadata (
    key_name VARCHAR(255) PRIMARY KEY COMMENT '特征Key，主键',
    storage_type ENUM('redis', 'keewidb') NOT NULL DEFAULT 'redis' COMMENT '存储类型：redis 或 keewidb',
    last_access_time BIGINT NOT NULL COMMENT '上次访问时间戳',
    access_count BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数（周期性清零）',
    create_time BIGINT NOT NULL COMMENT '创建时间戳',
    update_time BIGINT NOT NULL COMMENT '更新时间戳',
    expire_time BIGINT NULL COMMENT '过期时间戳（用于冷数据清理）',
    data_size BIGINT NOT NULL DEFAULT 0 COMMENT '数据大小（字节）',
    business_tag VARCHAR(100) NULL COMMENT '业务标签',
    migration_status ENUM('stable', 'migrating', 'failed') NOT NULL DEFAULT 'stable' COMMENT '迁移状态',
    migration_time BIGINT NULL COMMENT '迁移时间戳',
    INDEX idx_storage_type (storage_type),
    INDEX idx_last_access_time (last_access_time),
    INDEX idx_expire_time (expire_time),
    INDEX idx_business_tag (business_tag),
    INDEX idx_migration_status (migration_status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特征元数据表';

-- 创建冷热分层配置表
CREATE TABLE IF NOT EXISTS hot_cold_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE COMMENT '配置名称',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    config_type ENUM('STRATEGY', 'THRESHOLD', 'SCHEDULE') NOT NULL COMMENT '配置类型',
    description TEXT COMMENT '配置描述',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    create_time BIGINT NOT NULL COMMENT '创建时间',
    update_time BIGINT NOT NULL COMMENT '更新时间',
    INDEX idx_config_type (config_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='冷热分层配置表';

-- 创建数据迁移记录表
CREATE TABLE IF NOT EXISTS migration_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(255) NOT NULL COMMENT '特征Key',
    migration_type ENUM('HOT_TO_COLD', 'COLD_TO_HOT') NOT NULL COMMENT '迁移类型',
    source_storage ENUM('redis', 'keewidb') NOT NULL COMMENT '源存储',
    target_storage ENUM('redis', 'keewidb') NOT NULL COMMENT '目标存储',
    migration_status ENUM('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '迁移状态',
    start_time BIGINT NOT NULL COMMENT '开始时间',
    end_time BIGINT NULL COMMENT '结束时间',
    error_message TEXT NULL COMMENT '错误信息',
    data_size BIGINT NULL COMMENT '数据大小',
    INDEX idx_key_name (key_name),
    INDEX idx_migration_type (migration_type),
    INDEX idx_migration_status (migration_status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据迁移记录表';

-- 创建查询统计表（用于冷热分析）
CREATE TABLE IF NOT EXISTS query_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(255) NOT NULL COMMENT '特征Key',
    query_date DATE NOT NULL COMMENT '查询日期',
    query_hour TINYINT NOT NULL COMMENT '查询小时(0-23)',
    query_count BIGINT NOT NULL DEFAULT 0 COMMENT '查询次数',
    total_query_time BIGINT NOT NULL DEFAULT 0 COMMENT '总查询时间(毫秒)',
    avg_query_time DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平均查询时间(毫秒)',
    storage_type ENUM('redis', 'keewidb') NOT NULL COMMENT '存储类型',
    create_time BIGINT NOT NULL COMMENT '创建时间',
    update_time BIGINT NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_key_date_hour (key_name, query_date, query_hour),
    INDEX idx_query_date (query_date),
    INDEX idx_storage_type (storage_type),
    INDEX idx_key_name (key_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询统计表';

-- 插入默认配置
INSERT INTO hot_cold_config (config_name, config_value, config_type, description, create_time, update_time) VALUES
('hot_to_cold_access_threshold_hours', '24', 'THRESHOLD', '热转冷访问统计时间窗口（小时）', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('hot_to_cold_access_count_threshold', '5', 'THRESHOLD', '热转冷访问次数阈值', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('hot_to_cold_idle_days_threshold', '7', 'THRESHOLD', '热转冷不活跃天数阈值', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('cold_to_hot_access_threshold_hours', '1', 'THRESHOLD', '冷转热访问统计时间窗口（小时）', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('cold_to_hot_access_count_threshold', '10', 'THRESHOLD', '冷转热访问次数阈值', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('migration_batch_size', '100', 'STRATEGY', '数据迁移批次大小', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('migration_schedule_cron', '0 */30 * * * ?', 'SCHEDULE', '迁移任务调度表达式（每30分钟执行一次）', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('cleanup_schedule_cron', '0 0 2 * * ?', 'SCHEDULE', '清理任务调度表达式（每天凌晨2点执行）', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000),
('data_retention_days', '90', 'STRATEGY', '冷数据保留天数', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000);

-- 创建索引优化查询性能
CREATE INDEX idx_metadata_combined ON feature_metadata(storage_type, last_access_time, migration_status);
CREATE INDEX idx_migration_combined ON migration_record(migration_status, start_time);

-- 创建视图用于统计查询
CREATE VIEW v_storage_stats AS
SELECT 
    storage_type,
    COUNT(*) as total_keys,
    SUM(data_size) as total_size,
    AVG(access_count) as avg_access_count,
    MIN(last_access_time) as earliest_access_time,
    MAX(last_access_time) as latest_access_time
FROM feature_metadata
WHERE migration_status = 'stable'
GROUP BY storage_type;

-- 创建视图用于迁移统计
CREATE VIEW v_migration_stats AS
SELECT 
    DATE(FROM_UNIXTIME(start_time/1000)) as migration_date,
    migration_type,
    migration_status,
    COUNT(*) as migration_count,
    AVG(CASE WHEN end_time IS NOT NULL THEN end_time - start_time ELSE NULL END) as avg_duration_ms
FROM migration_record
GROUP BY DATE(FROM_UNIXTIME(start_time/1000)), migration_type, migration_status;

-- 创建性能监控表
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(15,4) NOT NULL COMMENT '指标值',
    metric_unit VARCHAR(20) NOT NULL COMMENT '指标单位',
    service_name VARCHAR(50) NOT NULL COMMENT '服务名称',
    create_time BIGINT NOT NULL COMMENT '创建时间',
    INDEX idx_metric_name (metric_name),
    INDEX idx_service_name (service_name),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='性能监控指标表';

-- 显示表结构
SHOW TABLES;
DESCRIBE feature_metadata;
DESCRIBE hot_cold_config;
DESCRIBE migration_record;
DESCRIBE query_statistics;
DESCRIBE performance_metrics;