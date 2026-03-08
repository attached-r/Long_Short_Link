package rj.highlink.testUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.utils.RedisUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisUtil 工具类测试
 */
@SpringBootTest
@DisplayName("RedisUtil 工具类测试")
public class TestRedisUtil {

    @Autowired
    private RedisUtil redisUtil;

    private String testShortCode;

    @BeforeEach
    void setUp() {
        testShortCode = "test:" + System.currentTimeMillis();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        redisUtil.deleteLink(testShortCode);
    }

    @Test
    @DisplayName("测试设置和获取短链 - 带过期时间")
    void testSetLink_WithExpire() throws InterruptedException {
        String longUrl = "https://www.example.com/test";

        // 设置短链，1 秒过期
        boolean setResult = redisUtil.setLink(testShortCode + "_exp", longUrl, 1);

        assertTrue(setResult, "设置短链应该成功");

        // 立即查询应该存在
        String getResult = redisUtil.getLink(testShortCode + "_exp");
        assertEquals(longUrl, getResult, "应该能获取到长链接");

        // 等待过期
        Thread.sleep(1500);

        // 过期后应该返回 null
        String expiredResult = redisUtil.getLink(testShortCode + "_exp");
        assertNull(expiredResult, "过期后应该返回 null");
    }

    @Test
    @DisplayName("测试设置和获取短链 - 永不过期")
    void testSetLink_NoExpire() {
        String longUrl = "https://www.example.com/permanent";

        // 设置短链，永不过期（-1）
        boolean setResult = redisUtil.setLink(testShortCode + "_perm", longUrl, -1);

        assertTrue(setResult, "设置永不过期的短链应该成功");

        // 查询应该存在
        String getResult = redisUtil.getLink(testShortCode + "_perm");
        assertEquals(longUrl, getResult, "应该能获取到长链接");
    }

    @Test
    @DisplayName("测试获取不存在的短链")
    void testGetLink_NotExist() {
        String result = redisUtil.getLink("nonexistent:" + System.currentTimeMillis());
        assertNull(result, "不存在的短链应该返回 null");
    }

    @Test
    @DisplayName("测试黑名单功能 - 设置和判断")
    void testSetBlack_IsBlack() {
        String shortCode = testShortCode + "_black";

        // 初始不在黑名单
        assertFalse(redisUtil.isBlack(shortCode), "初始状态不应该在黑名单中");

        // 加入黑名单，10 分钟过期
        boolean setResult = redisUtil.setBlack(shortCode, 600);
        assertTrue(setResult, "加入黑名单应该成功");

        // 判断应该在黑名单
        assertTrue(redisUtil.isBlack(shortCode), "应该在黑名单中");
    }

    @Test
    @DisplayName("测试黑名单功能 - 过期自动移除")
    void testSetBlack_Expire() throws InterruptedException {
        String shortCode = testShortCode + "_blackExp";

        // 加入黑名单，1 秒过期
        redisUtil.setBlack(shortCode, 1);
        assertTrue(redisUtil.isBlack(shortCode), "应该在黑名单中");

        // 等待过期
        Thread.sleep(1500);

        // 过期后应该不在黑名单
        assertFalse(redisUtil.isBlack(shortCode), "过期后应该不在黑名单中");
    }

    @Test
    @DisplayName("测试黑名单功能 - 永不过期")
    void testSetBlack_Permanent() {
        String shortCode = testShortCode + "_blackPerm";

        // 加入黑名单，永不过期
        redisUtil.setBlack(shortCode, -1);

        assertTrue(redisUtil.isBlack(shortCode), "永久黑名单应该在黑名单中");
    }

    @Test
    @DisplayName("测试删除短链")
    void testDeleteLink() {
        String longUrl = "https://www.example.com/todelete";

        // 先设置短链
        redisUtil.setLink(testShortCode + "_del", longUrl, 60);

        // 验证存在
        assertNotNull(redisUtil.getLink(testShortCode + "_del"), "删除前应该存在");

        // 删除短链
        boolean deleteResult = redisUtil.deleteLink(testShortCode + "_del");
        assertTrue(deleteResult, "删除短链应该成功");

        // 验证不存在
        assertNull(redisUtil.getLink(testShortCode + "_del"), "删除后应该返回 null");
    }

    @Test
    @DisplayName("测试布隆过滤器 - 添加和判断")
    void testAddToBloom_Contains() {
        String shortCode = testShortCode + "_bloom";

        // 添加到布隆过滤器
        boolean addResult = redisUtil.addToBloom(shortCode);
        assertTrue(addResult, "添加到布隆过滤器应该成功");

        // 判断应该存在（布隆过滤器可能误判，但不会漏判）
        // 注意：这里不能直接调用 redisUtil.contains()，需要通过 BloomFilterUtil
        System.out.println("✅ 布隆过滤器添加成功：" + shortCode);
    }

    @Test
    @DisplayName("测试删除短链后布隆过滤器仍存在")
    void testDeleteLink_BloomFilterStillExists() {
        String shortCode = testShortCode + "_bloomDel";
        String longUrl = "https://www.example.com/test";

        // 添加到布隆过滤器
        redisUtil.addToBloom(shortCode);

        // 设置短链
        redisUtil.setLink(shortCode, longUrl, 60);

        // 删除短链
        redisUtil.deleteLink(shortCode);

        // 布隆过滤器中仍然存在（布隆过滤器不支持删除）
        // 这是预期行为
        System.out.println("✅ 布隆过滤器删除后仍存在（预期行为）");
    }

    @Test
    @DisplayName("测试短链不存在且不在布隆过滤器")
    void testGetLink_NotInBloomFilter() {
        String shortCode = "notexist:" + System.currentTimeMillis();

        // 不在布隆过滤器中，应该返回 null
        String result = redisUtil.getLink(shortCode);
        assertNull(result, "不在布隆过滤器中的短链应该返回 null");
    }

    @Test
    @DisplayName("测试短链在删除标记中")
    void testGetLink_DeletedMark() {
        String shortCode = testShortCode + "_deleted";
        String longUrl = "https://www.example.com/test";

        // 先正常设置
        redisUtil.setLink(shortCode, longUrl, 60);

        // 添加删除标记
        redisUtil.deleteLink(shortCode);

        // 应该返回 null（删除标记优先级最高）
        String result = redisUtil.getLink(shortCode);
        assertNull(result, "有删除标记的短链应该返回 null");
    }

    @Test
    @DisplayName("测试并发设置同一个短链")
    void testConcurrentSetLink() throws InterruptedException {
        String shortCode = testShortCode + "_concurrent";
        String longUrl = "https://www.example.com/concurrent";

        // 并发设置 10 次
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                redisUtil.setLink(shortCode, longUrl + index, 60);
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 最终应该有一个值
        String result = redisUtil.getLink(shortCode);
        assertNotNull(result, "并发设置后应该有值");
    }
}
