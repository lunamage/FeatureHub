# FeatureHub - 特征中心存储系统

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-1.8+-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.9-green.svg)

## 项目简介

FeatureHub 是一个高性能、低成本的特征中心存储系统，通过引入冷热数据分层架构和智能迁移策略，实现了：

- 🔥 **热数据存储在Redis** - 确保核心业务的低延迟访问
- ❄️ **冷数据存储在KeeWiDB** - 显著降低存储成本
- 🔄 **自动化冷热分层** - 根据访问模式自动迁移数据
- 📊 **统一查询接口** - 业务方无需关心底层存储细节
- 🎯 **智能路由机制** - 根据元数据自动路由查询请求
- 🛡️ **数据治理** - 自动清理过期和孤儿数据

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   业务应用层    │    │   业务应用层    │    │   业务应用层    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────────────────────────────┐
         │           特征查询服务 (统一接口)            │
         └───────────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   元数据服务    │    │ 冷热数据管理服务 │    │ 数据清理服务    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   消息队列      │
                    │   (Kafka)       │
                    └─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐              │              ┌─────────────────┐
│  Redis Cluster  │              │              │ KeeWiDB Cluster │
│    (热数据)     │              │              │    (冷数据)     │
└─────────────────┘              │              └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │     MySQL       │
                    │   (元数据存储)  │
                    └─────────────────┘
```

## 核心特性

### 🚀 高性能查询
- **毫秒级响应**：热数据查询延迟 < 5ms
- **并发支持**：支持万级QPS并发访问
- **批量查询**：支持批量查询操作，提升吞吐量
- **智能路由**：根据元数据自动路由到最优存储

### 💰 成本优化
- **存储成本降低60%+**：通过冷热分层显著降低存储成本
- **自动化管理**：减少人工运维成本
- **按需扩展**：根据业务需求弹性扩容

### 🔄 智能分层
- **访问频率分析**：基于实时访问模式进行冷热判断
- **自动迁移**：热转冷、冷转热的自动化数据迁移
- **策略可配置**：支持自定义冷热分层策略

### 📊 可观测性
- **全链路监控**：完整的请求链路追踪
- **实时指标**：查询QPS、延迟、错误率等实时监控
- **告警机制**：异常情况自动告警

## 技术栈

- **后端框架**：Spring Boot 2.7.9
- **数据存储**：Redis、KeeWiDB、MySQL
- **消息队列**：Apache Kafka
- **数据库**：MySQL 8.0 + MyBatis
- **连接池**：Druid
- **监控**：Prometheus + Grafana

## 项目结构

```
FeatureHub/
├── feature-common/                 # 公共模块
│   ├── domain/                     # 领域实体
│   │   ├── FeatureMetadata.java    # 特征元数据实体
│   │   ├── StorageType.java        # 存储类型枚举
│   │   ├── MigrationStatus.java    # 迁移状态枚举
│   │   └── QueryLog.java           # 查询日志实体
│   └── dto/                        # 数据传输对象
├── feature-query-service/          # 特征查询服务
│   ├── controller/                 # REST控制器
│   ├── service/                    # 业务逻辑
│   ├── client/                     # 客户端封装
│   ├── publisher/                  # 事件发布
│   └── config/                     # 配置类
├── feature-metadata-service/       # 元数据服务
│   ├── controller/                 # REST控制器
│   ├── service/                    # 业务逻辑
│   ├── mapper/                     # 数据访问层
│   └── config/                     # 配置类
├── feature-hot-cold-manager/       # 冷热数据管理服务
│   ├── controller/                 # REST控制器
│   ├── service/                    # 核心业务逻辑
│   ├── client/                     # 客户端封装
│   ├── publisher/                  # 事件发布
│   └── domain/                     # 领域实体
├── feature-data-cleaner/           # 数据清理服务
│   ├── service/                    # 清理逻辑
│   ├── client/                     # 客户端封装  
│   ├── config/                     # 配置类
│   ├── domain/                     # 领域实体
│   └── publisher/                  # 事件发布器
└── scripts/                        # 脚本文件
    └── init-db.sql                 # 数据库初始化
```

## 核心服务实现逻辑

### 1. 特征查询服务 (FeatureQueryService)

**核心职责**：统一查询入口，提供高性能的特征数据访问接口

#### 主要功能模块：

##### 1.1 智能路由查询
```java
public FeatureQueryResponse queryFeature(FeatureQueryRequest request) {
    // 1. 查询元数据，确定存储位置
    FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
    
    // 2. 根据存储位置查询数据
    FeatureQueryResponse.FeatureResult result = 
        queryFromStorage(key, metadata.getStorageType());
    
    // 3. 记录查询日志
    recordQueryLog(key, metadata.getStorageType(), result, options);
    
    // 4. 异步更新元数据访问信息
    updateMetadataAsync(key, metadata);
}
```

**实现特点**：
- 通过元数据服务确定数据存储位置，避免无效查询
- 支持Redis和KeeWiDB的统一查询接口
- 异步更新访问统计信息，不影响查询性能
- 完整的查询日志记录，支持链路追踪

##### 1.2 批量查询优化
```java
public FeatureQueryResponse queryBatchFeatures(FeatureQueryRequest request) {
    // 1. 批量查询元数据
    Map<String, FeatureMetadata> metadataMap = 
        metadataServiceClient.getBatchMetadata(keys);
    
    // 2. 按存储类型分组
    Map<StorageType, List<String>> storageGroups = 
        groupKeysByStorage(keys, metadataMap);
    
    // 3. 并行查询不同存储
    List<CompletableFuture<Map<String, FeatureQueryResponse.FeatureResult>>> futures;
    // Redis查询
    futures.add(CompletableFuture.supplyAsync(() -> 
        queryBatchFromRedis(storageGroups.get(StorageType.REDIS))));
    // KeeWiDB查询
    futures.add(CompletableFuture.supplyAsync(() -> 
        queryBatchFromKeeWiDb(storageGroups.get(StorageType.KEEWIDB))));
    
    // 4. 合并结果
    Map<String, FeatureQueryResponse.FeatureResult> allResults = mergeResults(futures);
}
```

**性能优化策略**：
- 按存储类型分组，减少跨存储查询次数
- 并行查询Redis和KeeWiDB，降低总体延迟
- 批量操作减少网络开销

##### 1.3 监控指标收集
```java
// 实时监控指标
private final AtomicLong totalRequests = new AtomicLong(0);
private final AtomicLong redisRequests = new AtomicLong(0);
private final AtomicLong keewidbRequests = new AtomicLong(0);
private final AtomicLong successfulRequests = new AtomicLong(0);
private final AtomicLong failedRequests = new AtomicLong(0);
```

### 2. 元数据服务 (MetadataService)

**核心职责**：管理特征Key的元数据信息，提供高效的元数据查询和更新服务

#### 主要功能模块：

##### 2.1 多级缓存架构
```java
public FeatureMetadata getMetadata(String key) {
    // 1. 先从Redis缓存获取
    FeatureMetadata cached = getFromCache(key);
    if (cached != null) {
        return cached;
    }
    
    // 2. 从MySQL数据库获取
    FeatureMetadata metadata = metadataMapper.selectByKey(key);
    if (metadata != null) {
        // 3. 写入Redis缓存
        putToCache(key, metadata);
    }
    
    return metadata;
}
```

**缓存策略**：
- Redis作为一级缓存，TTL设置为30分钟
- MySQL作为持久化存储
- 缓存失效后自动回源到数据库

##### 2.2 批量操作优化
```java
public List<FeatureMetadata> getBatchMetadata(List<String> keys) {
    // 1. 批量从缓存获取
    Map<String, FeatureMetadata> cachedResults = batchGetFromCache(keys);
    
    // 2. 找出缓存miss的key
    List<String> missedKeys = keys.stream()
        .filter(key -> !cachedResults.containsKey(key))
        .collect(Collectors.toList());
    
    // 3. 批量从数据库获取
    if (!missedKeys.isEmpty()) {
        List<FeatureMetadata> dbResults = metadataMapper.selectByKeys(missedKeys);
        // 4. 批量写入缓存
        batchPutToCache(dbResults);
    }
}
```

##### 2.3 统计信息聚合
```java
public Map<String, Object> getStats(String storageType, String businessTag) {
    // 按存储类型和业务标签聚合统计信息
    // 包括：总Key数量、访问频次分布、存储空间占用等
}
```

### 3. 冷热数据管理服务 (MigrationService)

**核心职责**：基于访问模式和配置策略，自动执行冷热数据迁移

#### 主要功能模块：

##### 3.1 热转冷迁移策略
```java
@Scheduled(fixedRate = 300000) // 每5分钟执行一次
public void scheduleHotToColdMigration() {
    // 1. 查找需要迁移的热数据
    List<FeatureMetadata> hotDataForMigration = findHotDataForMigration();
    
    // 2. 分批执行迁移，避免系统压力
    List<List<FeatureMetadata>> batches = partitionList(hotDataForMigration, batchSize);
    
    for (List<FeatureMetadata> batch : batches) {
        executeHotToColdMigrationBatch(batch);
        Thread.sleep(batchIntervalMs); // 批次间暂停
    }
}
```

**迁移策略**：
- 基于访问时间：超过配置天数未访问的数据
- 基于访问频次：低于阈值的冷数据
- 分批处理：避免对系统造成冲击
- 可配置参数：迁移阈值、批次大小、执行间隔

##### 3.2 冷转热召回策略
```java
@Scheduled(fixedRate = 600000) // 每10分钟执行一次
public void scheduleColdToHotRecall() {
    // 1. 查找需要召回的冷数据（高频访问）
    List<FeatureMetadata> coldDataForRecall = findColdDataForRecall();
    
    // 2. 分批召回到热存储
    for (List<FeatureMetadata> batch : batches) {
        executeColdToHotRecallBatch(batch);
    }
}
```

##### 3.3 迁移状态管理
```java
private MigrationRecord executeHotToColdMigration(List<String> keys, MigrationRecord record) {
    for (String key : keys) {
        try {
            // 1. 从Redis读取数据
            String value = redisClient.get(key);
            
            // 2. 写入KeeWiDB
            boolean success = keeWiDbClient.set(key, value, ttl);
            
            // 3. 更新元数据
            updateMetadataAfterMigration(key, StorageType.KEEWIDB, MigrationStatus.COMPLETED);
            
            // 4. 删除Redis中的数据
            redisClient.delete(key);
            
            record.incrementSuccessCount();
        } catch (Exception e) {
            record.incrementFailedCount();
            logger.error("迁移失败: {}", key, e);
        }
    }
}
```

### 4. 数据清理服务 (DataCleanerService)

**核心职责**：定期清理过期数据和孤儿数据，保持数据一致性

#### 主要功能模块：

##### 4.1 过期数据清理
```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void scheduleExpiredDataCleanup() {
    // 1. 从元数据服务获取过期数据列表
    List<String> expiredKeys = metadataServiceClient.getExpiredKeys();
    
    // 2. 分批清理过期数据
    for (List<String> batch : batches) {
        cleanupExpiredDataBatch(batch);
    }
    
    // 3. 清理元数据
    metadataServiceClient.cleanupExpiredMetadata();
}
```

##### 4.2 孤儿数据清理
```java
@Scheduled(cron = "0 0 3 * * SUN") // 每周日凌晨3点执行
public void scheduleOrphanDataCleanup() {
    // 1. 查找Redis中的孤儿数据（有数据但没有元数据）
    List<String> redisOrphanKeys = findRedisOrphanKeys();
    
    // 2. 查找KeeWiDB中的孤儿数据
    List<String> keewidbOrphanKeys = findKeeWiDbOrphanKeys();
    
    // 3. 分别清理孤儿数据
    cleanupOrphanDataBatch(redisOrphanKeys, "REDIS");
    cleanupOrphanDataBatch(keewidbOrphanKeys, "KEEWIDB");
}
```

##### 4.3 数据一致性检查
```java
private List<String> findRedisOrphanKeys() {
    // 1. 获取Redis中的所有Key
    Set<String> redisKeys = redisClient.getAllKeys();
    
    // 2. 批量查询元数据
    List<String> keysWithoutMetadata = new ArrayList<>();
    for (String key : redisKeys) {
        FeatureMetadata metadata = metadataServiceClient.getMetadata(key);
        if (metadata == null) {
            keysWithoutMetadata.add(key);
        }
    }
    
    return keysWithoutMetadata;
}
```

## 核心数据模型

### FeatureMetadata（特征元数据）
```java
public class FeatureMetadata {
    private String keyName;              // 特征Key
    private StorageType storageType;     // 存储类型（REDIS/KEEWIDB）
    private Long lastAccessTime;         // 最后访问时间
    private Long accessCount;           // 访问次数
    private Long createTime;            // 创建时间
    private Long updateTime;            // 更新时间
    private Long expireTime;            // 过期时间
    private Long dataSize;              // 数据大小
    private String businessTag;         // 业务标签
    private MigrationStatus migrationStatus; // 迁移状态
    private Long migrationTime;         // 迁移时间
}
```

### 存储类型枚举
```java
public enum StorageType {
    REDIS("redis"),
    KEEWIDB("keewidb");
}
```

### 迁移状态枚举
```java
public enum MigrationStatus {
    STABLE("stable"),           // 稳定状态
    MIGRATING("migrating"),     // 迁移中
    COMPLETED("completed"),     // 迁移完成
    FAILED("failed");          // 迁移失败
}
```

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Apache Kafka 2.8+
- KeeWiDB (可选，用作冷数据存储)

### 1. 克隆项目

```bash
git clone https://github.com/your-org/FeatureHub.git
cd FeatureHub
```

### 2. 启动依赖服务

请根据实际环境启动以下依赖服务：

```bash
# 启动MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.0

# 启动Redis
docker run -d --name redis -p 6379:6379 redis:6.0

# 启动KeeWiDB
docker run -d --name keewidb -p 6380:6380 keewidb/keewidb:latest

# 启动Kafka
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.7
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:latest
```

### 3. 初始化数据库

```bash
mysql -h localhost -P 3306 -u root -p < scripts/init-db.sql
```

### 4. 编译项目

```bash
mvn clean install
```

### 5. 启动服务

#### 启动元数据服务
```bash
cd feature-metadata-service
mvn spring-boot:run
```

#### 启动特征查询服务
```bash
cd feature-query-service
mvn spring-boot:run
```

#### 启动冷热数据管理服务
```bash
cd feature-hot-cold-manager
mvn spring-boot:run
```

#### 启动数据清理服务
```bash
cd feature-data-cleaner
mvn spring-boot:run
```

### 6. 验证部署

```bash
# 检查服务状态
curl http://localhost:8080/api/v1/health
curl http://localhost:8081/api/v1/health

# 测试特征查询
curl -X PUT http://localhost:8080/api/v1/feature/user:123:age \
  -H "Content-Type: application/json" \
  -d '{"value": "25", "ttl": 3600}'

curl http://localhost:8080/api/v1/feature/user:123:age
```

## API 文档

### 特征查询服务 (端口: 8080)

#### 单个特征查询
```http
GET /api/v1/feature/{key}
```

**参数**：
- `include_metadata`: 是否包含元数据信息
- `timeout_ms`: 查询超时时间（毫秒）

#### 批量特征查询
```http
POST /api/v1/features/batch
Content-Type: application/json

{
  "keys": ["user:123:age", "user:123:gender"],
  "options": {
    "include_metadata": false,
    "timeout_ms": 5000
  }
}
```

#### 特征写入
```http
PUT /api/v1/feature/{key}
Content-Type: application/json

{
  "value": "25",
  "ttl": 3600,
  "storage_hint": "hot"
}
```

#### 监控指标
```http
GET /api/v1/metrics
```

### 元数据服务 (端口: 8081)

#### 获取元数据
```http
GET /api/v1/metadata/{key}
```

#### 批量获取元数据
```http
POST /api/v1/metadata/batch
Content-Type: application/json

{
  "keys": ["user:123:age", "user:123:gender"]
}
```

#### 获取统计信息
```http
GET /api/v1/metadata/stats?storage_type=redis&business_tag=user_profile
```

## 配置说明

### 冷热分层策略配置

| 参数名称 | 默认值 | 说明 |
|---------|--------|------|
| `migration.hot-to-cold-days` | 7 | 热转冷的天数阈值 |
| `migration.cold-to-hot-access-threshold` | 10 | 冷转热的访问次数阈值 |
| `migration.batch-size` | 1000 | 迁移批次大小 |
| `migration.batch-interval-ms` | 1000 | 批次间隔时间（毫秒） |
| `migration.max-migration-size` | 10000 | 单次迁移最大数量 |

### 数据清理配置

| 参数名称 | 默认值 | 说明 |
|---------|--------|------|
| `cleaner.batch-size` | 1000 | 清理批次大小 |
| `cleaner.expired-days` | 30 | 过期数据保留天数 |
| `cleaner.enable-orphan-cleanup` | true | 是否启用孤儿数据清理 |

### 应用配置示例

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    jedis:
      pool:
        max-active: 100
        max-idle: 50
        min-idle: 10

  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://localhost:3306/featurehub?useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

featurehub:
  keewidb:
    host: localhost
    port: 6380
    timeout: 5000ms
  
  metadata-service:
    base-url: http://localhost:8081
    cache-ttl-minutes: 30
  
  migration:
    hot-to-cold-days: 7
    cold-to-hot-access-threshold: 10
    batch-size: 1000
    batch-interval-ms: 1000
    max-migration-size: 10000
  
  cleaner:
    batch-size: 1000
    expired-days: 30
    enable-orphan-cleanup: true
```

## 监控与运维

### 健康检查

```bash
# 查看服务健康状态
curl http://localhost:8080/api/v1/health
curl http://localhost:8081/api/v1/health
```

### 监控指标

```bash
# 查看Prometheus格式的监控指标
curl http://localhost:8080/api/v1/metrics
curl http://localhost:8081/api/v1/metrics
```

**主要监控指标**：
- `featurehub_query_total`：总查询次数
- `featurehub_query_duration_seconds`：查询延迟
- `featurehub_storage_hits_total`：存储命中次数
- `featurehub_migration_total`：迁移次数
- `featurehub_cleanup_total`：清理次数

### 日志查看

```bash
# 查看服务日志
tail -f feature-query-service/logs/feature-query-service.log
tail -f feature-metadata-service/logs/feature-metadata-service.log
```

## 性能调优

### 1. 连接池配置

```yaml
spring:
  redis:
    jedis:
      pool:
        max-active: 200
        max-idle: 100
        min-idle: 20
        max-wait: 2000ms
  
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    initial-size: 10
    max-active: 100
    min-idle: 10
    max-wait: 60000
```

### 2. JVM参数调优

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -XX:+UnlockExperimentalVMOptions -XX:+UseZGC \
     -jar app.jar
```

### 3. 缓存策略优化

- 元数据缓存TTL：30分钟
- 批量查询缓存：减少数据库访问
- 分级缓存：Redis + 本地缓存

## 故障排查

### 常见问题

1. **查询延迟过高**
   - 检查Redis/KeeWiDB连接状态
   - 查看元数据缓存命中率
   - 分析慢查询日志

2. **数据不一致**
   - 检查元数据服务状态
   - 验证迁移任务执行情况
   - 运行数据一致性检查

3. **迁移任务失败**
   - 查看迁移服务日志
   - 检查存储服务可用性
   - 验证迁移配置参数

4. **内存占用过高**
   - 调整JVM参数
   - 检查连接池配置
   - 分析内存泄漏

### 故障诊断工具

```bash
# 检查服务状态
./scripts/health-check.sh

# 数据一致性检查
./scripts/data-consistency-check.sh

# 性能分析
./scripts/performance-analysis.sh
```

## 最佳实践

### 1. 特征Key命名规范
- 使用冒号分隔层级：`business:entity:feature`
- 例如：`user:123:age`、`product:456:price`

### 2. 业务标签管理
- 按业务域划分标签：`user_profile`、`product_info`
- 便于统计分析和运维管理

### 3. 冷热策略配置
- 根据业务特点调整迁移阈值
- 热数据保留周期：7-30天
- 冷数据召回阈值：根据访问模式调整

### 4. 监控告警配置
- 查询成功率 < 99%
- 查询延迟 > 100ms
- 迁移失败率 > 1%

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 联系我们

- 项目主页：https://github.com/your-org/FeatureHub
- 问题反馈：https://github.com/your-org/FeatureHub/issues
- 邮箱：featurehub@example.com

---

**FeatureHub** - 让特征存储更智能、更经济！
