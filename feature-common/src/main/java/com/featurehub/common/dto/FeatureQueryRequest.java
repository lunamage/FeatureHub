package com.featurehub.common.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 特征查询请求DTO
 */
public class FeatureQueryRequest {
    
    /**
     * 单个特征Key查询
     */
    @NotBlank(message = "Feature key cannot be blank")
    private String key;
    
    /**
     * 批量特征Key查询
     */
    @NotEmpty(message = "Feature keys cannot be empty")
    private List<String> keys;
    
    /**
     * 查询选项
     */
    private QueryOptions options;
    
    public FeatureQueryRequest() {
        this.options = new QueryOptions();
    }
    
    public FeatureQueryRequest(String key) {
        this();
        this.key = key;
    }
    
    public FeatureQueryRequest(List<String> keys) {
        this();
        this.keys = keys;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public List<String> getKeys() {
        return keys;
    }
    
    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
    
    public QueryOptions getOptions() {
        return options;
    }
    
    public void setOptions(QueryOptions options) {
        this.options = options;
    }
    
    /**
     * 查询选项
     */
    public static class QueryOptions {
        /**
         * 是否返回元数据信息
         */
        private boolean includeMetadata = false;
        
        /**
         * 查询超时时间（毫秒）
         */
        private Long timeoutMs = 5000L;
        
        /**
         * 客户端IP（用于统计）
         */
        private String clientIp;
        
        /**
         * 用户ID（用于统计）
         */
        private String userId;
        
        public boolean isIncludeMetadata() {
            return includeMetadata;
        }
        
        public void setIncludeMetadata(boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
        }
        
        public Long getTimeoutMs() {
            return timeoutMs;
        }
        
        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
        
        public String getClientIp() {
            return clientIp;
        }
        
        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
} 