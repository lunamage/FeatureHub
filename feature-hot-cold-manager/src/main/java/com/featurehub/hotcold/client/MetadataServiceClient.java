package com.featurehub.hotcold.client;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.common.domain.StorageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetadataServiceClient {
    
    public List<FeatureMetadata> getDataForMigration(StorageType storageType, long accessTimeThreshold, int maxSize) {
        // TODO: 实现获取迁移数据逻辑
        return new ArrayList<>();
    }
    
    public List<FeatureMetadata> getDataForRecall(StorageType storageType, int accessThreshold, long recentAccessTime, int maxSize) {
        // TODO: 实现获取召回数据逻辑
        return new ArrayList<>();
    }
    
    public void updateMetadata(FeatureMetadata metadata) {
        // TODO: 实现更新元数据逻辑
    }
} 