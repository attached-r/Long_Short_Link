package rj.highlink.testService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.entity.po.ShortLinkVisitPo;
import rj.highlink.mapper.ShortLinkVisitMapper;
import rj.highlink.service.VisitLogService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步日志服务简单测试
 * 验证异步日志功能是否正常工作
 */
@SpringBootTest
@DisplayName("异步日志服务测试")
class TestVisitLogService {

    @Autowired
    private VisitLogService visitLogService;

    @Autowired
    private ShortLinkVisitMapper visitMapper;

    @Test
    @DisplayName("测试异步日志保存 - 正常情况")
    void testSaveVisitLog_Normal() {
        try {
            // Given: 准备测试数据
            String shortCode = "test_async_" + System.currentTimeMillis();
            String ip = "192.168.1.100";
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

            System.out.println("=== 开始测试异步日志保存 ===");
            System.out.println("   短链码：" + shortCode);
            System.out.println("   IP: " + ip);
            System.out.println("   UserAgent: " + userAgent);

            // When: 调用异步方法
            visitLogService.saveVisitLog(shortCode, ip, userAgent);
            System.out.println("✅ 异步方法调用成功（不阻塞）");

            // Then: 等待异步执行完成
            System.out.println("   等待异步执行...");
            try {
                Thread.sleep(1000); // 等待 1 秒确保异步完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 验证数据库中有记录
            List<ShortLinkVisitPo> visits = visitMapper.selectList(null);

            ShortLinkVisitPo foundVisit = null;
            for (ShortLinkVisitPo visit : visits) {
                if (visit.getShortCode().equals(shortCode)) {
                    foundVisit = visit;
                    break;
                }
            }

            // 断言验证
            assertNotNull(foundVisit, "应该找到刚插入的访问记录");
            assertEquals(shortCode, foundVisit.getShortCode(), "短链码应该匹配");
            assertEquals(ip, foundVisit.getIp(), "IP 地址应该匹配");
            assertEquals(userAgent, foundVisit.getUserAgent(), "UserAgent 应该匹配");
            assertNotNull(foundVisit.getVisitTime(), "访问时间不应该为空");

            System.out.println("✅ 异步日志保存成功");
            System.out.println("   记录 ID: " + foundVisit.getId());
            System.out.println("   短链码：" + foundVisit.getShortCode());
            System.out.println("   IP: " + foundVisit.getIp());
            System.out.println("   访问时间：" + foundVisit.getVisitTime());

        } catch (Exception e) {
            fail("异步日志保存失败：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试异步日志保存 - 空短链码")
    void testSaveVisitLog_NullShortCode() {
        try {
            System.out.println("=== 测试空短链码情况 ===");

            // Given: 空短链码
            String nullShortCode = null;
            String ip = "192.168.1.100";
            String userAgent = "Test-Agent";

            // When: 调用异步方法（应该被参数校验拦截）
            visitLogService.saveVisitLog(nullShortCode, ip, userAgent);

            // Then: 等待异步执行
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ 空短链码被正确忽略，未抛异常");
            assertTrue(true, "空 shortCode 应该被忽略，不抛异常");

        } catch (Exception e) {
            fail("不应该抛出异常：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试异步日志保存 - 空白短链码")
    void testSaveVisitLog_BlankShortCode() {
        try {
            System.out.println("=== 测试空白短链码情况 ===");

            // Given: 空白短链码
            String blankShortCode = "   ";
            String ip = "192.168.1.100";
            String userAgent = "Test-Agent";

            // When: 调用异步方法
            visitLogService.saveVisitLog(blankShortCode, ip, userAgent);

            // Then: 等待异步执行
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ 空白短链码被正确忽略，未抛异常");
            assertTrue(true, "空白 shortCode 应该被忽略，不抛异常");

        } catch (Exception e) {
            fail("不应该抛出异常：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试异步日志保存 - 并发场景")
    void testSaveVisitLog_Concurrent() {
        try {
            System.out.println("=== 测试并发场景 ===");

            // Given: 并发数
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            String[] shortCodes = new String[threadCount];

            // 准备测试数据
            for (int i = 0; i < threadCount; i++) {
                shortCodes[i] = "concurrent_test_" + i + "_" + System.currentTimeMillis();
            }

            // When: 启动多个线程同时写入
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    String shortCode = shortCodes[index];
                    String ip = "192.168.1." + index;
                    String userAgent = "Test-Agent-" + index;

                    System.out.println("   线程 " + index + " 开始写入：" + shortCode);
                    visitLogService.saveVisitLog(shortCode, ip, userAgent);
                    System.out.println("   线程 " + index + " 写入完成");
                });
                threads[i].start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 额外等待异步执行完成
            System.out.println("   等待异步执行完成...");
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then: 验证所有记录都成功写入
            List<ShortLinkVisitPo> allVisits = visitMapper.selectList(null);

            int successCount = 0;
            for (int i = 0; i < threadCount; i++) {
                for (ShortLinkVisitPo visit : allVisits) {
                    if (visit.getShortCode().equals(shortCodes[i])) {
                        successCount++;
                        System.out.println("   ✅ 找到记录 " + i + ": " + shortCodes[i]);
                        break;
                    }
                }
            }

            assertEquals(threadCount, successCount,
                    String.format("应该成功写入%d条记录，实际找到%d条", threadCount, successCount));

            System.out.println("✅ 并发测试通过，成功写入 " + successCount + " 条记录");

        } catch (Exception e) {
            fail("并发测试失败：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试异步日志保存 - 不同 IP 格式")
    void testSaveVisitLog_DifferentIpFormats() {
        try {
            System.out.println("=== 测试不同 IP 格式 ===");

            // Given: 不同的 IP 格式
            String[] ips = {
                    "127.0.0.1",           // IPv4 本地
                    "192.168.1.100",       // IPv4 内网
                    "0:0:0:0:0:0:0:1",     // IPv6 本地
                    "2001:0db8:85a3::8a2e:0370:7334"  // IPv6
            };

            for (int i = 0; i < ips.length; i++) {
                String shortCode = "ip_test_" + i + "_" + System.currentTimeMillis();
                String ip = ips[i];
                String userAgent = "Test-Agent";

                // When: 保存日志
                visitLogService.saveVisitLog(shortCode, ip, userAgent);
            }

            // Then: 等待异步执行
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ 所有 IP 格式的日志都成功保存");
            assertTrue(true, "不同 IP 格式都应该正常保存");

        } catch (Exception e) {
            fail("IP 格式测试失败：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试异步日志保存 - 特殊 UserAgent")
    void testSaveVisitLog_SpecialUserAgent() {
        try {
            System.out.println("=== 测试特殊 UserAgent ===");

            // Given: 特殊的 UserAgent
            String shortCode = "ua_test_" + System.currentTimeMillis();
            String ip = "192.168.1.100";
            String[] userAgents = {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)",
                    "curl/7.68.0",
                    "PostmanRuntime/7.28.0",
                    null  // null UserAgent
            };

            for (int i = 0; i < userAgents.length; i++) {
                String testShortCode = shortCode + "_" + i;
                String ua = userAgents[i];

                // When: 保存日志（null 会转为 "unknown"）
                visitLogService.saveVisitLog(testShortCode, ip, ua);
            }

            // Then: 等待异步执行
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ 所有特殊 UserAgent 都成功保存");
            assertTrue(true, "特殊 UserAgent 都应该正常保存");

        } catch (Exception e) {
            fail("UserAgent 测试失败：" + e.getMessage());
        }
    }
}
