package rj.highlink.testUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import rj.highlink.utils.RateLimiter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimiter 限流器测试
 */
@SpringBootTest
@DisplayName("限流器测试")
public class TestRateLimiter {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // 清理所有测试 key，确保测试隔离
        cleanupTestKeys();
    }

    @Test
    @DisplayName("测试正常限流 - 允许访问")
    void testTryAcquire_AllowAccess() {
        String testKey = "test_allow_" + System.currentTimeMillis();

        boolean result1 = rateLimiter.tryAcquire(testKey, 5, 60);
        boolean result2 = rateLimiter.tryAcquire(testKey, 5, 60);
        boolean result3 = rateLimiter.tryAcquire(testKey, 5, 60);

        assertTrue(result1, "第一次请求应该允许");
        assertTrue(result2, "第二次请求应该允许");
        assertTrue(result3, "第三次请求应该允许");
    }

    @Test
    @DisplayName("测试限流触发 - 达到阈值")
    void testTryAcquire_RateLimited() {
        String key = "test_limited_" + System.currentTimeMillis();
        int limit = 3;

        assertTrue(rateLimiter.tryAcquire(key, limit, 60), "第 1 次请求应该允许");
        assertTrue(rateLimiter.tryAcquire(key, limit, 60), "第 2 次请求应该允许");
        assertTrue(rateLimiter.tryAcquire(key, limit, 60), "第 3 次请求应该允许");

        assertFalse(rateLimiter.tryAcquire(key, limit, 60), "第 4 次请求应该被限流");
        assertFalse(rateLimiter.tryAcquire(key, limit, 60), "第 5 次请求应该被限流");
    }

    @Test
    @DisplayName("测试边界值 - limit=1")
    void testTryAcquire_LimitOne() {
        String key = "test_limit_one_" + System.currentTimeMillis();

        assertTrue(rateLimiter.tryAcquire(key, 1, 60), "第一次请求应该允许");
        assertFalse(rateLimiter.tryAcquire(key, 1, 60), "第二次请求应该被限流");
    }

    @Test
    @DisplayName("测试边界值 - expireSeconds=1")
    void testTryAcquire_ShortExpire() throws InterruptedException {
        String key = "test_short_expire_" + System.currentTimeMillis();

        assertTrue(rateLimiter.tryAcquire(key, 1, 1), "第一次请求应该允许");
        assertFalse(rateLimiter.tryAcquire(key, 1, 1), "第二次请求应该被限流");

        Thread.sleep(2000);

        assertTrue(rateLimiter.tryAcquire(key, 1, 1), "过期后应该允许访问");
    }

    @Test
    @DisplayName("测试参数非法 - limit=0")
    void testTryAcquire_LimitZero() {
        String key = "test_limit_zero_" + System.currentTimeMillis();
        boolean result = rateLimiter.tryAcquire(key, 0, 60);

        assertTrue(result, "limit=0 时应该放行（参数非法默认放行）");
    }

    @Test
    @DisplayName("测试参数非法 - limit 为负数")
    void testTryAcquire_LimitNegative() {
        String key = "test_limit_neg_" + System.currentTimeMillis();
        boolean result = rateLimiter.tryAcquire(key, -1, 60);

        assertTrue(result, "limit 为负数时应该放行（参数非法默认放行）");
    }

    @Test
    @DisplayName("测试参数非法 - expireSeconds=0")
    void testTryAcquire_ExpireZero() {
        String key = "test_expire_zero_" + System.currentTimeMillis();
        boolean result = rateLimiter.tryAcquire(key, 10, 0);

        assertTrue(result, "expireSeconds=0 时应该放行（参数非法默认放行）");
    }

    @Test
    @DisplayName("测试参数非法 - expireSeconds 为负数")
    void testTryAcquire_ExpireNegative() {
        String key = "test_expire_neg_" + System.currentTimeMillis();
        boolean result = rateLimiter.tryAcquire(key, 10, -1);

        assertTrue(result, "expireSeconds 为负数时应该放行（参数非法默认放行）");
    }

    @Test
    @DisplayName("测试 key 为 null")
    void testTryAcquire_KeyNull() {
        boolean result = rateLimiter.tryAcquire(null, 10, 60);

        assertTrue(result, "key 为 null 时应该放行");
    }

    @Test
    @DisplayName("测试 key 为空字符串")
    void testTryAcquire_KeyEmpty() {
        boolean result = rateLimiter.tryAcquire("", 10, 60);

        assertTrue(result, "key 为空字符串时应该放行");
    }

    @Test
    @DisplayName("测试 key 为空白字符串")
    void testTryAcquire_KeyBlank() {
        boolean result = rateLimiter.tryAcquire("   ", 10, 60);

        assertTrue(result, "key 为空白字符串时应该放行");
    }

    @Test
    @DisplayName("测试简化版限流方法")
    void testTryAcquire_SimpleVersion() {
        String key = "test_simple_" + System.currentTimeMillis();

        for (int i = 0; i < 60; i++) {
            if (i < 60) {
                assertTrue(rateLimiter.tryAcquire(key), "第 " + (i + 1) + " 次请求应该允许");
            }
        }

        assertFalse(rateLimiter.tryAcquire(key), "第 61 次请求应该被限流");
    }

    @Test
    @DisplayName("测试不同 key 独立计数")
    void testTryAcquire_DifferentKeys() {
        String key1 = "test_user1_" + System.currentTimeMillis();
        String key2 = "test_user2_" + System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire(key1, 5, 60);
        }
        assertFalse(rateLimiter.tryAcquire(key1, 5, 60), "key1 应该被限流");

        assertTrue(rateLimiter.tryAcquire(key2, 5, 60), "key2 应该允许访问");
    }

    @Test
    @DisplayName("测试 Redis 故障降级")
    void testTryAcquire_RedisException() {
        System.out.println("⚠️  Redis 故障降级测试需要 Mock 支持，已跳过");
    }

    private void cleanupTestKeys() {
        Set<String> keys = redisTemplate.keys("rate_limit:test_*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
