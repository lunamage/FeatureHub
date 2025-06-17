# FeatureHub JavaDoc 文档

## 项目概述

FeatureHub 是一个高性能、低成本的特征中心存储系统，通过引入冷热数据分层架构和智能迁移策略，实现对特征数据的高效管理。

## 查看JavaDoc文档

### 在线查看
在浏览器中打开 `javadoc/index.html` 文件即可查看完整的API文档。

### 主要模块说明

#### 1. 公共模块 (com.featurehub.common)
- **domain**: 领域实体类，包含特征元数据、存储类型等核心数据模型
- **dto**: 数据传输对象，用于API请求响应的数据封装

#### 2. 特征查询服务 (com.featurehub.query)
- **controller**: REST API控制器，提供特征查询的HTTP接口
- **service**: 核心业务逻辑服务，处理特征查询和写入操作
- **client**: 客户端封装，统一管理对Redis、KeeWiDB等存储的访问
- **config**: 配置类，管理Redis连接和Web配置
- **publisher**: 事件发布器，将查询日志发送到Kafka

#### 3. 元数据服务 (com.featurehub.metadata)
- **controller**: 元数据管理的REST API控制器
- **service**: 元数据业务逻辑，包含CRUD操作和缓存管理
- **mapper**: 数据访问层，与MySQL数据库交互
- **config**: 数据库配置管理

#### 4. 冷热数据管理服务 (com.featurehub.hotcold)
- **controller**: 数据迁移管理的REST API控制器
- **service**: 数据迁移核心逻辑，包含热转冷和冷转热的策略
- **client**: 外部服务客户端封装
- **config**: 迁移配置管理
- **domain**: 迁移相关的领域实体
- **publisher**: 迁移事件发布器

#### 5. 数据清理服务 (com.featurehub.cleaner)
- **service**: 数据清理业务逻辑，包含过期数据和孤儿数据清理
- **client**: 外部服务客户端封装
- **config**: 清理配置管理
- **domain**: 清理相关的领域实体
- **publisher**: 清理事件发布器

## 核心特性

### 🔥 高性能查询
- 毫秒级响应时间
- 支持万级QPS并发访问
- 批量查询优化
- 智能路由机制

### ❄️ 智能分层存储
- Redis热数据存储
- KeeWiDB冷数据存储
- 自动化冷热迁移
- 按需数据召回

### 📊 数据治理
- 自动清理过期数据
- 孤儿数据检测清理
- 完整的访问日志
- 实时监控指标

## API使用示例

### 查询特征数据
```java
// 单个查询
FeatureQueryRequest request = new FeatureQueryRequest();
request.setKey("user_feature_123");
FeatureQueryResponse response = featureQueryService.queryFeature(request);

// 批量查询
FeatureQueryRequest batchRequest = new FeatureQueryRequest();
batchRequest.setKeys(Arrays.asList("key1", "key2", "key3"));
FeatureQueryResponse batchResponse = featureQueryService.queryBatchFeatures(batchRequest);
```

### 写入特征数据
```java
// 写入热数据
featureQueryService.putFeature("user_feature_123", "feature_value", 3600L, "hot");

// 写入冷数据
featureQueryService.putFeature("user_feature_456", "feature_value", null, "cold");
```

### 元数据管理
```java
// 创建元数据
FeatureMetadata metadata = new FeatureMetadata("user_feature_123");
metadata.setStorageType(StorageType.REDIS);
metadata.setBusinessTag("user_profile");
metadataService.upsertMetadata(metadata);
```

## 技术栈

- **Java版本**: 1.8+
- **Spring Boot**: 2.7.9
- **数据存储**: Redis, KeeWiDB, MySQL
- **消息队列**: Apache Kafka
- **数据库**: MySQL 8.0 + MyBatis
- **连接池**: Druid

## 生成文档

如需重新生成JavaDoc文档，请执行：

```bash
javadoc -d docs/javadoc \
  -sourcepath "feature-common/src/main/java:feature-query-service/src/main/java:feature-metadata-service/src/main/java:feature-hot-cold-manager/src/main/java:feature-data-cleaner/src/main/java" \
  -subpackages com.featurehub \
  -windowtitle "FeatureHub API Documentation" \
  -doctitle "FeatureHub - 特征中心存储系统 API 文档" \
  -author -version -use -encoding UTF-8 -charset UTF-8 -docencoding UTF-8
```

## 联系方式

如需了解更多信息或有任何疑问，请查看项目文档或联系开发团队。 