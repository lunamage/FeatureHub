package com.featurehub.metadata.mapper;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.common.domain.StorageType;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 特征元数据 Mapper 接口
 * 定义数据库操作方法
 */
@Mapper
public interface FeatureMetadataMapper {

    /**
     * 根据Key查询元数据
     */
    @Select("SELECT * FROM feature_metadata WHERE key_name = #{keyName}")
    @Results({
        @Result(property = "keyName", column = "key_name"),
        @Result(property = "storageType", column = "storage_type", 
                typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
        @Result(property = "lastAccessTime", column = "last_access_time"),
        @Result(property = "accessCount", column = "access_count"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "expireTime", column = "expire_time"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "businessTag", column = "business_tag"),
        @Result(property = "migrationStatus", column = "migration_status",
                typeHandler = org.apache.ibatis.type.EnumTypeHandler.class),
        @Result(property = "migrationTime", column = "migration_time")
    })
    FeatureMetadata selectByKey(@Param("keyName") String keyName);

    /**
     * 根据Key列表批量查询元数据
     */
    @Select("<script>" +
            "SELECT * FROM feature_metadata WHERE key_name IN " +
            "<foreach collection='keys' item='key' open='(' separator=',' close=')'>" +
            "#{key}" +
            "</foreach>" +
            "</script>")
    @Results({
        @Result(property = "keyName", column = "key_name"),
        @Result(property = "storageType", column = "storage_type"),
        @Result(property = "lastAccessTime", column = "last_access_time"),
        @Result(property = "accessCount", column = "access_count"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "expireTime", column = "expire_time"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "businessTag", column = "business_tag"),
        @Result(property = "migrationStatus", column = "migration_status"),
        @Result(property = "migrationTime", column = "migration_time")
    })
    List<FeatureMetadata> selectByKeys(@Param("keys") List<String> keys);

    /**
     * 插入新的元数据记录
     */
    @Insert("INSERT INTO feature_metadata (" +
            "key_name, storage_type, last_access_time, access_count, create_time, " +
            "update_time, expire_time, data_size, business_tag, migration_status, migration_time" +
            ") VALUES (" +
            "#{keyName}, #{storageType}, #{lastAccessTime}, #{accessCount}, #{createTime}, " +
            "#{updateTime}, #{expireTime}, #{dataSize}, #{businessTag}, #{migrationStatus}, #{migrationTime}" +
            ")")
    int insert(FeatureMetadata metadata);

    /**
     * 根据Key更新元数据
     */
    @Update("UPDATE feature_metadata SET " +
            "storage_type = #{storageType}, " +
            "last_access_time = #{lastAccessTime}, " +
            "access_count = #{accessCount}, " +
            "update_time = #{updateTime}, " +
            "expire_time = #{expireTime}, " +
            "data_size = #{dataSize}, " +
            "business_tag = #{businessTag}, " +
            "migration_status = #{migrationStatus}, " +
            "migration_time = #{migrationTime} " +
            "WHERE key_name = #{keyName}")
    int updateByKey(FeatureMetadata metadata);

    /**
     * 根据Key删除元数据
     */
    @Delete("DELETE FROM feature_metadata WHERE key_name = #{keyName}")
    int deleteByKey(@Param("keyName") String keyName);

    /**
     * 统计各存储类型的数量
     */
    @Select("SELECT storage_type as storageType, COUNT(*) as count " +
            "FROM feature_metadata " +
            "GROUP BY storage_type")
    @MapKey("storageType")
    Map<String, Integer> countByStorageType();

    /**
     * 根据存储类型获取详细统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(data_size) as total_size, " +
            "AVG(access_count) as avg_access_count, " +
            "MIN(last_access_time) as earliest_access_time, " +
            "MAX(last_access_time) as latest_access_time " +
            "FROM feature_metadata " +
            "WHERE storage_type = #{storageType} AND migration_status = 'stable'")
    Map<String, Object> getDetailStatsByStorageType(@Param("storageType") StorageType storageType);

    /**
     * 根据业务标签获取统计
     */
    @Select("SELECT " +
            "COUNT(*) as count, " +
            "SUM(data_size) as total_size, " +
            "AVG(access_count) as avg_access_count " +
            "FROM feature_metadata " +
            "WHERE business_tag = #{businessTag}")
    Map<String, Object> getStatsByBusinessTag(@Param("businessTag") String businessTag);

    /**
     * 统计活跃Key数量（在指定时间之后有访问的）
     */
    @Select("SELECT COUNT(*) FROM feature_metadata WHERE last_access_time > #{sinceTime}")
    int countActiveKeys(@Param("sinceTime") long sinceTime);

    /**
     * 查询过期的Key列表
     */
    @Select("SELECT key_name FROM feature_metadata " +
            "WHERE expire_time IS NOT NULL AND expire_time < #{currentTime}")
    List<String> selectExpiredKeys(@Param("currentTime") long currentTime);

    /**
     * 删除过期的元数据
     */
    @Delete("DELETE FROM feature_metadata " +
            "WHERE expire_time IS NOT NULL AND expire_time < #{currentTime}")
    int deleteExpiredKeys(@Param("currentTime") long currentTime);

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM feature_metadata")
    int countTotal();

    /**
     * 根据存储类型和访问时间查询需要迁移的Key
     */
    @Select("SELECT * FROM feature_metadata " +
            "WHERE storage_type = #{storageType} " +
            "AND last_access_time < #{accessTimeThreshold} " +
            "AND migration_status = 'stable' " +
            "ORDER BY last_access_time ASC " +
            "LIMIT #{limit}")
    @Results({
        @Result(property = "keyName", column = "key_name"),
        @Result(property = "storageType", column = "storage_type"),
        @Result(property = "lastAccessTime", column = "last_access_time"),
        @Result(property = "accessCount", column = "access_count"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "expireTime", column = "expire_time"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "businessTag", column = "business_tag"),
        @Result(property = "migrationStatus", column = "migration_status"),
        @Result(property = "migrationTime", column = "migration_time")
    })
    List<FeatureMetadata> selectForMigration(
            @Param("storageType") StorageType storageType,
            @Param("accessTimeThreshold") long accessTimeThreshold,
            @Param("limit") int limit);

    /**
     * 根据访问频率查询需要召回的Key
     */
    @Select("SELECT * FROM feature_metadata " +
            "WHERE storage_type = #{storageType} " +
            "AND access_count >= #{accessCountThreshold} " +
            "AND last_access_time > #{recentAccessTime} " +
            "AND migration_status = 'stable' " +
            "ORDER BY access_count DESC, last_access_time DESC " +
            "LIMIT #{limit}")
    @Results({
        @Result(property = "keyName", column = "key_name"),
        @Result(property = "storageType", column = "storage_type"),
        @Result(property = "lastAccessTime", column = "last_access_time"),
        @Result(property = "accessCount", column = "access_count"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "expireTime", column = "expire_time"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "businessTag", column = "business_tag"),
        @Result(property = "migrationStatus", column = "migration_status"),
        @Result(property = "migrationTime", column = "migration_time")
    })
    List<FeatureMetadata> selectForRecall(
            @Param("storageType") StorageType storageType,
            @Param("accessCountThreshold") int accessCountThreshold,
            @Param("recentAccessTime") long recentAccessTime,
            @Param("limit") int limit);

    /**
     * 批量更新迁移状态
     */
    @Update("<script>" +
            "UPDATE feature_metadata SET " +
            "migration_status = #{migrationStatus}, " +
            "migration_time = #{migrationTime} " +
            "WHERE key_name IN " +
            "<foreach collection='keyNames' item='keyName' open='(' separator=',' close=')'>" +
            "#{keyName}" +
            "</foreach>" +
            "</script>")
    int batchUpdateMigrationStatus(
            @Param("keyNames") List<String> keyNames,
            @Param("migrationStatus") String migrationStatus,
            @Param("migrationTime") long migrationTime);

    /**
     * 重置访问计数（用于周期性统计）
     */
    @Update("UPDATE feature_metadata SET access_count = 0, update_time = #{updateTime}")
    int resetAccessCount(@Param("updateTime") long updateTime);
} 