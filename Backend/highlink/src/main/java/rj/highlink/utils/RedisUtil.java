package rj.highlink.utils;

import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 包含：短链增删查、黑名单、布隆过滤器操作
 * setLink / getLink(含布隆判断) / setBlack / isBlack / deleteLink / addToBloom
 */
@Component // 注册为 Spring Bean
public class RedisUtil {
    // 注入自定义 RedisTemplate
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 注入布隆过滤器（用于短链去重/防穿透）
    @Resource
    private RBloomFilter<String> shortLinkBloomFilter;

    // 短链 Redis 前缀
    private static final String SHORT_LINK_PREFIX = "short_link:";
    // 黑名单 Redis 前缀
    private static final String BLACK_LIST_PREFIX = "black_list:";

    /**
     * 1. 存储短链（带过期时间）
     * @param shortCode 短码
     * @param longUrl 长链接
     * @param expireSeconds 过期时间（秒，-1 表示永不过期）
     * @return 存储结果
     */
    public boolean setLink(String shortCode, String longUrl, long expireSeconds) {
        try {
            String key = SHORT_LINK_PREFIX + shortCode;
            if (expireSeconds > 0) {
                // 最后一个参数 表示过期时间单位：秒
                redisTemplate.opsForValue().set(key, longUrl, expireSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, longUrl);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 2. 查询长链接（含布隆过滤器前置判断，防穿透）
     * @param shortCode 短码
     * @return 长链接（null 表示不存在）
     */
    public String getLink(String shortCode) {
        // 先查是否被逻辑删除（优先级最高）
        if (Boolean.TRUE.equals(redisTemplate.hasKey("short_link_deleted:" + shortCode))) {
            return null;
        }
        // 布隆过滤器快速判断：返回 false → 一定不存在，直接返回 null
        // 防止缓存穿透
        if (!shortLinkBloomFilter.contains(shortCode)) {
            return null;
        }
        // 布隆过滤器返回 true → 可能存在，查询 Redis
        try {
            String key = SHORT_LINK_PREFIX + shortCode;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 3. 加入黑名单（禁止访问该短链）
     * @param shortCode 短码
     * @param expireSeconds 黑名单过期时间（秒，-1 永不过期）
     * @return 操作结果
     */
    public boolean setBlack(String shortCode, long expireSeconds) {
        try {
            String key = BLACK_LIST_PREFIX + shortCode;
            // 黑名单值存当前时间戳（标记加入时间）
            String value = String.valueOf(System.currentTimeMillis());
            if (expireSeconds > 0) {
                redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 4. 判断是否在黑名单中
     * @param shortCode 短码
     * @return true=在黑名单，false=不在
     */
    public boolean isBlack(String shortCode) {
        try {
            String key = BLACK_LIST_PREFIX + shortCode;
            return redisTemplate.hasKey(key); //存在：true 不存在：false
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 5. 删除短链（同时移除布隆过滤器标记，注意：布隆过滤器不支持删除，这里仅标记逻辑删除）
     * @param shortCode 短码
     * @return 操作结果
     */
    public boolean deleteLink(String shortCode) {
        try {
            // 删除 Redis 中的短链
            String key = SHORT_LINK_PREFIX + shortCode;
            redisTemplate.delete(key);

            // 注意：布隆过滤器不支持直接删除元素，这里可做逻辑处理（比如加删除标记）
            String deleteKey = "short_link_deleted:" + shortCode;
            redisTemplate.opsForValue().set(deleteKey, "1", 7, TimeUnit.DAYS); // 7天过期

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 6. 将短码加入布隆过滤器（新增短链时调用）
     * @param shortCode 短码
     * @return 操作结果
     * 先添加到布隆过滤器，再添加到 Redis 中，防止短链重复
     */
    public boolean addToBloom(String shortCode) {
        try {
            shortLinkBloomFilter.add(shortCode);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
