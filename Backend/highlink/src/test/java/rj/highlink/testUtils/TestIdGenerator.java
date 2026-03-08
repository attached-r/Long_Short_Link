package rj.highlink.testUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rj.highlink.utils.IdGenerator;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdGenerator 雪花 ID 生成器测试
 */
@DisplayName("雪花 ID 生成器测试")
public class TestIdGenerator {

    @Test
    @DisplayName("测试生成 ID 的唯一性")
    void testNextId_Uniqueness() {
        Set<Long> idSet = new HashSet<>();
        int count = 10000;

        // 生成 10000 个 ID
        for (int i = 0; i < count; i++) {
            long id = IdGenerator.nextId();
            idSet.add(id);
        }

        // 验证唯一性
        assertEquals(count, idSet.size(), "生成的 " + count + " 个 ID 应该全部唯一");
    }

    @Test
    @DisplayName("测试生成 ID 的趋势递增性")
    void testNextId_TrendIncreasing() {
        long firstId = IdGenerator.nextId();

        try {
            Thread.sleep(10); // 等待 10ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long secondId = IdGenerator.nextId();

        // 雪花 ID 应该是趋势递增的（时间戳增大）
        assertTrue(secondId > firstId, "后生成的 ID 应该大于先生成的 ID（趋势递增）");
    }

    @Test
    @DisplayName("测试生成 ID 为正数")
    void testNextId_Positive() {
        for (int i = 0; i < 100; i++) {
            long id = IdGenerator.nextId();
            assertTrue(id > 0, "生成的 ID 应该是正数：" + id);
        }
    }

    @Test
    @DisplayName("测试并发安全性")
    void testNextId_ConcurrentSafety() throws InterruptedException {
        Set<Long> idSet = new HashSet<>();
        Thread[] threads = new Thread[20];

        // 20 个线程并发生成 ID
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 500; j++) {
                    long id = IdGenerator.nextId();
                    synchronized (idSet) {
                        idSet.add(id);
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证总数和唯一性
        assertEquals(10000, idSet.size(), "20 个线程各生成 500 个 ID，应该全部唯一");
    }

    @Test
    @DisplayName("测试生成 ID 的格式")
    void testNextId_Format() {
        long id = IdGenerator.nextId();

        // 雪花 ID 应该是 64 位长整型
        assertTrue(id > 0, "ID 应该是正数");
        assertTrue(id < Long.MAX_VALUE, "ID 不应该超过 Long 最大值");

        System.out.println("✅ 生成的雪花 ID: " + id);
        System.out.println("   二进制：" + Long.toBinaryString(id));
    }

    @Test
    @DisplayName("测试快速连续生成")
    void testNextId_RapidGeneration() {
        long[] ids = new long[1000];

        // 快速连续生成 1000 个 ID
        for (int i = 0; i < 1000; i++) {
            ids[i] = IdGenerator.nextId();
        }

        // 验证所有 ID 唯一
        Set<Long> uniqueSet = new HashSet<>();
        for (long id : ids) {
            uniqueSet.add(id);
        }

        assertEquals(1000, uniqueSet.size(), "快速连续生成的 1000 个 ID 应该全部唯一");
    }
}
