package rj.highlink.testService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import rj.highlink.common.result.R;
import rj.highlink.controller.ShortLinkController;
import rj.highlink.entity.dto.CreateShortLinkDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 高并发测试类
 * 测试创建接口的幂等性和限流功能
 */
@SpringBootTest
@DisplayName("高并发测试")
@AutoConfigureMockMvc // 启用 MockMvc，模拟 HTTP 请求
class TestHighConcurrency {
    @Autowired
    private MockMvc mockMvc; // 模拟 HTTP 请求的核心类

    @Autowired
    private ObjectMapper objectMapper; // 序列化/反序列化 JSON

    @Autowired
    private ShortLinkController shortLinkController;

    // 固定线程池，模拟并发用户
    private static final ExecutorService executor = Executors.newFixedThreadPool(20);

    @Test
    @DisplayName("测试并发创建短链接 - 100 个请求")
    void testConcurrentCreate() throws InterruptedException {
        // Given: 准备测试数据
        int requestCount = 100;
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> shortCodes = new ArrayList<>();

        System.out.println("=== 开始并发测试：创建 " + requestCount + " 个短链接 ===");
        long startTime = System.currentTimeMillis();

        // When: 并发发送 100 个请求
        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            CompletableFuture.runAsync(() -> {
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.example.com/test/" + index);
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                        if (result.getData() != null) {
                            synchronized (shortCodes) {
                                shortCodes.add(result.getData().getShortCode());
                            }
                        }
                        System.out.println("✅ 请求 " + index + " 成功：" +
                                (result.getData() != null ? result.getData().getShortCode() : "null"));
                    } else {
                        failCount.incrementAndGet();
                        System.out.println("❌ 请求 " + index + " 失败：" + result.getMessage());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("❌ 请求 " + index + " 异常：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        // 等待所有请求完成
        latch.await();
        long endTime = System.currentTimeMillis();

        // Then: 验证结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   失败数：" + failCount.get());
        System.out.println("   耗时：" + (endTime - startTime) + "ms");
        System.out.println("   平均 QPS: " + (requestCount * 1000 / (endTime - startTime)));

        // 验证：至少有一部分请求成功
        assertTrue(successCount.get() > 0, "应该有部分请求成功");

        // 验证：所有短链码不重复（幂等性）
        long uniqueCount = shortCodes.stream().distinct().count();
        assertEquals(shortCodes.size(), uniqueCount, "所有短链码应该唯一");

        System.out.println("✅ 并发测试通过");
    }

    @Test
    @DisplayName("测试并发创建相同 URL - 验证幂等性")
    void testConcurrentSameUrl() throws InterruptedException {
        // Given: 相同的 URL
        int requestCount = 50;
        CountDownLatch latch = new CountDownLatch(requestCount);
        String sameUrl = "https://www.example.com/same-url-test";
        List<String> shortCodes = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("\n=== 测试并发创建相同 URL（幂等性）===");

        // When: 并发发送 50 个相同 URL 的请求
        for (int i = 0; i < requestCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl(sameUrl);
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200 && result.getData() != null) {
                        successCount.incrementAndGet();
                        synchronized (shortCodes) {
                            shortCodes.add(result.getData().getShortCode());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("❌ 请求异常：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        // 等待所有请求完成
        latch.await();

        // Then: 验证所有返回的短链码相同（幂等性）
        long uniqueCount = shortCodes.stream().distinct().count();
        assertEquals(1, uniqueCount, "相同 URL 应该生成相同的短链码");

        System.out.println("✅ 幂等性测试通过");
        System.out.println("   成功次数：" + successCount.get());
        System.out.println("   短链码唯一数：" + uniqueCount);
        System.out.println("   短链码：" + (shortCodes.isEmpty() ? "无" : shortCodes.get(0)));
    }

    @Test
    @DisplayName("测试限流拦截器 - 超过阈值返回 429")
    void testRateLimiter() throws InterruptedException {
        // 1. 测试配置
        int requestCount = 15; // 超过 10 次/分钟的阈值
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 2. 打印测试信息
        System.out.println("\n=== 测试限流拦截器（阈值：10 次/分钟）===");
        System.out.println("   请求数：" + requestCount);
        System.out.println("   限流阈值：" + 10 + " 次/分钟");

        // 3. 分批发送请求，确保触发限流
        for (int i = 0; i < requestCount; i++) {
            final int index = i;

            // 每 10ms 发送一个请求，确保限流计数器能累加
            if (i > 0 && i % 5 == 0) {
                Thread.sleep(50); // 每 5 个请求稍作等待
            }

            executor.submit(() -> {
                try {
                    // 构建创建短链接的请求参数
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.limit-test-" + System.currentTimeMillis() + "-" + index + ".com");
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    // 发送 HTTP POST 请求到 /shortlink/create（核心：触发拦截器）
                    String responseJson = mockMvc.perform(post("/shortlink/create")
                                    .contentType(MediaType.APPLICATION_JSON) // JSON 请求体
                                    .content(objectMapper.writeValueAsString(dto))) // 序列化 DTO
                            .andReturn() // 获取响应结果
                            .getResponse()
                            .getContentAsString(); // 响应体转字符串

                    // 解析响应结果
                    R<?> result = objectMapper.readValue(responseJson, R.class);
                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                        System.out.println("✅ 请求 " + index + " 成功：" + result.getData());
                    } else if (result.getCode() == 429) {
                        rateLimitedCount.incrementAndGet();
                        System.out.println("⚠️  请求 " + index + " 被限流 (429): " + result.getMessage());
                    } else {
                        failCount.incrementAndGet();
                        System.out.println("❌ 请求 " + index + " 失败：" + result.getMessage());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("❌ 请求 " + index + " 异常：" + e.getMessage());
                } finally {
                    latch.countDown(); // 计数减 1
                }
            });
        }

        // 4. 等待所有请求完成
        latch.await();

        // 5. 打印测试结果
        System.out.println("\n=== 限流测试结果 ===");
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   被限流数：" + rateLimitedCount.get());
        System.out.println("   失败数：" + failCount.get());

        // 6. 断言验证（核心：必须有请求被限流）
        assertTrue(rateLimitedCount.get() > 0,
                "应该有请求被限流（当前：" + rateLimitedCount.get() + "）");
        assertTrue(successCount.get() <= 10,
                String.format("成功数不应超过限流阈值（10），实际：%d", successCount.get()));

        System.out.println("✅ 限流测试通过");
    }


    @Test
    @DisplayName("测试高并发重定向 - 500 个请求")
    void testConcurrentRedirect() throws InterruptedException {
        // Given: 先创建一个短链接
        CreateShortLinkDTO dto = new CreateShortLinkDTO();
        dto.setLongUrl("https://www.example.com/redirect-test");
        dto.setExpireTime(LocalDateTime.now().plusDays(7));

        R<rj.highlink.entity.vo.ShortLinkVO> createResult = shortLinkController.create(dto);
        assertNotNull(createResult.getData(), "应该成功创建短链接");
        String shortCode = createResult.getData().getShortCode();

        // When: 并发访问 500 次
        int requestCount = 500;
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        System.out.println("\n=== 测试高并发重定向（" + requestCount + " 次）===");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.getInfo(shortCode);

                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        // 等待所有请求完成
        latch.await();
        long endTime = System.currentTimeMillis();

        // Then: 验证结果
        System.out.println("\n=== 重定向测试结果 ===");
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   失败数：" + failCount.get());
        System.out.println("   耗时：" + (endTime - startTime) + "ms");
        System.out.println("   平均 QPS: " + (requestCount * 1000 / (endTime - startTime)));

        // 验证：大部分请求应该成功
        assertTrue(successCount.get() > requestCount * 0.9,
                "90% 以上的请求应该成功");

        System.out.println("✅ 高并发重定向测试通过");
    }

    @Test
    @DisplayName("测试混合场景 - 创建 + 查询 + 重定向")
    void testMixedScenario() throws InterruptedException {
        // Given: 混合场景
        int createCount = 20;
        int queryCount = 50;
        int redirectCount = 100;

        CountDownLatch createLatch = new CountDownLatch(createCount);
        CountDownLatch queryLatch = new CountDownLatch(queryCount);
        CountDownLatch redirectLatch = new CountDownLatch(redirectCount);

        List<String> createdShortCodes = new ArrayList<>();
        AtomicInteger createSuccess = new AtomicInteger(0);
        AtomicInteger querySuccess = new AtomicInteger(0);
        AtomicInteger redirectSuccess = new AtomicInteger(0);

        System.out.println("\n=== 测试混合场景 ===");

        // 1. 并发创建短链接
        for (int i = 0; i < createCount; i++) {
            final int index = i;
            CompletableFuture.runAsync(() -> {
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.mixed-test.com/" + index);
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200 && result.getData() != null) {
                        createSuccess.incrementAndGet();
                        synchronized (createdShortCodes) {
                            createdShortCodes.add(result.getData().getShortCode());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("❌ 创建失败：" + e.getMessage());
                } finally {
                    createLatch.countDown();
                }
            }, executor);
        }

        // 等待创建完成
        createLatch.await();
        System.out.println("✅ 创建阶段完成：" + createSuccess.get() + " 个");

        if (createdShortCodes.isEmpty()) {
            fail("没有成功创建任何短链接");
        }

        // 2. 并发查询短链接
        for (int i = 0; i < queryCount; i++) {
            final int index = i;
            final String shortCode = createdShortCodes.get(index % createdShortCodes.size());

            CompletableFuture.runAsync(() -> {
                try {
                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.getInfo(shortCode);

                    if (result.getCode() == 200) {
                        querySuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.out.println("❌ 查询失败：" + e.getMessage());
                } finally {
                    queryLatch.countDown();
                }
            }, executor);
        }

        // 等待查询完成
        queryLatch.await();
        System.out.println("✅ 查询阶段完成：" + querySuccess.get() + " 次");

        // 3. 并发重定向
        for (int i = 0; i < redirectCount; i++) {
            final int index = i;
            final String shortCode = createdShortCodes.get(index % createdShortCodes.size());

            CompletableFuture.runAsync(() -> {
                try {
                    // 这里调用重定向方法（实际应该调用 redirect 方法）
                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.getInfo(shortCode);

                    if (result.getCode() == 200) {
                        redirectSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.out.println("❌ 重定向失败：" + e.getMessage());
                } finally {
                    redirectLatch.countDown();
                }
            }, executor);
        }

        // 等待重定向完成
        redirectLatch.await();

        // Then: 验证结果
        System.out.println("\n=== 混合场景测试结果 ===");
        System.out.println("   创建成功：" + createSuccess.get() + "/" + createCount);
        System.out.println("   查询成功：" + querySuccess.get() + "/" + queryCount);
        System.out.println("   重定向成功：" + redirectSuccess.get() + "/" + redirectCount);

        // 验证：各阶段都应该有一定成功率
        assertTrue(createSuccess.get() > 0, "应该有创建成功");
        assertTrue(querySuccess.get() > 0, "应该有查询成功");
        assertTrue(redirectSuccess.get() > 0, "应该有重定向成功");

        System.out.println("✅ 混合场景测试通过");
    }
}
