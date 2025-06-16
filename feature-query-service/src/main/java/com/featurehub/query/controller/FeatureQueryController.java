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
 * 提供统一的特征查询REST接口
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
     * GET /api/v1/feature/{key}
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
     * POST /api/v1/features/batch
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
     * 特征写入
     * PUT /api/v1/feature/{key}
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
     * 健康检查
     * GET /api/v1/health
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
     * 获取监控指标
     * GET /api/v1/metrics
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
     * 获取客户端IP地址
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
     * 创建错误响应
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
     * 创建错误映射
     */
    private Map<String, Object> createErrorMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
} 