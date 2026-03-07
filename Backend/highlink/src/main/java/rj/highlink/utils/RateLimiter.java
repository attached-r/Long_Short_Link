package rj.highlink.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * Redis 限流工具类：基于 Lua 脚本实现固定窗口限流
 * 特点：Lua 脚本保证原子性，避免并发问题，适配短链系统高并发场景
 */
@Component
public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 优化后的固定窗口限流 Lua 脚本（修复nil比较、简化逻辑、增加边界判断）
    private static final String RATE_LIMIT_LUA_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local expire = tonumber(ARGV[2])
        
        -- 边界校验：阈值或过期时间非法，直接放行（避免配置错误导致全限流）
        if not limit or limit <= 0 or not expire or expire <= 0 then
            return 1
        end
        
        -- 获取当前计数，nil/非数字值均转为0（核心修复：避免nil比较）
        local current = tonumber(redis.call('GET', key)) or 0
        
        -- 超过阈值直接返回限流
        if current >= limit then
            return 0
        end
        
        -- 计数+1（首次访问时自动从0→1）
        redis.call('INCR', key)
        -- 首次访问时设置过期时间（避免重复设置expire，提升性能）
        if current == 0 then
            redis.call('EXPIRE', key, expire)
        end
        
        return 1
        """;

    // 初始化 Lua 脚本对象（静态常量，避免重复创建）
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    // 静态代码块初始化脚本（比构造方法更高效）
    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(RATE_LIMIT_LUA_SCRIPT);
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    /**
     * 固定窗口限流核心方法
     * @param key 限流标识（建议格式：IP:接口名 或 用户 ID:接口名）
     * @param limit 单位时间内最大请求数（阈值，必须>0）
     * @param expireSeconds 窗口时间（秒，必须>0）
     * @return true=允许访问，false=限流
     */
    public boolean tryAcquire(String key, int limit, int expireSeconds) {
        // 1. 参数前置校验（避免无效 Redis 调用）
        if (key == null || key.trim().isEmpty()) {
            log.warn("限流 key 为空，默认放行");
            return true;
        }
        if (limit <= 0 || expireSeconds <= 0) {
            log.warn("限流参数非法：limit={}, expireSeconds={}，默认放行", limit, expireSeconds);
            return true;
        }

        try {
            // 2. 构造 Redis 键（增加前缀，方便 Redis 管理）
            String redisKey = "rate_limit:" + key.trim();
            List<String> keys = Collections.singletonList(redisKey);

            // 3. 执行 Lua 脚本（原子性操作）
            Long result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    keys,
                    String.valueOf(limit),
                    String.valueOf(expireSeconds)
            );

            // 4. 处理脚本返回结果（避免 null）
            boolean allow = result != null && result == 1;
            if (!allow) {
                log.debug("限流触发：key={}, 当前请求数已达到阈值={}, 窗口时间={}秒", redisKey, limit, expireSeconds);
            }
            return allow;
        } catch (Exception e) {
            // 5. Redis 异常容错（核心：避免限流组件故障导致服务不可用）
            log.error("Redis 限流执行异常，默认放行。key={}, limit={}, expireSeconds={}", key, limit, expireSeconds, e);
            return true;
        }
    }

    /**
     * 简化版限流方法（默认：1分钟最多60次请求）
     * @param key 限流标识
     * @return true=允许访问，false=限流
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 60, 60);
    }
}