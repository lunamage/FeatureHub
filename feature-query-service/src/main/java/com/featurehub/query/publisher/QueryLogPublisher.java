package com.featurehub.query.publisher;

import com.featurehub.common.domain.QueryLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 查询日志发布器
 * 负责将查询日志异步发送到Kafka
 */
@Component
public class QueryLogPublisher {

    private static final Logger logger = LoggerFactory.getLogger(QueryLogPublisher.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${featurehub.kafka.query-log-topic:feature-query-logs}")
    private String queryLogTopic;

    // 监控指标
    private final AtomicLong totalSent = new AtomicLong(0);
    private final AtomicLong successfulSent = new AtomicLong(0);
    private final AtomicLong failedSent = new AtomicLong(0);

    /**
     * 发布查询日志
     */
    public void publish(QueryLog queryLog) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(queryLog);
            String key = queryLog.getKey(); // 使用特征Key作为Kafka消息的key，保证同一Key的日志有序

            totalSent.incrementAndGet();

            ListenableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(queryLogTopic, key, jsonMessage);

            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    successfulSent.incrementAndGet();
                    logger.debug("Query log sent successfully for key: {} to topic: {} partition: {} offset: {}",
                               queryLog.getKey(), queryLogTopic, 
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    failedSent.incrementAndGet();
                    logger.error("Failed to send query log for key: {} to topic: {}", 
                               queryLog.getKey(), queryLogTopic, throwable);
                }
            });

        } catch (Exception e) {
            failedSent.incrementAndGet();
            logger.error("Error publishing query log for key: {}", queryLog.getKey(), e);
        }
    }

    /**
     * 批量发布查询日志
     */
    public void publishBatch(java.util.List<QueryLog> queryLogs) {
        for (QueryLog queryLog : queryLogs) {
            publish(queryLog);
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            // 发送一个测试消息来检查Kafka连接
            QueryLog testLog = new QueryLog();
            testLog.setKey("health_check_" + System.currentTimeMillis());
            testLog.setTimestamp(System.currentTimeMillis());
            testLog.setSuccess(true);

            String jsonMessage = objectMapper.writeValueAsString(testLog);
            
            ListenableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(queryLogTopic, testLog.getKey(), jsonMessage);
            
            // 等待发送完成（超时2秒）
            future.get(2, java.util.concurrent.TimeUnit.SECONDS);
            
            return true;
        } catch (Exception e) {
            logger.warn("Kafka health check failed", e);
            return false;
        }
    }

    /**
     * 获取发送统计信息
     */
    public java.util.Map<String, Object> getPublishStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("topic", queryLogTopic);
        stats.put("total_sent", totalSent.get());
        stats.put("successful_sent", successfulSent.get());
        stats.put("failed_sent", failedSent.get());
        
        double successRate = totalSent.get() > 0 ? 
            (double) successfulSent.get() / totalSent.get() * 100 : 0;
        stats.put("success_rate_percent", successRate);
        stats.put("healthy", isHealthy());
        
        return stats;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalSent.set(0);
        successfulSent.set(0);
        failedSent.set(0);
        logger.info("Query log publisher stats have been reset");
    }

    /**
     * 获取主题名称
     */
    public String getTopicName() {
        return queryLogTopic;
    }
} 