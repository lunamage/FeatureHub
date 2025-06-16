package com.featurehub.hotcold.client;

import org.springframework.stereotype.Component;

@Component
public class RedisClient {
    
    public String get(String key) {
        // TODO: 实现Redis get操作
        return null;
    }
    
    public boolean set(String key, String value) {
        // TODO: 实现Redis set操作
        return true;
    }
    
    public boolean delete(String key) {
        // TODO: 实现Redis delete操作
        return true;
    }
} 