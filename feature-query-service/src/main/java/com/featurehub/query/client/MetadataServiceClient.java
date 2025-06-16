package com.featurehub.query.client;

import com.featurehub.common.domain.FeatureMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据服务客户端
 * 负责与元数据服务进行HTTP通信
 */
@Component
public class MetadataServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MetadataServiceClient.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${featurehub.metadata-service.base-url:http://localhost:8081}")
    private String metadataServiceBaseUrl;

    @Value("${featurehub.metadata-service.timeout:5000}")
    private long timeoutMs;

    private WebClient webClient;

    /**
     * 获取WebClient实例
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(metadataServiceBaseUrl)
                    .build();
        }
        return webClient;
    }

    /**
     * 获取单个Key的元数据
     */
    public FeatureMetadata getMetadata(String key) {
        try {
            ResponseEntity<FeatureMetadata> response = getWebClient()
                    .get()
                    .uri("/api/v1/metadata/{key}", key)
                    .retrieve()
                    .toEntity(FeatureMetadata.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else if (response != null && response.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("Metadata not found for key: {}", key);
                return null;
            } else {
                logger.warn("Unexpected response from metadata service for key {}: {}", 
                           key, response != null ? response.getStatusCode() : "null");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting metadata for key: {}", key, e);
            return null;
        }
    }

    /**
     * 批量获取多个Key的元数据
     */
    public Map<String, FeatureMetadata> getBatchMetadata(List<String> keys) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("keys", keys);

            ResponseEntity<String> response = getWebClient()
                    .post()
                    .uri("/api/v1/metadata/batch")
                    .bodyValue(requestBody)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                // 解析响应
                Map<String, Object> responseBody = objectMapper.readValue(
                        response.getBody(), new TypeReference<Map<String, Object>>() {});
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> metadataList = 
                    (List<Map<String, Object>>) responseBody.get("metadata");

                Map<String, FeatureMetadata> result = new HashMap<>();
                if (metadataList != null) {
                    for (Map<String, Object> metadataMap : metadataList) {
                        FeatureMetadata metadata = objectMapper.convertValue(metadataMap, FeatureMetadata.class);
                        result.put(metadata.getKeyName(), metadata);
                    }
                }
                return result;
            } else {
                logger.warn("Unexpected response from metadata service for batch query: {}", 
                           response != null ? response.getStatusCode() : "null");
                return new HashMap<>();
            }
        } catch (Exception e) {
            logger.error("Error getting batch metadata for keys: {}", keys, e);
            return new HashMap<>();
        }
    }

    /**
     * 更新元数据
     */
    public boolean updateMetadata(FeatureMetadata metadata) {
        try {
            ResponseEntity<Void> response = getWebClient()
                    .put()
                    .uri("/api/v1/metadata/{key}", metadata.getKeyName())
                    .bodyValue(metadata)
                    .retrieve()
                    .toEntity(Void.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            return response != null && response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Error updating metadata for key: {}", metadata.getKeyName(), e);
            return false;
        }
    }

    /**
     * 创建或更新元数据（Upsert）
     */
    public boolean upsertMetadata(FeatureMetadata metadata) {
        try {
            ResponseEntity<Void> response = getWebClient()
                    .post()
                    .uri("/api/v1/metadata")
                    .bodyValue(metadata)
                    .retrieve()
                    .toEntity(Void.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            return response != null && 
                   (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error upserting metadata for key: {}", metadata.getKeyName(), e);
            return false;
        }
    }

    /**
     * 批量更新元数据
     */
    public Map<String, Boolean> batchUpdateMetadata(List<FeatureMetadata> metadataList) {
        Map<String, Boolean> results = new HashMap<>();
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("updates", metadataList);

            ResponseEntity<String> response = getWebClient()
                    .put()
                    .uri("/api/v1/metadata/batch")
                    .bodyValue(requestBody)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                // 假设所有更新成功
                for (FeatureMetadata metadata : metadataList) {
                    results.put(metadata.getKeyName(), true);
                }
            } else {
                // 如果批量更新失败，回退到单个更新
                for (FeatureMetadata metadata : metadataList) {
                    boolean success = updateMetadata(metadata);
                    results.put(metadata.getKeyName(), success);
                }
            }
        } catch (Exception e) {
            logger.error("Error in batch metadata update", e);
            // 如果批量更新异常，回退到单个更新
            for (FeatureMetadata metadata : metadataList) {
                boolean success = updateMetadata(metadata);
                results.put(metadata.getKeyName(), success);
            }
        }
        
        return results;
    }

    /**
     * 删除元数据
     */
    public boolean deleteMetadata(String key) {
        try {
            ResponseEntity<Void> response = getWebClient()
                    .delete()
                    .uri("/api/v1/metadata/{key}", key)
                    .retrieve()
                    .toEntity(Void.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            return response != null && response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Error deleting metadata for key: {}", key, e);
            return false;
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = getWebClient()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(Map.class)
                    .timeout(Duration.ofMillis(2000)) // 健康检查使用较短超时
                    .block();

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> healthInfo = response.getBody();
                return healthInfo != null && "healthy".equals(healthInfo.get("status"));
            }
            return false;
        } catch (Exception e) {
            logger.warn("Metadata service health check failed", e);
            return false;
        }
    }

    /**
     * 获取服务信息
     */
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "metadata-service");
        info.put("base_url", metadataServiceBaseUrl);
        info.put("timeout_ms", timeoutMs);
        info.put("healthy", isHealthy());
        return info;
    }
} 