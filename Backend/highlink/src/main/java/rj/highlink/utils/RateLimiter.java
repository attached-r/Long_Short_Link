package rj.highlink.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * Redis 限流工具类：基于 Lua 脚本实现令牌桶/固定窗口限流
 * 特点：Lua 脚本保证原子性，避免并发问题
 * 该版本适用于短链系统，可扩展到高并发场景，其中Lua脚本可以后续优化，这里不做更改
 */
@Component
public class RateLimiter {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 固定窗口限流 Lua 脚本（核心：单位时间内请求数不超过阈值）,原子性
    private static final String RATE_LIMIT_LUA_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1]) -- 限流阈值（单位时间内最大请求数）
            local expire = tonumber(ARGV[2]) -- 窗口过期时间（秒）
            
            -- 获取当前窗口的请求数
            local current = tonumber(redis.call('get', key) or "0")
            if current + 1 > limit then
                -- 超过阈值，限流
                return 0
            else
                -- 未超过阈值，请求数+1，设置过期时间
                redis.call('incr', key)
                redis.call('expire', key, expire)
                return 1
            end
            """;

    // 初始化 Lua 脚本对象
    private final DefaultRedisScript<Long> rateLimitScript;

    // 构造方法，初始化 Lua 脚本对象
    public RateLimiter() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(RATE_LIMIT_LUA_SCRIPT); // 设置脚本内容
        rateLimitScript.setResultType(Long.class);
    }
    /**
     * 固定窗口限流方法
     * @param key 限流标识（比如：IP+接口名、用户ID+接口名）
     * @param limit 单位时间内最大请求数（阈值）
     * @param expireSeconds 窗口时间（秒，比如 60=1分钟）
     * @return true=允许访问，false=限流
     */
    public boolean tryAcquire(String key, int limit, int expireSeconds) {
        try {
            // Lua 脚本参数：KEYS 是键列表，ARGV 是参数列表
            // KEYS[1] = "rate_limit:" + key，包装成 Redis 的键
            List<String> keys = Collections.singletonList("rate_limit:" + key);
            // 执行 Lua 脚本
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    keys,
                    String.valueOf(limit),
                    String.valueOf(expireSeconds)
            );
            // result=1 允许访问，result=0 限流
            return result != null && result == 1;
        } catch (Exception e) {
            // 异常时默认放行（避免 Redis 故障导致服务不可用）
            e.printStackTrace();
            return true;
        }
    }

    /**
     * 简化版限流方法（默认 1分钟 最多 60 次请求）
     * @param key 限流标识
     * @return true=允许访问，false=限流
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 60, 60);
    }
}
/*
 * 关键说明：
 * 1.Lua 脚本原子性：整个限流逻辑（查请求数→判断→加 1→设过期）在 Lua 脚本中执行，避免多线程并发下的计数错误；
 * 2.限流 key 设计：建议格式 IP:接口名（比如 127.0.0.1:/api/shortLink/create）或 用户ID:接口名，精准限流；
 * 3.异常容错：Redis 异常时默认放行，避免限流组件故障导致整个服务不可用；
 * 4.固定窗口优势：逻辑简单、性能高，适合短链系统的接口限流（比如创建短链、访问短链接口）。
 */