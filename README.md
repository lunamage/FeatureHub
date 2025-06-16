# FeatureHub - 特征中心存储系统

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-1.8+-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.9-green.svg)

## 项目简介

FeatureHub 是一个高性能、低成本的特征中心存储系统，通过引入冷热数据分层架构，实现了：

- 🔥 **热数据存储在Redis** - 确保核心业务的低延迟访问
- ❄️ **冷数据存储在KeeWiDB** - 显著降低存储成本
- 🔄 **自动化冷热分层** - 根据访问模式自动迁移数据
- 📊 **统一查询接口** - 业务方无需关心底层存储细节
- 🎯 **智能路由机制** - 根据元数据自动路由查询请求

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
- **监控**：Prometheus + Grafana
- **日志**：ELK Stack (Elasticsearch + Logstash + Kibana)

## 项目结构

```
FeatureHub/
├── feature-common/                 # 公共模块
│   ├── domain/                     # 领域实体
│   └── dto/                        # 数据传输对象
├── feature-query-service/          # 特征查询服务
│   ├── controller/                 # REST控制器
│   ├── service/                    # 业务逻辑
│   ├── client/                     # 客户端封装
│   └── config/                     # 配置类
├── feature-metadata-service/       # 元数据服务
│   ├── controller/                 # REST控制器
│   ├── service/                    # 业务逻辑
│   ├── mapper/                     # 数据访问层
│   └── config/                     # 配置类
├── feature-hot-cold-manager/       # 冷热数据管理服务
│   ├── controller/                 # REST控制器
│   └── service/                    # 核心业务逻辑
├── feature-data-cleaner/           # 数据清理服务
│   ├── client/                     # 客户端封装  
│   ├── config/                     # 配置类
│   ├── domain/                     # 领域实体
│   ├── publisher/                  # 事件发布器
│   └── service/                    # 清理逻辑
└── scripts/                        # 脚本文件
    └── init-db.sql                 # 数据库初始化
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
- MySQL 8.0+
- Redis 6.0+
- KeeWiDB (兼容Redis协议)
- Apache Kafka 2.8+

如需使用Docker启动，可参考以下命令：

```bash
# 启动MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.0

# 启动Redis
docker run -d --name redis -p 6379:6379 redis:6.0

# 启动Kafka (需要先启动Zookeeper)
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.7
docker run -d --name kafka -p 9092:9092 -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 confluentinc/cp-kafka:latest
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
curl http://localhost:8080/health
curl http://localhost:8081/health

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

### 元数据服务 (端口: 8081)

#### 获取元数据
```http
GET /api/v1/metadata/{key}
```

#### 获取统计信息
```http
GET /api/v1/metadata/stats?storage_type=redis
```

## 配置说明

### 冷热分层策略配置

| 参数名称 | 默认值 | 说明 |
|---------|--------|------|
| `hot_to_cold_access_threshold_hours` | 24 | 热转冷访问统计时间窗口（小时） |
| `hot_to_cold_access_count_threshold` | 5 | 热转冷访问次数阈值 |
| `hot_to_cold_idle_days_threshold` | 7 | 热转冷不活跃天数阈值 |
| `cold_to_hot_access_threshold_hours` | 1 | 冷转热访问统计时间窗口（小时） |
| `cold_to_hot_access_count_threshold` | 10 | 冷转热访问次数阈值 |

### 应用配置

主要配置文件位于各服务的 `src/main/resources/application.yml`：

```yaml
# Redis配置
spring:
  redis:
    host: localhost
    port: 6379

# KeeWiDB配置
featurehub:
  keewidb:
    host: localhost
    port: 6380

# 元数据服务配置
featurehub:
  metadata-service:
    base-url: http://localhost:8081
```

## 监控与运维

### 健康检查

```bash
# 查看服务健康状态
curl http://localhost:8080/health
curl http://localhost:8081/health
```

### 监控指标

```bash
# 查看Prometheus格式的监控指标
curl http://localhost:8080/metrics
curl http://localhost:8081/metrics
```

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
        max-active: 100
        max-idle: 50
        min-idle: 10
```

### 2. JVM参数调优

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar app.jar
```

### 3. 数据库优化

- 为常用查询字段添加索引
- 定期清理过期数据
- 使用读写分离

## 故障排查

### 常见问题

1. **连接超时**
   - 检查网络连通性
   - 验证服务端口是否正确

2. **数据不一致**
   - 检查元数据服务状态
   - 验证迁移任务是否正常执行

3. **性能问题**
   - 查看监控指标
   - 检查JVM GC情况
   - 分析慢查询日志

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
