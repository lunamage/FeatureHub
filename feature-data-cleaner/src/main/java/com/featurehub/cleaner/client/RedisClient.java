package com.featurehub.cleaner.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisClient {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getAllFeatureKeys() {
        return redisTemplate.keys("feature:*");
    }

    public long getKeyCount() {
        return redisTemplate.getConnectionFactory().getConnection().dbSize();
    }

    public String getMemoryUsage() {
        return "N/A"; // Redis内存使用信息
    }
} 