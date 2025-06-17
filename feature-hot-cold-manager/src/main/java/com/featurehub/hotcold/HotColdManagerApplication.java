package com.featurehub.hotcold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 热冷数据管理服务启动类
 * <p>
 * 负责特征数据在Redis（热存储）和KeeWiDB（冷存储）之间的智能迁移管理。
 * 基于访问模式和预设策略，自动执行数据迁移任务，优化存储成本和查询性能。
 * </p>
 * 
 * <h3>核心职责:</h3>
 * <ul>
 *   <li>热冷数据迁移 - 根据访问频率迁移数据</li>
 *   <li>迁移策略管理 - 支持多种迁移触发条件</li>
 *   <li>迁移任务调度 - 定时扫描和执行迁移</li>
 *   <li>迁移监控统计 - 记录迁移进度和结果</li>
 *   <li>回滚和重试 - 处理迁移失败情况</li>
 * </ul>
 * 
 * <h3>迁移策略:</h3>
 * <ul>
 *   <li>基于时间 - 长时间未访问的数据迁移到冷存储</li>
 *   <li>基于频率 - 低频访问数据迁移到冷存储</li>
 *   <li>基于容量 - Redis容量不足时主动迁移</li>
 *   <li>基于成本 - 成本优化驱动的迁移策略</li>
 * </ul>
 * 
 * <h3>技术组件:</h3>
 * <ul>
 *   <li>Spring Boot - 微服务框架</li>
 *   <li>Spring Scheduling - 定时任务调度</li>
 *   <li>MyBatis - 迁移记录持久化</li>
 *   <li>Redis Client - 热数据操作</li>
 *   <li>KeeWiDB Client - 冷数据操作</li>
 *   <li>Metadata Client - 元数据更新</li>
 * </ul>
 * 
 * <h3>性能特点:</h3>
 * <ul>
 *   <li>批量迁移 - 提高迁移效率</li>
 *   <li>分片处理 - 避免大批量操作影响业务</li>
 *   <li>限流控制 - 控制迁移对系统的影响</li>
 *   <li>断点续传 - 支持迁移任务中断恢复</li>
 * </ul>
 * 
 * <h3>配置参数:</h3>
 * <p>默认端口: 8082</p>
 * <p>迁移任务间隔: 可配置（默认每小时）</p>
 * <p>批量大小: 可配置（默认1000条）</p>
 * 
 * <h3>启动示例:</h3>
 * <pre>{@code
 * // 基本启动
 * java -jar feature-hot-cold-manager.jar
 * 
 * // 生产环境启动
 * java -Xms1g -Xmx2g \
 *      -jar feature-hot-cold-manager.jar \
 *      --spring.profiles.active=prod \
 *      --migration.batch-size=500 \
 *      --migration.schedule.interval=30m
 * 
 * // 禁用自动迁移（仅手动触发）
 * java -jar feature-hot-cold-manager.jar \
 *      --migration.auto-enabled=false
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.featurehub.hotcold.service.MigrationService
 * @see com.featurehub.hotcold.controller.MigrationController
 */
@SpringBootApplication
@EnableScheduling
// TODO: 添加 @MapperScan("com.featurehub.hotcold.mapper") 当配置MyBatis依赖后
public class HotColdManagerApplication {

    /**
     * 热冷数据管理服务主入口方法
     * <p>
     * 启动热冷数据管理服务，初始化迁移调度器、存储客户端、
     * 元数据服务连接等核心组件，开始执行数据迁移监控和管理任务。
     * </p>
     * 
     * <h4>启动初始化流程:</h4>
     * <ol>
     *   <li>加载迁移策略配置</li>
     *   <li>初始化Redis和KeeWiDB连接</li>
     *   <li>建立元数据服务连接</li>
     *   <li>启动定时任务调度器</li>
     *   <li>初始化迁移任务监控</li>
     *   <li>注册HTTP接口服务</li>
     * </ol>
     * 
     * <h4>服务就绪检查:</h4>
     * <p>服务启动后会自动检查以下依赖服务的可用性：</p>
     * <ul>
     *   <li>Redis连接状态</li>
     *   <li>KeeWiDB连接状态</li>
     *   <li>元数据服务连通性</li>
     *   <li>数据库连接状态</li>
     * </ul>
     * 
     * @param args 命令行参数，支持迁移相关配置：
     *             <ul>
     *               <li>--server.port - 服务端口（默认8082）</li>
     *               <li>--migration.auto-enabled - 是否启用自动迁移（默认true）</li>
     *               <li>--migration.batch-size - 批量迁移大小（默认1000）</li>
     *               <li>--migration.schedule.interval - 迁移任务间隔</li>
     *               <li>--migration.inactive-threshold - 不活跃阈值（默认7天）</li>
     *               <li>--migration.max-concurrent-tasks - 最大并发迁移任务数</li>
     *             </ul>
     * 
     * @throws Exception 启动失败的可能原因：
     *                   <ul>
     *                     <li>存储服务连接失败</li>
     *                     <li>数据库连接异常</li>
     *                     <li>元数据服务不可用</li>
     *                     <li>配置参数错误</li>
     *                     <li>端口占用</li>
     *                   </ul>
     * 
     * @apiNote 服务启动后，定时迁移任务会根据配置的间隔自动执行
     * @see SpringApplication#run(Class, String...)
     */
    public static void main(String[] args) {
        SpringApplication.run(HotColdManagerApplication.class, args);
    }
} 