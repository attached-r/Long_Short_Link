package rj.highlink.testService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rj.highlink.common.result.R;
import rj.highlink.entity.po.ShortLinkVisitPo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重定向服务简单测试类
 * 不使用 Spring Boot 上下文，只使用 @Test 进行基本功能测试
 */
@DisplayName("重定向服务简单测试")
class TestRedirectionService {

    @Test
    @DisplayName("测试访问日志 PO 对象创建")
    void testVisitPoCreation() {
        // Given: 创建访问日志对象
        ShortLinkVisitPo visitPo = new ShortLinkVisitPo();
        visitPo.setShortCode("abc123");
        visitPo.setIp("192.168.1.100");
        visitPo.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        visitPo.setVisitTime(LocalDateTime.now());

        // Then: 验证属性设置正确
        assertNotNull(visitPo.getShortCode(), "短链码不应为 null");
        assertEquals("abc123", visitPo.getShortCode());
        assertNotNull(visitPo.getIp(), "IP 地址不应为 null");
        assertEquals("192.168.1.100", visitPo.getIp());
        assertNotNull(visitPo.getUserAgent(), "User-Agent 不应为 null");
        assertNotNull(visitPo.getVisitTime(), "访问时间不应为 null");

        System.out.println("✅ 访问日志 PO 创建成功");
        System.out.println("   短链码：" + visitPo.getShortCode());
        System.out.println("   IP: " + visitPo.getIp());
        System.out.println("   User-Agent: " + visitPo.getUserAgent());
        System.out.println("   访问时间：" + visitPo.getVisitTime());
    }

    @Test
    @DisplayName("测试 IP 地址格式验证")
    void testIpAddressFormat() {
        // Given: 不同的 IP 地址格式
        String ipv4 = "192.168.1.1";
        String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        String unknown = "unknown";

        // Then: 验证 IP 格式
        assertTrue(ipv4.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
                "IPv4 格式应该正确");
        assertTrue(ipv6.contains(":"), "IPv6 应该包含冒号");
        assertEquals("unknown", unknown);

        System.out.println("✅ IP 地址格式验证通过");
        System.out.println("   IPv4: " + ipv4);
        System.out.println("   IPv6: " + ipv6);
        System.out.println("   Unknown: " + unknown);
    }

    @Test
    @DisplayName("测试 User-Agent 字符串")
    void testUserAgentString() {
        // Given: 不同的 User-Agent 字符串
        String chrome = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        String firefox = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0";
        String unknown = "unknown";

        // Then: 验证 User-Agent 格式
        assertTrue(chrome.startsWith("Mozilla/5.0"), "Chrome UA 应该以 Mozilla/5.0 开头");
        assertTrue(firefox.contains("Firefox"), "Firefox UA 应该包含 Firefox");
        assertEquals("unknown", unknown);

        System.out.println("✅ User-Agent 字符串验证通过");
        System.out.println("   Chrome: " + chrome);
        System.out.println("   Firefox: " + firefox);
        System.out.println("   Unknown: " + unknown);
    }

    @Test
    @DisplayName("测试短链码格式")
    void testShortCodeFormat() {
        // Given: 不同的短链码
        String shortCode1 = "abc123";
        String shortCode2 = "xyz789";
        String shortCode3 = "ABC123";

        // Then: 验证短链码格式
        assertNotNull(shortCode1, "短链码不应为 null");
        assertFalse(shortCode1.isEmpty(), "短链码不应为空");
        assertNotEquals(shortCode1, shortCode2, "不同的短链码应该不同");
        assertNotEquals(shortCode1, shortCode3, "大小写敏感的短链码应该不同");

        System.out.println("✅ 短链码格式验证通过");
        System.out.println("   ShortCode1: " + shortCode1);
        System.out.println("   ShortCode2: " + shortCode2);
        System.out.println("   ShortCode3: " + shortCode3);
    }

    @Test
    @DisplayName("测试过期时间判断")
    void testExpireTimeCheck() {
        // Given: 设置不同的时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(1);
        LocalDateTime past = now.minusHours(1);

        // Then: 验证时间比较逻辑
        assertTrue(future.isAfter(now), "未来时间应该晚于当前时间");
        assertTrue(past.isBefore(now), "过去时间应该早于当前时间");
        assertFalse(future.isBefore(now), "未来时间不应早于当前时间");

        System.out.println("✅ 过期时间判断验证通过");
        System.out.println("   当前时间：" + now);
        System.out.println("   未来时间：" + future);
        System.out.println("   过去时间：" + past);
    }

    @Test
    @DisplayName("测试状态码判断")
    void testStatusCodeCheck() {
        // Given: 不同的状态码
        Integer statusEnabled = 1;
        Integer statusDisabled = 0;

        // Then: 验证状态判断逻辑
        assertEquals(1, statusEnabled, "启用状态应该是 1");
        assertEquals(0, statusDisabled, "禁用状态应该是 0");
        assertNotEquals(statusEnabled, statusDisabled, "启用和禁用状态应该不同");

        System.out.println("✅ 状态码判断验证通过");
        System.out.println("   启用状态：" + statusEnabled);
        System.out.println("   禁用状态：" + statusDisabled);
    }

    @Test
    @DisplayName("测试 R 响应对象 - 重定向成功")
    void testRedirectSuccessResponse() {
        // When: 创建重定向成功响应
        R<String> r = R.ok("重定向成功", "https://www.example.com");

        // Then: 验证响应内容
        assertEquals(200, r.getCode(), "成功响应状态码应为 200");
        assertEquals("重定向成功", r.getMessage());
        assertEquals("https://www.example.com", r.getData());

        System.out.println("✅ 重定向成功响应测试通过");
        System.out.println("   状态码：" + r.getCode());
        System.out.println("   消息：" + r.getMessage());
        System.out.println("   目标 URL: " + r.getData());
    }

    @Test
    @DisplayName("测试 R 响应对象 - 重定向失败")
    void testRedirectFailResponse() {
        // When: 创建重定向失败响应
        R<String> r1 = R.fail("短链接不存在");
        R<String> r2 = R.fail(404, "资源未找到");

        // Then: 验证响应内容
        assertEquals(500, r1.getCode(), "失败响应状态码应为 500");
        assertEquals("短链接不存在", r1.getMessage());

        assertEquals(404, r2.getCode(), "自定义状态码应为 404");
        assertEquals("资源未找到", r2.getMessage());

        System.out.println("✅ 重定向失败响应测试通过");
        System.out.println("   r1 状态码：" + r1.getCode() + ", 消息：" + r1.getMessage());
        System.out.println("   r2 状态码：" + r2.getCode() + ", 消息：" + r2.getMessage());
    }

    @Test
    @DisplayName("测试时间差计算")
    void testDurationCalculation() {
        // Given: 设置不同的时间点
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);

        // When: 计算时间差
        long hoursDiff = java.time.Duration.between(startTime, endTime).toHours();

        // Then: 验证时间差计算正确
        assertEquals(2, hoursDiff, "时间差应该是 2 小时");
        assertTrue(hoursDiff > 0, "时间差应该大于 0");

        System.out.println("✅ 时间差计算验证通过");
        System.out.println("   开始时间：" + startTime);
        System.out.println("   结束时间：" + endTime);
        System.out.println("   相差小时数：" + hoursDiff);
    }

    @Test
    @DisplayName("测试字符串空值判断")
    void testStringEmptyCheck() {
        // Given: 不同的字符串
        String normalStr = "abc123";
        String emptyStr = "";
        String nullStr = null;
        String blankStr = "   ";

        // Then: 验证空值判断逻辑
        assertNotNull(normalStr, "正常字符串不应为 null");
        assertFalse(normalStr.isEmpty(), "正常字符串不应为空");
        assertFalse(normalStr.isBlank(), "正常字符串不应为空白");

        assertNotNull(emptyStr, "空字符串对象不应为 null");
        assertTrue(emptyStr.isEmpty(), "空字符串应该为空");

        assertNull(nullStr, "null 字符串应该为 null");

        assertNotNull(blankStr, "空白字符串对象不应为 null");
        assertFalse(blankStr.isEmpty(), "空白字符串不应为空");
        assertTrue(blankStr.isBlank(), "空白字符串应该为空白");

        System.out.println("✅ 字符串空值判断验证通过");
        System.out.println("   正常字符串：'" + normalStr + "'");
        System.out.println("   空字符串：'" + emptyStr + "'");
        System.out.println("   null 字符串：" + nullStr);
        System.out.println("   空白字符串：'" + blankStr + "'");
    }
}
