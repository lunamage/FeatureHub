package com.featurehub.common.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 特征查询请求DTO
 * <p>
 * 封装特征查询的请求参数，支持单个特征和批量特征查询。
 * 提供丰富的查询选项，包括超时设置、元数据返回控制等。
 * </p>
 * 
 * <h3>查询模式:</h3>
 * <ul>
 *   <li>单个查询: 设置key字段</li>
 *   <li>批量查询: 设置keys字段</li>
 * </ul>
 * 
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 单个特征查询
 * FeatureQueryRequest request = new FeatureQueryRequest("user:123:profile");
 * request.getOptions().setIncludeMetadata(true);
 * request.getOptions().setTimeoutMs(3000L);
 * 
 * // 批量特征查询
 * List<String> keys = Arrays.asList("user:123:profile", "user:123:preference");
 * FeatureQueryRequest batchRequest = new FeatureQueryRequest(keys);
 * batchRequest.getOptions().setClientIp("192.168.1.1");
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FeatureQueryResponse
 */
public class FeatureQueryRequest {
    
    /**
     * 单个特征Key查询
     * <p>用于单个特征查询模式，与keys字段互斥</p>
     */
    @NotBlank(message = "Feature key cannot be blank")
    private String key;
    
    /**
     * 批量特征Key查询
     * <p>用于批量特征查询模式，与key字段互斥</p>
     */
    @NotEmpty(message = "Feature keys cannot be empty")
    private List<String> keys;
    
    /**
     * 查询选项配置
     * <p>包含超时设置、元数据控制、统计信息等选项</p>
     */
    private QueryOptions options;
    
    /**
     * 默认构造函数
     * <p>初始化默认的查询选项</p>
     */
    public FeatureQueryRequest() {
        this.options = new QueryOptions();
    }
    
    /**
     * 单个特征查询构造函数
     * 
     * @param key 特征Key，不能为空
     * @throws IllegalArgumentException 如果key为空
     */
    public FeatureQueryRequest(String key) {
        this();
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Feature key cannot be null or empty");
        }
        this.key = key;
    }
    
    /**
     * 批量特征查询构造函数
     * 
     * @param keys 特征Key列表，不能为空或null
     * @throws IllegalArgumentException 如果keys为空或null
     */
    public FeatureQueryRequest(List<String> keys) {
        this();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Feature keys cannot be null or empty");
        }
        this.keys = keys;
    }
    
    /**
     * 获取单个查询的特征Key
     * 
     * @return 特征Key，可能为null（批量查询模式下）
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 设置单个查询的特征Key
     * 
     * @param key 特征Key，设置后将清空keys字段
     */
    public void setKey(String key) {
        this.key = key;
        if (key != null) {
            this.keys = null; // 单个查询模式，清空批量查询字段
        }
    }
    
    /**
     * 获取批量查询的特征Key列表
     * 
     * @return 特征Key列表，可能为null（单个查询模式下）
     */
    public List<String> getKeys() {
        return keys;
    }
    
    /**
     * 设置批量查询的特征Key列表
     * 
     * @param keys 特征Key列表，设置后将清空key字段
     */
    public void setKeys(List<String> keys) {
        this.keys = keys;
        if (keys != null && !keys.isEmpty()) {
            this.key = null; // 批量查询模式，清空单个查询字段
        }
    }
    
    /**
     * 获取查询选项配置
     * 
     * @return 查询选项，不会为null
     */
    public QueryOptions getOptions() {
        return options;
    }
    
    /**
     * 设置查询选项配置
     * 
     * @param options 查询选项，如果为null将使用默认选项
     */
    public void setOptions(QueryOptions options) {
        this.options = options != null ? options : new QueryOptions();
    }
    
    /**
     * 判断是否为批量查询模式
     * 
     * @return true 如果是批量查询，false 如果是单个查询
     */
    public boolean isBatchQuery() {
        return keys != null && !keys.isEmpty();
    }
    
    /**
     * 查询选项配置类
     * <p>
     * 包含查询过程中的各种选项和参数，用于控制查询行为和返回内容。
     * </p>
     * 
     * <h3>主要选项:</h3>
     * <ul>
     *   <li>超时控制: 设置查询超时时间</li>
     *   <li>元数据控制: 是否返回特征元数据信息</li>
     *   <li>统计信息: 记录客户端IP和用户ID用于统计</li>
     * </ul>
     * 
     * @author FeatureHub Team
     * @version 1.0.0
     * @since 1.0.0
     */
    public static class QueryOptions {
        
        /**
         * 是否返回元数据信息
         * <p>如果为true，响应中将包含特征的元数据（如访问次数、存储类型等）</p>
         */
        private boolean includeMetadata = false;
        
        /**
         * 查询超时时间（毫秒）
         * <p>超过此时间未返回结果将抛出超时异常，默认5秒</p>
         */
        private Long timeoutMs = 5000L;
        
        /**
         * 客户端IP地址
         * <p>用于访问统计和安全审计，通常从HTTP请求头中获取</p>
         */
        private String clientIp;
        
        /**
         * 用户ID
         * <p>用于用户级别的访问统计和个性化服务</p>
         */
        private String userId;
        
        /**
         * 获取是否包含元数据信息
         * 
         * @return true 如果包含元数据，false 否则
         */
        public boolean isIncludeMetadata() {
            return includeMetadata;
        }
        
        /**
         * 设置是否包含元数据信息
         * 
         * @param includeMetadata true 包含元数据，false 不包含（推荐，可提高性能）
         */
        public void setIncludeMetadata(boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
        }
        
        /**
         * 获取查询超时时间
         * 
         * @return 超时时间（毫秒），默认5000毫秒
         */
        public Long getTimeoutMs() {
            return timeoutMs;
        }
        
        /**
         * 设置查询超时时间
         * 
         * @param timeoutMs 超时时间（毫秒），建议设置合理的值避免长时间等待
         * @throws IllegalArgumentException 如果超时时间小于等于0
         */
        public void setTimeoutMs(Long timeoutMs) {
            if (timeoutMs != null && timeoutMs <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutMs = timeoutMs;
        }
        
        /**
         * 获取客户端IP地址
         * 
         * @return 客户端IP，可能为null
         */
        public String getClientIp() {
            return clientIp;
        }
        
        /**
         * 设置客户端IP地址
         * 
         * @param clientIp 客户端IP地址，用于统计和审计
         */
        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }
        
        /**
         * 获取用户ID
         * 
         * @return 用户ID，可能为null
         */
        public String getUserId() {
            return userId;
        }
        
        /**
         * 设置用户ID
         * 
         * @param userId 用户ID，用于用户级别的统计分析
         */
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        /**
         * 创建一个快速查询选项（无元数据，短超时）
         * 
         * @return 快速查询选项实例
         */
        public static QueryOptions fastQuery() {
            QueryOptions options = new QueryOptions();
            options.setIncludeMetadata(false);
            options.setTimeoutMs(1000L);
            return options;
        }
        
        /**
         * 创建一个详细查询选项（包含元数据，较长超时）
         * 
         * @return 详细查询选项实例
         */
        public static QueryOptions detailedQuery() {
            QueryOptions options = new QueryOptions();
            options.setIncludeMetadata(true);
            options.setTimeoutMs(10000L);
            return options;
        }
    }
} 