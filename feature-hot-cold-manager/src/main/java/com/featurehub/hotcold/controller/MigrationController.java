package com.featurehub.hotcold.controller;

import com.featurehub.hotcold.domain.MigrationRecord;
import com.featurehub.hotcold.domain.MigrationTask;
import com.featurehub.hotcold.service.MigrationService;
import com.featurehub.hotcold.service.MigrationStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 冷热数据迁移管理接口
 * 提供手动触发迁移、查询迁移状态等功能
 */
@RestController
@RequestMapping("/api/migration")
@Validated
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private MigrationStatisticsService statisticsService;

    /**
     * 手动触发迁移任务
     */
    @PostMapping("/trigger")
    public ResponseEntity<MigrationRecord> triggerMigration(@Valid @RequestBody MigrationTask task) {
        logger.info("接收到手动迁移请求: {}", task);
        
        try {
            MigrationRecord record = migrationService.triggerMigration(task);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.error("手动迁移任务失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取迁移统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMigrationStatistics(
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String storageType) {
        
        try {
            Map<String, Object> statistics = statisticsService.getMigrationStatistics(timeRange, storageType);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取迁移统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取最近的迁移记录
     */
    @GetMapping("/records")
    public ResponseEntity<List<MigrationRecord>> getRecentMigrationRecords(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        
        try {
            List<MigrationRecord> records = statisticsService.getRecentMigrationRecords(limit, status);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("获取迁移记录失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> health = statisticsService.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取迁移配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMigrationConfig() {
        try {
            Map<String, Object> config = statisticsService.getMigrationConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("获取迁移配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 暂停自动迁移任务
     */
    @PostMapping("/pause")
    public ResponseEntity<String> pauseMigration() {
        try {
            // TODO: 实现暂停逻辑
            return ResponseEntity.ok("迁移任务已暂停");
        } catch (Exception e) {
            logger.error("暂停迁移任务失败", e);
            return ResponseEntity.internalServerError().body("暂停失败: " + e.getMessage());
        }
    }

    /**
     * 恢复自动迁移任务
     */
    @PostMapping("/resume")
    public ResponseEntity<String> resumeMigration() {
        try {
            // TODO: 实现恢复逻辑
            return ResponseEntity.ok("迁移任务已恢复");
        } catch (Exception e) {
            logger.error("恢复迁移任务失败", e);
            return ResponseEntity.internalServerError().body("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 根据业务标签批量迁移
     */
    @PostMapping("/batch/by-tag")
    public ResponseEntity<MigrationRecord> batchMigrationByTag(
            @RequestParam @NotEmpty String businessTag,
            @RequestParam String sourceStorage,
            @RequestParam String targetStorage) {
        
        try {
            // TODO: 实现按业务标签批量迁移
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("按业务标签批量迁移失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 预估迁移成本和时间
     */
    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimateMigration(@Valid @RequestBody MigrationTask task) {
        try {
            Map<String, Object> estimation = statisticsService.estimateMigration(task);
            return ResponseEntity.ok(estimation);
        } catch (Exception e) {
            logger.error("预估迁移成本失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 