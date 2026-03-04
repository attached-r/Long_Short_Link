package rj.highlink.common.config;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/*
 * Redis 核心配置类：
 * 1. 自定义 RedisTemplate（解决默认序列化乱码问题）
 * 2. 初始化 RedissonClient（封装 Redis 连接）
 * 3. 初始化布隆过滤器 RBloomFilter（用于短链去重/防穿透）
 */

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:}")
    private int redisDatabase;

    /**
     * 自定义 RedisTemplate：
     * - 键：String 序列化（避免乱码）
     * - 值：JSON 序列化（支持对象存储，可读性好）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 初始化 RedissonClient：
     * Redisson 是 Redis 的分布式客户端，提供布隆过滤器、分布式锁等高级功能
     */

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机模式（集群/哨兵模式可参考 Redisson 官方文档修改）
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort) // Redis 地址
                .setPassword(redisPassword.isEmpty() ? null : redisPassword) // 密码（无密码则传 null）
                .setDatabase(redisDatabase) // 使用第 0 个数据库
                .setConnectionPoolSize(10) // 连接池大小
                .setConnectionMinimumIdleSize(5); // 最小空闲连接数

        // 创建 Redisson 客户端
        return Redisson.create(config);
    }

    /**
     * 初始化布隆过滤器 RBloomFilter：
     * 用于短链场景：过滤已存在的短码/防恶意请求穿透到数据库
     * 核心参数：
     * - expectedInsertions：预计插入的元素数量（比如 100 万条短链）
     * - falseProbability：误判率（越小越精准，但占用内存越大，推荐 0.01）
     */
    @Bean
    public RBloomFilter<String> shortLinkBloomFilter(RedissonClient redissonClient) {
        // 布隆过滤器名称（唯一标识）
        String filterName = "short_link_bloom_filter";
        // 1. 获取/创建布隆过滤器
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);

        // 2. 初始化布隆过滤器（仅第一次初始化时生效，重复调用不会覆盖）
        // 预计插入 100 万条数据，误判率 1%

        // 这里可以提高初始化速度，但占用内存会增大，学习使用就不做优化
        bloomFilter.tryInit(1_000_000L, 0.01);

        return bloomFilter;
    }
}
