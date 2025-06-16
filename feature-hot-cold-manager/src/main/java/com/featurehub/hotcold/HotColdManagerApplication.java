package com.featurehub.hotcold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 冷热数据管理服务启动类
 * 负责特征数据在Redis和KeeWiDB之间的智能迁移
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HotColdManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotColdManagerApplication.class, args);
    }
} 