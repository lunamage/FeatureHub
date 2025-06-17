package com.featurehub.query.controller;

import com.featurehub.common.dto.FeatureQueryRequest;
import com.featurehub.common.dto.FeatureQueryResponse;
import com.featurehub.query.service.FeatureQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 特征查询控制器
 * <p>
 * 提供统一的特征查询REST接口，支持单个特征和批量特征查询。
 * 该控制器是特征平台的核心查询入口，负责处理所有特征数据的读写请求。
 * </p>
 * 
 * <h3>主要功能:</h3>
 * <ul>
 *   <li>单个特征查询 - 根据Key获取特征值</li>
 *   <li>批量特征查询 - 一次查询多个特征</li>
 *   <li>特征数据写入 - 支持设置TTL和存储提示</li>
 *   <li>健康检查和监控 - 提供服务状态监控</li>
 * </ul>
 * 
 * <h3>API路径规划:</h3>
 * <ul>
 *   <li>GET /api/v1/feature/{key} - 单个特征查询</li>
 *   <li>POST /api/v1/features/batch - 批量特征查询</li>
 *   <li>PUT /api/v1/feature/{key} - 特征数据写入</li>
 *   <li>GET /api/v1/health - 健康检查</li>
 *   <li>GET /api/v1/metrics - 监控指标</li>
 * </ul>
 * 
 * <h3>性能特点:</h3>
 * <ul>
 *   <li>智能路由 - 自动选择Redis或KeeWiDB</li>
 *   <li>并发优化 - 支持高并发查询</li>
 *   <li>超时控制 - 可配置查询超时时间</li>
 *   <li>访问统计 - 自动记录访问信息</li>
 * </ul>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FeatureQueryService
 * @see FeatureQueryRequest
 * @see FeatureQueryResponse
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class FeatureQueryController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureQueryController.class);

    @Autowired
    private FeatureQueryService featureQueryService;

    /**
     * 单个特征查询
     * <p>
     * 根据特征Key查询对应的特征值。系统会自动选择最优的存储后端（Redis或KeeWiDB），
     * 并记录访问统计信息用于热冷数据迁移决策。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>
     * GET /api/v1/feature/user:123:profile?include_metadata=true&timeout_ms=3000
     * </pre>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "result": {
     *     "key": "user:123:profile",
     *     "value": "{\"name\":\"张三\",\"age\":25}",
     *     "found": true,
     *     "storageType": "REDIS",
     *     "accessTime": 1634567890000,
     *     "metadata": {
     *       "accessCount": 100,
     *       "dataSize": 1024
     *     }
     *   },
     *   "success": true,
     *   "responseTime": 15
     * }
     * }</pre>
     * 
     * @param key 特征Key，不能为空，格式通常为 "业务:用户ID:特征类型"
     * @param includeMetadata 是否包含元数据信息，默认false，设为true可获取访问统计等信息
     * @param timeoutMs 查询超时时间（毫秒），默认5000毫秒，建议根据业务场景调整
     * @param request HTTP请求对象，用于获取客户端IP等信息
     * @return ResponseEntity 包含查询结果的响应对象
     *         <ul>
     *           <li>200 OK - 查询成功（无论是否找到数据）</li>
     *           <li>400 Bad Request - 参数错误</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 该接口会自动更新特征的访问统计信息，影响热冷数据迁移策略
     * @see FeatureQueryService#queryFeature(FeatureQueryRequest)
     */
    @GetMapping("/feature/{key}")
    public ResponseEntity<FeatureQueryResponse> getFeature(
            @PathVariable @NotBlank String key,
            @RequestParam(value = "include_metadata", defaultValue = "false") boolean includeMetadata,
            @RequestParam(value = "timeout_ms", defaultValue = "5000") Long timeoutMs,
            HttpServletRequest request) {
        
        try {
            // 构建查询请求
            FeatureQueryRequest queryRequest = new FeatureQueryRequest(key);
            queryRequest.getOptions().setIncludeMetadata(includeMetadata);
            queryRequest.getOptions().setTimeoutMs(timeoutMs);
            queryRequest.getOptions().setClientIp(getClientIp(request));

            // 执行查询
            FeatureQueryResponse response = featureQueryService.queryFeature(queryRequest);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to query feature key: {}", key, e);
            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 批量特征查询
     * <p>
     * 一次查询多个特征的值，提高查询效率。系统会并行查询多个特征，
     * 自动优化查询路径，并返回所有查询结果。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>{@code
     * POST /api/v1/features/batch
     * Content-Type: application/json
     * 
     * {
     *   "keys": ["user:123:profile", "user:123:preference", "user:123:history"],
     *   "options": {
     *     "includeMetadata": false,
     *     "timeoutMs": 3000,
     *     "userId": "123"
     *   }
     * }
     * }</pre>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "results": [
     *     {
     *       "key": "user:123:profile",
     *       "value": "{\"name\":\"张三\"}",
     *       "found": true,
     *       "storageType": "REDIS"
     *     },
     *     {
     *       "key": "user:123:preference",
     *       "found": false
     *     }
     *   ],
     *   "success": true,
     *   "responseTime": 25,
     *   "totalCount": 2,
     *   "foundCount": 1
     * }
     * }</pre>
     * 
     * @param request 批量查询请求对象，必须包含keys字段
     * @param httpRequest HTTP请求对象，用于获取客户端信息
     * @return ResponseEntity 包含所有查询结果的响应对象
     *         <ul>
     *           <li>200 OK - 查询成功</li>
     *           <li>400 Bad Request - 请求格式错误或keys为空</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 不存在的特征会在结果中标记为found=false，不会影响其他特征的查询
     * @see FeatureQueryService#queryBatchFeatures(FeatureQueryRequest)
     */
    @PostMapping("/features/batch")
    public ResponseEntity<FeatureQueryResponse> getBatchFeatures(
            @Valid @RequestBody FeatureQueryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 设置客户端IP
            request.getOptions().setClientIp(getClientIp(httpRequest));
            
            // 执行批量查询
            FeatureQueryResponse response = featureQueryService.queryBatchFeatures(request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to query batch features", e);
            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 特征数据写入
     * <p>
     * 向特征平台写入或更新特征数据。支持设置过期时间（TTL）和存储位置提示。
     * 系统会根据提示和当前策略选择合适的存储后端。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>{@code
     * PUT /api/v1/feature/user:123:profile
     * Content-Type: application/json
     * 
     * {
     *   "value": "{\"name\":\"张三\",\"age\":25,\"city\":\"北京\"}",
     *   "ttl": 3600000,
     *   "storage_hint": "redis"
     * }
     * }</pre>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "success": true,
     *   "storageType": "REDIS",
     *   "ttl": 3600000,
     *   "timestamp": 1634567890000
     * }
     * }</pre>
     * 
     * @param key 特征Key，不能为空，路径参数形式
     * @param requestBody 包含特征值和选项的请求体
     *                    <ul>
     *                      <li>value - 特征值（字符串格式，通常为JSON）</li>
     *                      <li>ttl - 过期时间（毫秒），可选</li>
     *                      <li>storage_hint - 存储提示（redis/keewidb），可选</li>
     *                    </ul>
     * @param request HTTP请求对象
     * @return ResponseEntity 包含写入结果的响应对象
     *         <ul>
     *           <li>200 OK - 写入成功</li>
     *           <li>400 Bad Request - 参数错误</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 写入操作会自动创建或更新元数据记录
     * @see FeatureQueryService#putFeature(String, String, Long, String)
     */
    @PutMapping("/feature/{key}")
    public ResponseEntity<Map<String, Object>> putFeature(
            @PathVariable @NotBlank String key,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        
        try {
            String value = (String) requestBody.get("value");
            Long ttl = requestBody.containsKey("ttl") ? 
                Long.valueOf(requestBody.get("ttl").toString()) : null;
            String storageHint = (String) requestBody.get("storage_hint");

            Map<String, Object> result = featureQueryService.putFeature(key, value, ttl, storageHint);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to put feature key: {}", key, e);
            return ResponseEntity.status(500).body(createErrorMap("error", e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     * <p>
     * 检查查询服务的健康状态，包括依赖的存储服务连接状态、
     * 内存使用情况、处理队列状态等关键指标。
     * </p>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "status": "healthy",
     *   "timestamp": 1634567890000,
     *   "redis": {
     *     "status": "connected",
     *     "responseTime": 2
     *   },
     *   "keewidb": {
     *     "status": "connected", 
     *     "responseTime": 15
     *   },
     *   "metadata": {
     *     "status": "connected"
     *   }
     * }
     * }</pre>
     * 
     * @return ResponseEntity 包含健康状态信息
     *         <ul>
     *           <li>200 OK - 服务健康</li>
     *           <li>500 Internal Server Error - 服务异常</li>
     *         </ul>
     * 
     * @apiNote 此接口通常用于负载均衡器的健康检查和监控系统
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> healthInfo = featureQueryService.getHealthInfo();
            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(500).body(createErrorMap("status", "unhealthy"));
        }
    }

    /**
     * 获取服务监控指标
     * <p>
     * 返回查询服务的各项性能指标和统计数据，用于监控和分析。
     * 包括查询QPS、响应时间、缓存命中率等关键指标。
     * </p>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "qps": {
     *     "current": 1250,
     *     "peak": 2100,
     *     "average": 980
     *   },
     *   "responseTime": {
     *     "p95": 25,
     *     "p99": 45,
     *     "average": 12
     *   },
     *   "cacheHitRate": 0.85,
     *   "errorRate": 0.001,
     *   "timestamp": 1634567890000
     * }
     * }</pre>
     * 
     * @return ResponseEntity 包含监控指标数据
     *         <ul>
     *           <li>200 OK - 成功获取指标</li>
     *           <li>500 Internal Server Error - 获取失败</li>
     *         </ul>
     * 
     * @apiNote 指标数据通常每分钟更新一次，用于实时监控和告警
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        try {
            Map<String, Object> metrics = featureQueryService.getMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to get metrics", e);
            return ResponseEntity.status(500).body(createErrorMap("error", e.getMessage()));
        }
    }

    /**
     * 获取客户端真实IP地址
     * <p>
     * 考虑反向代理和负载均衡器的情况，按优先级获取客户端真实IP。
     * 检查顺序：X-Forwarded-For -> X-Real-IP -> RemoteAddr
     * </p>
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址，不会为null
     * 
     * @apiNote 用于访问统计和安全审计
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 创建错误响应对象
     * <p>
     * 统一的错误响应格式，用于异常情况下的响应构建。
     * </p>
     * 
     * @param message 错误信息
     * @return 标准格式的错误响应对象
     */
    private FeatureQueryResponse createErrorResponse(String message) {
        FeatureQueryResponse response = new FeatureQueryResponse();
        FeatureQueryResponse.FeatureResult result = new FeatureQueryResponse.FeatureResult();
        result.setFound(false);
        result.setError(message);
        response.setResult(result);
        return response;
    }

    /**
     * 创建简单的错误映射
     * <p>
     * 用于非标准响应格式的错误信息构建。
     * </p>
     * 
     * @param key 错误字段名
     * @param value 错误值
     * @return 包含错误信息的Map对象
     */
    private Map<String, Object> createErrorMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
} 