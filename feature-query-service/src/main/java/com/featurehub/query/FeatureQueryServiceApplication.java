package com.featurehub.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 特征查询服务启动类
 * <p>
 * 特征平台的核心查询服务，提供高性能的特征数据查询和写入接口。
 * 支持智能路由、热冷数据分离、并发优化等特性，是业务系统获取特征数据的主要入口。
 * </p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>特征数据查询 - 单个/批量查询特征值</li>
 *   <li>特征数据写入 - 支持TTL和存储策略</li>
 *   <li>智能路由 - 自动选择Redis或KeeWiDB</li>
 *   <li>访问统计 - 记录访问信息供迁移决策</li>
 *   <li>监控告警 - 提供健康检查和性能指标</li>
 * </ul>
 * 
 * <h3>技术架构:</h3>
 * <ul>
 *   <li>Spring Boot - 微服务框架</li>
 *   <li>Redis - 热数据存储</li>
 *   <li>KeeWiDB - 冷数据存储</li>
 *   <li>Metadata Service - 元数据管理</li>
 *   <li>Event Publisher - 查询日志发布</li>
 * </ul>
 * 
 * <h3>性能优化:</h3>
 * <ul>
 *   <li>连接池复用 - 优化存储连接性能</li>
 *   <li>批量查询 - 减少网络往返次数</li>
 *   <li>异步处理 - 提升并发处理能力</li>
 *   <li>超时控制 - 防止慢查询影响系统</li>
 * </ul>
 * 
 * <h3>部署配置:</h3>
 * <p>默认端口: 8081</p>
 * <p>配置文件: application.yml</p>
 * 
 * <h3>启动方式:</h3>
 * <pre>{@code
 * // 标准启动
 * java -jar feature-query-service.jar
 * 
 * // 生产环境启动（指定配置和JVM参数）
 * java -Xms2g -Xmx4g -XX:+UseG1GC \
 *      -jar feature-query-service.jar \
 *      --spring.profiles.active=prod \
 *      --server.port=8081
 * 
 * // Docker容器启动
 * docker run -p 8081:8081 \
 *      -e SPRING_PROFILES_ACTIVE=prod \
 *      feature-query-service:latest
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.featurehub.query.controller.FeatureQueryController
 * @see com.featurehub.query.service.FeatureQueryService
 */
@SpringBootApplication
@EnableKafka
public class FeatureQueryServiceApplication {

    /**
     * 查询服务主入口方法
     * <p>
     * 启动特征查询服务，初始化所有必要的组件包括：
     * 存储客户端连接池、元数据服务客户端、查询日志发布器、
     * 监控指标收集器等核心组件。
     * </p>
     * 
     * <h4>启动过程:</h4>
     * <ol>
     *   <li>加载配置文件和环境变量</li>
     *   <li>初始化Spring上下文和依赖注入</li>
     *   <li>建立Redis和KeeWiDB连接池</li>
     *   <li>注册元数据服务客户端</li>
     *   <li>启动HTTP服务器监听请求</li>
     *   <li>初始化监控和健康检查</li>
     * </ol>
     * 
     * @param args 命令行参数，支持以下选项：
     *             <ul>
     *               <li>--server.port - 服务端口（默认8081）</li>
     *               <li>--spring.profiles.active - 环境配置（dev/test/prod）</li>
     *               <li>--redis.host - Redis服务地址</li>
     *               <li>--keewidb.host - KeeWiDB服务地址</li>
     *               <li>--metadata.service.url - 元数据服务地址</li>
     *             </ul>
     * 
     * @throws Exception 如果服务启动失败，包括：
     *                   <ul>
     *                     <li>端口占用异常</li>
     *                     <li>存储服务连接失败</li>
     *                     <li>配置文件解析错误</li>
     *                     <li>依赖服务不可用</li>
     *                   </ul>
     * 
     * @apiNote 服务启动后会自动注册到服务发现中心（如果配置了的话）
     * @see SpringApplication#run(Class, String...)
     */
    public static void main(String[] args) {
        SpringApplication.run(FeatureQueryServiceApplication.class, args);
    }
} 