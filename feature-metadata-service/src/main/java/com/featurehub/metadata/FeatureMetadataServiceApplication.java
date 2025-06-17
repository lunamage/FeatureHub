package com.featurehub.metadata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 特征元数据服务启动类
 * <p>
 * 负责启动特征元数据管理服务，提供特征元数据的CRUD操作、统计分析和缓存管理功能。
 * 该服务是特征平台的核心组件之一，维护着所有特征的元信息。
 * </p>
 * 
 * <h3>服务功能:</h3>
 * <ul>
 *   <li>特征元数据的增删改查</li>
 *   <li>特征访问统计和分析</li>
 *   <li>过期数据清理</li>
 *   <li>Redis缓存管理</li>
 *   <li>健康检查和监控</li>
 * </ul>
 * 
 * <h3>技术栈:</h3>
 * <ul>
 *   <li>Spring Boot: 应用框架</li>
 *   <li>MyBatis: 数据持久化</li>
 *   <li>Redis: 缓存管理</li>
 *   <li>MySQL: 元数据存储</li>
 * </ul>
 * 
 * <h3>端口配置:</h3>
 * <p>默认服务端口: 8080</p>
 * <p>可通过application.yml中的server.port配置修改</p>
 * 
 * <h3>启动示例:</h3>
 * <pre>{@code
 * // 直接启动
 * java -jar feature-metadata-service.jar
 * 
 * // 指定配置文件启动  
 * java -jar feature-metadata-service.jar --spring.config.location=classpath:application-prod.yml
 * 
 * // 指定端口启动
 * java -jar feature-metadata-service.jar --server.port=8081
 * }</pre>
 * 
 * @author FeatureHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.featurehub.metadata.controller.MetadataController
 * @see com.featurehub.metadata.service.MetadataService
 */
@SpringBootApplication
@MapperScan("com.featurehub.metadata.mapper")
public class FeatureMetadataServiceApplication {

    /**
     * 应用程序主入口方法
     * <p>
     * 启动Spring Boot应用，初始化所有必要的组件和配置。
     * 应用启动后将监听HTTP请求，提供RESTful API服务。
     * </p>
     * 
     * @param args 命令行参数，支持Spring Boot标准参数
     *             <ul>
     *               <li>--server.port: 指定服务端口</li>
     *               <li>--spring.profiles.active: 指定激活的配置文件</li>
     *               <li>--spring.config.location: 指定配置文件位置</li>
     *             </ul>
     * 
     * @throws Exception 如果应用启动失败
     * 
     * @see SpringApplication#run(Class, String...)
     */
    public static void main(String[] args) {
        SpringApplication.run(FeatureMetadataServiceApplication.class, args);
    }
} 