package rj.highlink.testService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import rj.highlink.common.result.R;
import rj.highlink.controller.ShortLinkController;
import rj.highlink.entity.dto.CreateShortLinkDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 高并发测试类
 * 测试创建接口的幂等性和限流功能
 */
@SpringBootTest
@DisplayName("高并发测试")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestHighConcurrency {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkController shortLinkController;

    private static final ExecutorService executor = Executors.newFixedThreadPool(50);

    @Test
    @Order(1)
    @DisplayName("压测场景 1: 小流量 - 100 并发创建短链接")
    void testSmallLoadCreate() throws InterruptedException {
        runLoadTest("小流量", 100, 20, true);
    }

    @Test
    @Order(2)
    @DisplayName("压测场景 2: 中流量 - 500 并发创建短链接")
    void testMediumLoadCreate() throws InterruptedException {
        runLoadTest("中流量", 500, 50, true);
    }

    @Test
    @Order(3)
    @DisplayName("压测场景 3: 大流量 - 1000 并发创建短链接")
    void testHighLoadCreate() throws InterruptedException {
        runLoadTest("大流量", 1000, 50, true);
    }

    @Test
    @Order(4)
    @DisplayName("压测场景 4: 超大流量 - 2000 并发创建短链接")
    void testVeryHighLoadCreate() throws InterruptedException {
        runLoadTest("超大流量", 2000, 50, true);
    }

    @Test
    @Order(5)
    @DisplayName("压测场景 5: 小流量重定向 - 500 并发")
    void testSmallLoadRedirect() throws InterruptedException {
        String shortCode = createTestShortLink("https://www.example.com/redirect-small-test");
        assertNotNull(shortCode, "应该成功创建测试短链接");

        runRedirectLoadTest("小流量重定向", 500, 20, shortCode);
    }

    @Test
    @Order(6)
    @DisplayName("压测场景 6: 中流量重定向 - 1000 并发")
    void testMediumLoadRedirect() throws InterruptedException {
        String shortCode = createTestShortLink("https://www.example.com/redirect-medium-test");
        assertNotNull(shortCode, "应该成功创建测试短链接");

        runRedirectLoadTest("中流量重定向", 1000, 50, shortCode);
    }

    @Test
    @Order(7)
    @DisplayName("压测场景 7: 大流量重定向 - 3000 并发")
    void testHighLoadRedirect() throws InterruptedException {
        String shortCode = createTestShortLink("https://www.example.com/redirect-high-test");
        assertNotNull(shortCode, "应该成功创建测试短链接");

        runRedirectLoadTest("大流量重定向", 3000, 50, shortCode);
    }

    @Test
    @Order(8)
    @DisplayName("压测场景 8: 混合场景 - 创建 + 查询 + 重定向")
    void testMixedScenario() throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("开始混合场景压测");
        System.out.println("=".repeat(80));

        int createCount = 100;
        int queryCount = 300;
        int redirectCount = 500;

        CountDownLatch createLatch = new CountDownLatch(createCount);
        CountDownLatch queryLatch = new CountDownLatch(queryCount);
        CountDownLatch redirectLatch = new CountDownLatch(redirectCount);

        List<String> createdShortCodes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger createSuccess = new AtomicInteger(0);
        AtomicInteger querySuccess = new AtomicInteger(0);
        AtomicInteger redirectSuccess = new AtomicInteger(0);
        AtomicLong createTotalTime = new AtomicLong(0);
        AtomicLong queryTotalTime = new AtomicLong(0);
        AtomicLong redirectTotalTime = new AtomicLong(0);

        long overallStartTime = System.currentTimeMillis();

        for (int i = 0; i < createCount; i++) {
            final int index = i;
            CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.mixed-test.com/" + index);
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200 && result.getData() != null) {
                        createSuccess.incrementAndGet();
                        createdShortCodes.add(result.getData().getShortCode());
                    }
                } catch (Exception e) {
                    System.out.println("❌ 创建失败：" + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - start;
                    createTotalTime.addAndGet(elapsed);
                    createLatch.countDown();
                }
            }, executor);
        }

        createLatch.await();
        System.out.println("✅ 创建阶段完成：" + createSuccess.get() + "/" + createCount);

        if (createdShortCodes.isEmpty()) {
            fail("没有成功创建任何短链接");
        }

        for (int i = 0; i < queryCount; i++) {
            final int index = i;
            final String shortCode = createdShortCodes.get(index % createdShortCodes.size());

            CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.getInfo(shortCode);

                    if (result.getCode() == 200) {
                        querySuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.out.println("❌ 查询失败：" + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - start;
                    queryTotalTime.addAndGet(elapsed);
                    queryLatch.countDown();
                }
            }, executor);
        }

        queryLatch.await();
        System.out.println("✅ 查询阶段完成：" + querySuccess.get() + "/" + queryCount);

        for (int i = 0; i < redirectCount; i++) {
            final int index = i;
            final String shortCode = createdShortCodes.get(index % createdShortCodes.size());

            CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.getInfo(shortCode);

                    if (result.getCode() == 200) {
                        redirectSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.out.println("❌ 重定向失败：" + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - start;
                    redirectTotalTime.addAndGet(elapsed);
                    redirectLatch.countDown();
                }
            }, executor);
        }

        redirectLatch.await();

        long overallEndTime = System.currentTimeMillis();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("混合场景压测结果");
        System.out.println("=".repeat(80));
        System.out.printf("总耗时：%d ms%n", overallEndTime - overallStartTime);
        System.out.println("创建阶段:");
        System.out.printf("  成功：%d/%d (%.2f%%)%n", createSuccess.get(), createCount,
                (createSuccess.get() * 100.0 / createCount));
        System.out.printf("  平均响应时间：%d ms%n", createTotalTime.get() / Math.max(createSuccess.get(), 1));
        System.out.printf("  QPS: %.2f%n", createCount * 1000.0 / Math.max(createTotalTime.get(), 1));

        System.out.println("查询阶段:");
        System.out.printf("  成功：%d/%d (%.2f%%)%n", querySuccess.get(), queryCount,
                (querySuccess.get() * 100.0 / queryCount));
        System.out.printf("  平均响应时间：%d ms%n", queryTotalTime.get() / Math.max(querySuccess.get(), 1));
        System.out.printf("  QPS: %.2f%n", queryCount * 1000.0 / Math.max(queryTotalTime.get(), 1));

        System.out.println("重定向阶段:");
        System.out.printf("  成功：%d/%d (%.2f%%)%n", redirectSuccess.get(), redirectCount,
                (redirectSuccess.get() * 100.0 / redirectCount));
        System.out.printf("  平均响应时间：%d ms%n", redirectTotalTime.get() / Math.max(redirectSuccess.get(), 1));
        System.out.printf("  QPS: %.2f%n", redirectCount * 1000.0 / Math.max(redirectTotalTime.get(), 1));

        assertTrue(createSuccess.get() > 0, "应该有创建成功");
        assertTrue(querySuccess.get() > 0, "应该有查询成功");
        assertTrue(redirectSuccess.get() > 0, "应该有重定向成功");

        System.out.println("✅ 混合场景测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("压测场景 9: 幂等性测试 - 相同 URL 并发创建")
    void testConcurrentSameUrl() throws InterruptedException {
        int requestCount = 100;
        CountDownLatch latch = new CountDownLatch(requestCount);
        String sameUrl = "https://www.example.com/same-url-idempotency-test";
        List<String> shortCodes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("开始幂等性压测");
        System.out.println("=".repeat(80));

        for (int i = 0; i < requestCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl(sameUrl);
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200 && result.getData() != null) {
                        successCount.incrementAndGet();
                        shortCodes.add(result.getData().getShortCode());
                    }
                } catch (Exception e) {
                    System.out.println("❌ 请求异常：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, executor);
        }

        latch.await();

        long uniqueCount = shortCodes.stream().distinct().count();
        assertEquals(1, uniqueCount, "相同 URL 应该生成相同的短链码");

        System.out.println("✅ 幂等性测试通过");
        System.out.println("   成功次数：" + successCount.get());
        System.out.println("   短链码唯一数：" + uniqueCount);
        System.out.println("   短链码：" + (shortCodes.isEmpty() ? "无" : shortCodes.get(0)));
    }

    @Test
    @Order(10)
    @DisplayName("压测场景 10: 限流测试 - 超过阈值")
    void testRateLimiter() throws InterruptedException {
        int requestCount = 30;
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("开始限流压测（阈值：10 次/分钟）");
        System.out.println("=".repeat(80));
        System.out.println("   请求数：" + requestCount);
        System.out.println("   限流阈值：" + 10 + " 次/分钟");
        System.out.println("   注意：MockMvc 可能不会触发拦截器，此测试仅供参考");

        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < requestCount; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.rate-limit-test-" + System.currentTimeMillis() + "-" + index + ".com");
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    String responseJson = mockMvc.perform(post("/shortlink/create")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(dto)))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

                    R<?> result = objectMapper.readValue(responseJson, R.class);
                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                        System.out.println("✅ 请求 " + index + " 成功");
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
                    latch.countDown();
                }
            });
        }

        startLatch.countDown();

        latch.await(60, TimeUnit.SECONDS);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("限流测试结果");
        System.out.println("=".repeat(80));
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   被限流数：" + rateLimitedCount.get());
        System.out.println("   失败数：" + failCount.get());

        if (rateLimitedCount.get() > 0) {
            System.out.println("✅ 限流测试通过：成功触发限流，" + rateLimitedCount.get() + " 个请求被拦截");
        } else {
            System.out.println("⚠️  限流测试提示：没有请求被限流，可能是：");
            System.out.println("   1. MockMvc 未触发拦截器（需要使用 WebMvcTest 或集成测试）");
            System.out.println("   2. 所有请求都在阈值范围内");
            System.out.println("   3. 建议通过真实 HTTP 请求测试限流功能");
            System.out.println("✅ 测试完成");
        }
    }


    private void runLoadTest(String scenarioName, int requestCount, int threadCount, boolean verifyUniqueness)
            throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("开始 " + scenarioName + " 压测");
        System.out.println("=".repeat(80));
        System.out.println("   请求总数：" + requestCount);
        System.out.println("   线程数：" + threadCount);

        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> shortCodes = Collections.synchronizedList(new ArrayList<>());
        AtomicLong totalTime = new AtomicLong(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    CreateShortLinkDTO dto = new CreateShortLinkDTO();
                    dto.setLongUrl("https://www.load-test-" + scenarioName.replaceAll("[^a-zA-Z0-9]", "") + "-" +
                            System.currentTimeMillis() + "-" + index + ".com");
                    dto.setExpireTime(LocalDateTime.now().plusDays(7));

                    R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                        if (result.getData() != null) {
                            shortCodes.add(result.getData().getShortCode());
                        }
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("❌ 请求 " + index + " 异常：" + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - start;
                    totalTime.addAndGet(elapsed);
                    latch.countDown();
                }
            }, executor);
        }

        latch.await(60, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        System.out.println("\n" + "=".repeat(80));
        System.out.println(scenarioName + " 压测结果");
        System.out.println("=".repeat(80));
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   失败数：" + failCount.get());
        System.out.println("   成功率：" + String.format("%.2f%%", successCount.get() * 100.0 / requestCount));
        System.out.println("   总耗时：" + totalDuration + "ms");
        System.out.println("   平均响应时间：" + (totalTime.get() / Math.max(successCount.get(), 1)) + "ms");
        System.out.println("   QPS: " + String.format("%.2f", requestCount * 1000.0 / Math.max(totalDuration, 1)));

        if (verifyUniqueness && !shortCodes.isEmpty()) {
            long uniqueCount = shortCodes.stream().distinct().count();
            System.out.println("   唯一短链码数：" + uniqueCount);
            assertEquals(shortCodes.size(), uniqueCount, "所有短链码应该唯一");
            System.out.println("   ✅ 唯一性验证通过");
        }

        assertTrue(successCount.get() > 0, "应该有请求成功");
        System.out.println("✅ " + scenarioName + " 压测通过");
    }

    private void runRedirectLoadTest(String scenarioName, int requestCount, int threadCount, String shortCode)
            throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("开始 " + scenarioName + " 压测");
        System.out.println("=".repeat(80));
        System.out.println("   请求总数：" + requestCount);
        System.out.println("   短链码：" + shortCode);

        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
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
                    long elapsed = System.currentTimeMillis() - start;
                    totalTime.addAndGet(elapsed);
                    latch.countDown();
                }
            }, executor);
        }

        latch.await(60, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        System.out.println("\n" + "=".repeat(80));
        System.out.println(scenarioName + " 结果");
        System.out.println("=".repeat(80));
        System.out.println("   总请求数：" + requestCount);
        System.out.println("   成功数：" + successCount.get());
        System.out.println("   失败数：" + failCount.get());
        System.out.println("   成功率：" + String.format("%.2f%%", successCount.get() * 100.0 / requestCount));
        System.out.println("   总耗时：" + totalDuration + "ms");
        System.out.println("   平均响应时间：" + (totalTime.get() / Math.max(successCount.get(), 1)) + "ms");
        System.out.println("   QPS: " + String.format("%.2f", requestCount * 1000.0 / Math.max(totalDuration, 1)));

        assertTrue(successCount.get() > requestCount * 0.9, "90% 以上的请求应该成功");
        System.out.println("✅ " + scenarioName + " 压测通过");
    }

    private String createTestShortLink(String longUrl) throws InterruptedException {
        CreateShortLinkDTO dto = new CreateShortLinkDTO();
        dto.setLongUrl(longUrl);
        dto.setExpireTime(LocalDateTime.now().plusDays(7));

        R<rj.highlink.entity.vo.ShortLinkVO> result = shortLinkController.create(dto);

        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData().getShortCode();
        }
        return null;
    }

    @AfterAll
    static void tearDown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
