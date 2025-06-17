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
 * <p>
 * 提供特征元数据的RESTful API接口，包括CRUD操作、批量处理、统计查询和数据清理功能。
 * 所有接口都遵循RESTful设计规范，支持标准的HTTP状态码和错误处理。
 * </p>
 * 
 * <h3>主要接口:</h3>
 * <ul>
 *   <li>GET /api/v1/metadata/{key} - 获取单个元数据</li>
 *   <li>POST /api/v1/metadata/batch - 批量获取元数据</li>
 *   <li>POST /api/v1/metadata - 创建或更新元数据</li>
 *   <li>PUT /api/v1/metadata/{key} - 更新指定元数据</li>
 *   <li>DELETE /api/v1/metadata/{key} - 删除元数据</li>
 *   <li>GET /api/v1/metadata/stats - 获取统计信息</li>
 *   <li>POST /api/v1/metadata/cleanup - 清理过期数据</li>
 * </ul>
 * 
 * <h3>错误处理:</h3>
 * <ul>
 *   <li>400 Bad Request - 请求参数错误</li>
 *   <li>404 Not Found - 资源不存在</li>
 *   <li>500 Internal Server Error - 服务器内部错误</li>
 * </ul>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see MetadataService
 * @see FeatureMetadata
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
     * <p>
     * 根据特征Key获取对应的元数据信息。如果数据存在于缓存中，将直接从缓存返回；
     * 否则从数据库查询并更新缓存。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>
     * GET /api/v1/metadata/user:123:profile
     * </pre>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "keyName": "user:123:profile",
     *   "storageType": "REDIS",
     *   "accessCount": 100,
     *   "lastAccessTime": 1634567890000,
     *   "dataSize": 1024
     * }
     * }</pre>
     * 
     * @param key 特征Key，不能为空或空白字符串
     * @return ResponseEntity 包含元数据信息或404状态码
     *         <ul>
     *           <li>200 OK - 成功返回元数据</li>
     *           <li>404 Not Found - 元数据不存在</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 该接口会更新元数据的访问统计信息
     * @see MetadataService#getMetadata(String)
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
     * <p>
     * 根据提供的特征Key列表批量获取对应的元数据信息。
     * 采用缓存优先策略，优先从缓存获取，缓存未命中的数据再从数据库查询。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>{@code
     * POST /api/v1/metadata/batch
     * Content-Type: application/json
     * 
     * {
     *   "keys": ["user:123:profile", "user:123:preference"]
     * }
     * }</pre>
     * 
     * <h4>响应示例:</h4>
     * <pre>{@code
     * {
     *   "metadata": [
     *     {
     *       "keyName": "user:123:profile",
     *       "storageType": "REDIS",
     *       "accessCount": 100
     *     }
     *   ],
     *   "total": 1
     * }
     * }</pre>
     * 
     * @param request 包含keys字段的请求体，keys不能为空
     * @return ResponseEntity 包含元数据列表和总数
     *         <ul>
     *           <li>200 OK - 成功返回元数据列表</li>
     *           <li>400 Bad Request - 请求参数错误</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 不存在的Key不会包含在响应结果中
     * @see MetadataService#getBatchMetadata(List)
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
     * <p>
     * 如果元数据不存在则创建新记录，如果已存在则更新现有记录。
     * 该操作会自动更新updateTime字段，对于新创建的记录还会设置createTime。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>{@code
     * POST /api/v1/metadata
     * Content-Type: application/json
     * 
     * {
     *   "keyName": "user:123:profile",
     *   "storageType": "REDIS",
     *   "dataSize": 1024,
     *   "businessTag": "user_profile"
     * }
     * }</pre>
     * 
     * @param metadata 元数据对象，必须包含keyName字段
     * @return ResponseEntity 表示操作结果
     *         <ul>
     *           <li>201 Created - 成功创建新元数据</li>
     *           <li>200 OK - 成功更新现有元数据</li>
     *           <li>400 Bad Request - 请求数据无效</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 操作完成后会更新缓存中的数据
     * @see MetadataService#upsertMetadata(FeatureMetadata)
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
     * 更新指定的元数据
     * <p>
     * 更新指定特征Key的元数据信息。与upsert接口不同，此接口只更新现有记录，
     * 如果记录不存在将返回404错误。
     * </p>
     * 
     * <h4>请求示例:</h4>
     * <pre>{@code
     * PUT /api/v1/metadata/user:123:profile
     * Content-Type: application/json
     * 
     * {
     *   "storageType": "KEEWIDB",
     *   "businessTag": "user_profile_archived"
     * }
     * }</pre>
     * 
     * @param key 特征Key，不能为空，必须与请求体中的keyName一致
     * @param metadata 要更新的元数据对象
     * @return ResponseEntity 表示更新结果
     *         <ul>
     *           <li>200 OK - 成功更新元数据</li>
     *           <li>404 Not Found - 元数据不存在</li>
     *           <li>400 Bad Request - 请求数据无效</li>
     *           <li>500 Internal Server Error - 服务器错误</li>
     *         </ul>
     * 
     * @apiNote 路径参数中的key会覆盖请求体中的keyName
     * @see MetadataService#updateMetadata(FeatureMetadata)
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