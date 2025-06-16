package com.featurehub.metadata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 元数据服务启动类
 */
@SpringBootApplication
@MapperScan("com.featurehub.metadata.mapper")
public class FeatureMetadataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureMetadataServiceApplication.class, args);
    }
} 