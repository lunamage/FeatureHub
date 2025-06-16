package com.featurehub.query.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis配置类
 * 配置Redis和KeeWiDB连接
 */
@Configuration
public class RedisConfig {

    // Redis配置
    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    // KeeWiDB配置
    @Value("${featurehub.keewidb.host:localhost}")
    private String keewidbHost;

    @Value("${featurehub.keewidb.port:6380}")
    private int keewidbPort;

    @Value("${featurehub.keewidb.password:}")
    private String keewidbPassword;

    @Value("${featurehub.keewidb.database:0}")
    private int keewidbDatabase;

    // 连接池配置
    @Value("${spring.redis.jedis.pool.max-active:100}")
    private int maxActive;

    @Value("${spring.redis.jedis.pool.max-idle:50}")
    private int maxIdle;

    @Value("${spring.redis.jedis.pool.min-idle:10}")
    private int minIdle;

    @Value("${spring.redis.jedis.pool.max-wait:3000}")
    private long maxWaitMillis;

    /**
     * Jedis连接池配置
     */
    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWaitMillis);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        return config;
    }

    /**
     * Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        config.setDatabase(redisDatabase);

        JedisConnectionFactory factory = new JedisConnectionFactory(config, jedisPoolConfig());
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * KeeWiDB连接工厂
     */
    @Bean
    @Qualifier("keewidbConnectionFactory")
    public RedisConnectionFactory keewidbConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(keewidbHost);
        config.setPort(keewidbPort);
        if (!keewidbPassword.isEmpty()) {
            config.setPassword(keewidbPassword);
        }
        config.setDatabase(keewidbDatabase);

        JedisConnectionFactory factory = new JedisConnectionFactory(config, jedisPoolConfig());
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Redis模板
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * KeeWiDB Redis模板
     */
    @Bean
    @Qualifier("keewidbRedisTemplate")
    public RedisTemplate<String, String> keewidbRedisTemplate(
            @Qualifier("keewidbConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
} 