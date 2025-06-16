package com.featurehub.cleaner.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class KeeWiDbClient {

    @Autowired
    @Qualifier("keewidbRedisTemplate")
    private RedisTemplate<String, String> keewidbTemplate;

    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(keewidbTemplate.delete(key));
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getAllFeatureKeys() {
        return keewidbTemplate.keys("feature:*");
    }

    public long getKeyCount() {
        return keewidbTemplate.getConnectionFactory().getConnection().dbSize();
    }

    public String getMemoryUsage() {
        return "N/A"; // KeeWiDB内存使用信息
    }
} 