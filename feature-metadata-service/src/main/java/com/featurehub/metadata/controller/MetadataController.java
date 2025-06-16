package com.featurehub.metadata.controller;

import com.featurehub.common.domain.FeatureMetadata;
import com.featurehub.metadata.service.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据管理控制器
 * 提供元数据的CRUD操作接口
 */
@RestController
@RequestMapping("/api/v1/metadata")
@Validated
public class MetadataController {

    private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

    @Autowired
    private MetadataService metadataService;

    /**
     * 获取单个元数据
     * GET /api/v1/metadata/{key}
     */
    @GetMapping("/{key}")
    public ResponseEntity<FeatureMetadata> getMetadata(@PathVariable @NotBlank String key) {
        try {
            FeatureMetadata metadata = metadataService.getMetadata(key);
            if (metadata != null) {
                return ResponseEntity.ok(metadata);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get metadata for key: {}", key, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 批量获取元数据
     * POST /api/v1/metadata/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> getBatchMetadata(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) request.get("keys");
            
            if (keys == null || keys.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<FeatureMetadata> metadataList = metadataService.getBatchMetadata(keys);
            
            Map<String, Object> response = new HashMap<>();
            response.put("metadata", metadataList);
            response.put("total", metadataList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get batch metadata", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 创建或更新元数据
     * POST /api/v1/metadata
     */
    @PostMapping
    public ResponseEntity<Void> upsertMetadata(@Valid @RequestBody FeatureMetadata metadata) {
        try {
            boolean isNew = metadataService.upsertMetadata(metadata);
            if (isNew) {
                return ResponseEntity.status(201).build(); // Created
            } else {
                return ResponseEntity.ok().build(); // Updated
            }
        } catch (Exception e) {
            logger.error("Failed to upsert metadata for key: {}", metadata.getKeyName(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 更新元数据
     * PUT /api/v1/metadata/{key}
     */
    @PutMapping("/{key}")
    public ResponseEntity<Void> updateMetadata(@PathVariable @NotBlank String key, 
                                             @Valid @RequestBody FeatureMetadata metadata) {
        try {
            // 确保路径参数中的key与请求体中的key一致
            metadata.setKeyName(key);
            
            boolean updated = metadataService.updateMetadata(metadata);
            if (updated) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to update metadata for key: {}", key, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 批量更新元数据
     * PUT /api/v1/metadata/batch
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateMetadata(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updates = (List<Map<String, Object>>) request.get("updates");
            
            if (updates == null || updates.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Boolean> results = metadataService.batchUpdateMetadata(updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("total", results.size());
            response.put("successful", results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to batch update metadata", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 删除元数据
     * DELETE /api/v1/metadata/{key}
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteMetadata(@PathVariable @NotBlank String key) {
        try {
            boolean deleted = metadataService.deleteMetadata(key);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to delete metadata for key: {}", key, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取统计信息
     * GET /api/v1/metadata/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(value = "storage_type", required = false) String storageType,
            @RequestParam(value = "business_tag", required = false) String businessTag) {
        try {
            Map<String, Object> stats = metadataService.getStats(storageType, businessTag);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to get metadata stats", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 清理过期元数据
     * POST /api/v1/metadata/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredMetadata() {
        try {
            int cleanedCount = metadataService.cleanupExpiredMetadata();
            
            Map<String, Object> response = new HashMap<>();
            response.put("cleaned_count", cleanedCount);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to cleanup expired metadata", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 健康检查
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> healthInfo = metadataService.getHealthInfo();
            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            logger.error("Health check failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "unhealthy");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 