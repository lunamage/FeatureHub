package com.featurehub.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 特征查询服务启动类
 */
@SpringBootApplication
@EnableKafka
public class FeatureQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureQueryServiceApplication.class, args);
    }
} 