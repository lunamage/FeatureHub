package com.featurehub.cleaner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据清理服务启动类
 * 负责清理过期和无效的特征数据
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DataCleanerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCleanerApplication.class, args);
    }
} 