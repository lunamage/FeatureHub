package com.featurehub.common.dto;

import com.featurehub.common.domain.StorageType;
import java.util.List;

/**
 * 特征查询响应DTO
 */
public class FeatureQueryResponse {
    
    /**
     * 单个查询结果
     */
    private FeatureResult result;
    
    /**
     * 批量查询结果
     */
    private List<FeatureResult> results;
    
    /**
     * 批量查询统计信息
     */
    private QuerySummary summary;
    
    public FeatureQueryResponse() {
    }
    
    public FeatureQueryResponse(FeatureResult result) {
        this.result = result;
    }
    
    public FeatureQueryResponse(List<FeatureResult> results, QuerySummary summary) {
        this.results = results;
        this.summary = summary;
    }
    
    public FeatureResult getResult() {
        return result;
    }
    
    public void setResult(FeatureResult result) {
        this.result = result;
    }
    
    public List<FeatureResult> getResults() {
        return results;
    }
    
    public void setResults(List<FeatureResult> results) {
        this.results = results;
    }
    
    public QuerySummary getSummary() {
        return summary;
    }
    
    public void setSummary(QuerySummary summary) {
        this.summary = summary;
    }
    
    /**
     * 单个特征查询结果
     */
    public static class FeatureResult {
        private String key;
        private String value;
        private StorageType source;
        private boolean found;
        private String error;
        private Long queryTimeMs;
        
        public FeatureResult() {
        }
        
        public FeatureResult(String key, String value, StorageType source) {
            this.key = key;
            this.value = value;
            this.source = source;
            this.found = true;
        }
        
        public FeatureResult(String key, String error) {
            this.key = key;
            this.error = error;
            this.found = false;
        }
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public StorageType getSource() {
            return source;
        }
        
        public void setSource(StorageType source) {
            this.source = source;
        }
        
        public boolean isFound() {
            return found;
        }
        
        public void setFound(boolean found) {
            this.found = found;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public Long getQueryTimeMs() {
            return queryTimeMs;
        }
        
        public void setQueryTimeMs(Long queryTimeMs) {
            this.queryTimeMs = queryTimeMs;
        }
    }
    
    /**
     * 批量查询统计信息
     */
    public static class QuerySummary {
        private int totalRequested;
        private int found;
        private int notFound;
        private int redisHits;
        private int keewidbHits;
        private Long totalQueryTimeMs;
        
        public QuerySummary() {
        }
        
        public QuerySummary(int totalRequested, int found, int notFound, 
                           int redisHits, int keewidbHits, Long totalQueryTimeMs) {
            this.totalRequested = totalRequested;
            this.found = found;
            this.notFound = notFound;
            this.redisHits = redisHits;
            this.keewidbHits = keewidbHits;
            this.totalQueryTimeMs = totalQueryTimeMs;
        }
        
        public int getTotalRequested() {
            return totalRequested;
        }
        
        public void setTotalRequested(int totalRequested) {
            this.totalRequested = totalRequested;
        }
        
        public int getFound() {
            return found;
        }
        
        public void setFound(int found) {
            this.found = found;
        }
        
        public int getNotFound() {
            return notFound;
        }
        
        public void setNotFound(int notFound) {
            this.notFound = notFound;
        }
        
        public int getRedisHits() {
            return redisHits;
        }
        
        public void setRedisHits(int redisHits) {
            this.redisHits = redisHits;
        }
        
        public int getKeewidbHits() {
            return keewidbHits;
        }
        
        public void setKeewidbHits(int keewidbHits) {
            this.keewidbHits = keewidbHits;
        }
        
        public Long getTotalQueryTimeMs() {
            return totalQueryTimeMs;
        }
        
        public void setTotalQueryTimeMs(Long totalQueryTimeMs) {
            this.totalQueryTimeMs = totalQueryTimeMs;
        }
    }
} 