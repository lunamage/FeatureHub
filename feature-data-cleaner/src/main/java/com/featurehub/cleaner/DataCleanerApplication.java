package com.featurehub.cleaner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据清理服务启动类
 * <p>
 * 负责特征平台中过期和无效数据的自动清理，维护系统的存储效率和数据质量。
 * 定期扫描和清理过期特征数据、无效元数据记录、历史迁移记录等，
 * 确保系统存储空间的合理利用和查询性能的稳定。
 * </p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>过期数据清理 - 清除超过TTL的特征数据</li>
 *   <li>孤儿数据清理 - 清除没有对应元数据的特征数据</li>
 *   <li>元数据清理 - 清除无效的元数据记录</li>
 *   <li>日志清理 - 清理过期的访问日志和迁移记录</li>
 *   <li>统计清理 - 清理历史统计数据</li>
 * </ul>
 * 
 * <h3>清理策略:</h3>
 * <ul>
 *   <li>基于TTL - 根据设置的过期时间清理数据</li>
 *   <li>基于时间 - 清理超过保留期限的历史数据</li>
 *   <li>基于大小 - 当存储空间不足时触发清理</li>
 *   <li>基于访问 - 清理长期未访问的数据</li>
 * </ul>
 * 
 * <h3>清理范围:</h3>
 * <ul>
 *   <li>Redis特征数据 - 过期的热数据</li>
 *   <li>KeeWiDB特征数据 - 过期的冷数据</li>
 *   <li>元数据记录 - 无效的元数据条目</li>
 *   <li>迁移记录 - 历史迁移任务记录</li>
 *   <li>查询日志 - 过期的访问日志</li>
 * </ul>
 * 
 * <h3>安全保障:</h3>
 * <ul>
 *   <li>分批清理 - 避免大批量删除影响性能</li>
 *   <li>确认机制 - 多重检查确保数据安全</li>
 *   <li>回滚支持 - 支持清理操作的回滚</li>
 *   <li>白名单保护 - 保护关键数据不被误删</li>
 * </ul>
 * 
 * <h3>技术组件:</h3>
 * <ul>
 *   <li>Spring Boot - 微服务框架</li>
 *   <li>Spring Scheduling - 定时任务调度</li>
 *   <li>Redis Client - Redis数据操作</li>
 *   <li>KeeWiDB Client - KeeWiDB数据操作</li>
 *   <li>Metadata Client - 元数据服务调用</li>
 *   <li>Event Publisher - 清理事件发布</li>
 * </ul>
 * 
 * <h3>监控指标:</h3>
 * <ul>
 *   <li>清理数据量 - 每次清理的数据条数</li>
 *   <li>清理耗时 - 清理任务的执行时间</li>
 *   <li>存储释放量 - 释放的存储空间大小</li>
 *   <li>错误率 - 清理过程中的错误比例</li>
 * </ul>
 * 
 * <h3>配置参数:</h3>
 * <p>默认端口: 8083</p>
 * <p>清理任务间隔: 可配置（默认每天凌晨）</p>
 * <p>批量清理大小: 可配置（默认500条）</p>
 * <p>数据保留期: 可配置（默认30天）</p>
 * 
 * <h3>启动示例:</h3>
 * <pre>{@code
 * // 标准启动
 * java -jar feature-data-cleaner.jar
 * 
 * // 生产环境启动（自定义清理策略）
 * java -Xms512m -Xmx1g \
 *      -jar feature-data-cleaner.jar \
 *      --spring.profiles.active=prod \
 *      --cleaner.batch-size=200 \
 *      --cleaner.retention-days=60 \
 *      --cleaner.schedule.cron="0 2 * * * ?"
 * 
 * // 立即执行清理（一次性任务）
 * java -jar feature-data-cleaner.jar \
 *      --cleaner.run-once=true \
 *      --cleaner.dry-run=true
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.featurehub.cleaner.service.DataCleanerService
 * @see com.featurehub.cleaner.domain.CleanupRecord
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DataCleanerApplication {

    /**
     * 数据清理服务主入口方法
     * <p>
     * 启动数据清理服务，初始化清理调度器、存储客户端连接、
     * 清理策略配置等核心组件，开始执行定期的数据清理任务。
     * </p>
     * 
     * <h4>启动初始化过程:</h4>
     * <ol>
     *   <li>加载清理策略和规则配置</li>
     *   <li>初始化Redis和KeeWiDB客户端</li>
     *   <li>建立元数据服务连接</li>
     *   <li>配置定时清理任务调度</li>
     *   <li>初始化清理监控和统计</li>
     *   <li>启动HTTP管理接口</li>
     * </ol>
     * 
     * <h4>清理任务调度:</h4>
     * <p>服务启动后将根据配置自动执行以下清理任务：</p>
     * <ul>
     *   <li>每日凌晨2点 - 执行常规数据清理</li>
     *   <li>每周日凌晨3点 - 执行深度清理</li>
     *   <li>存储空间告警时 - 执行紧急清理</li>
     * </ul>
     * 
     * <h4>安全检查:</h4>
     * <p>清理服务在执行前会进行以下安全检查：</p>
     * <ul>
     *   <li>验证存储服务连接状态</li>
     *   <li>检查元数据服务可用性</li>
     *   <li>确认清理白名单配置</li>
     *   <li>验证清理规则的合法性</li>
     * </ul>
     * 
     * @param args 命令行参数，支持清理相关配置：
     *             <ul>
     *               <li>--server.port - 服务端口（默认8083）</li>
     *               <li>--cleaner.auto-enabled - 是否启用自动清理（默认true）</li>
     *               <li>--cleaner.batch-size - 批量清理大小（默认500）</li>
     *               <li>--cleaner.retention-days - 数据保留天数（默认30）</li>
     *               <li>--cleaner.schedule.cron - 清理任务Cron表达式</li>
     *               <li>--cleaner.dry-run - 是否为演练模式（只检查不删除）</li>
     *               <li>--cleaner.run-once - 是否为一次性执行模式</li>
     *               <li>--cleaner.whitelist - 清理白名单配置文件路径</li>
     *             </ul>
     * 
     * @throws Exception 启动失败的可能原因：
     *                   <ul>
     *                     <li>存储服务连接失败</li>
     *                     <li>元数据服务不可用</li>
     *                     <li>清理配置文件错误</li>
     *                     <li>权限不足（删除权限）</li>
     *                     <li>端口占用</li>
     *                     <li>白名单配置文件缺失</li>
     *                   </ul>
     * 
     * @apiNote 建议在生产环境中启用dry-run模式进行清理策略验证
     * @see SpringApplication#run(Class, String...)
     */
    public static void main(String[] args) {
        SpringApplication.run(DataCleanerApplication.class, args);
    }
} 