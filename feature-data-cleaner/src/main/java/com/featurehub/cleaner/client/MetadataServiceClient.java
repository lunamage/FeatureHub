package com.featurehub.cleaner.client;

import com.featurehub.common.domain.FeatureMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetadataServiceClient {

    public List<String> getExpiredKeys() {
        // TODO: 实现从元数据服务获取过期Keys
        return new ArrayList<>();
    }

    public FeatureMetadata getMetadata(String key) {
        // TODO: 实现获取元数据
        return null;
    }

    public int cleanupExpiredMetadata() {
        // TODO: 实现清理过期元数据
        return 0;
    }

    public long getTotalMetadataCount() {
        // TODO: 实现获取元数据总数
        return 0;
    }
} 