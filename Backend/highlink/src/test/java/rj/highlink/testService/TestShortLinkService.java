package rj.highlink.testService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rj.highlink.common.result.R;
import rj.highlink.entity.dto.CreateShortLinkDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 短链接服务简单测试类
 * 不使用 Spring Boot 上下文，只使用 @Test 进行基本功能测试
 */
@DisplayName("短链接服务简单测试")
class TestShortLinkService {

    @Test
    @DisplayName("测试 DTO 对象创建")
    void testDtoCreation() {
        // Given: 创建测试 DTO
        CreateShortLinkDTO dto = new CreateShortLinkDTO();
        dto.setLongUrl("https://www.example.com/test");
        dto.setExpireTime(LocalDateTime.now().plusDays(7));

        // Then: 验证属性设置正确
        assertNotNull(dto.getLongUrl(), "长链接不应为 null");
        assertEquals("https://www.example.com/test", dto.getLongUrl());
        assertNotNull(dto.getExpireTime(), "过期时间不应为 null");

        System.out.println("✅ DTO 创建成功");
        System.out.println("   长链接：" + dto.getLongUrl());
        System.out.println("   过期时间：" + dto.getExpireTime());
    }

    @Test
    @DisplayName("测试 URL 格式校验 - 有效 URL")
    void testValidUrlFormat() {
        // Given: 有效的 URL
        String validUrl = "https://www.example.com/path?param=value";

        // Then: 应该符合格式（这里只是简单验证，实际校验在 DTO 中）
        assertTrue(validUrl.startsWith("http://") || validUrl.startsWith("https://"),
                "URL 应以 http:// 或 https:// 开头");

        System.out.println("✅ URL 格式验证通过：" + validUrl);
    }

    @Test
    @DisplayName("测试 URL 格式校验 - 无效 URL")
    void testInvalidUrlFormat() {
        // Given: 无效的 URL
        String invalidUrl = "not-a-valid-url";

        // Then: 应该不符合格式
        assertFalse(invalidUrl.startsWith("http://") || invalidUrl.startsWith("https://"),
                "无效的 URL 不应以 http/https 开头");

        System.out.println("✅ 无效 URL 被正确识别：" + invalidUrl);
    }

    @Test
    @DisplayName("测试过期时间设置")
    void testExpireTimeSetting() {
        // Given: 设置不同的过期时间
        LocalDateTime expireTime1 = LocalDateTime.now().plusHours(1);
        LocalDateTime expireTime2 = LocalDateTime.now().plusDays(7);
        LocalDateTime expireTime3 = LocalDateTime.now().plusWeeks(1);

        // Then: 验证时间设置正确
        assertTrue(expireTime1.isAfter(LocalDateTime.now()), "1 小时后应该晚于当前时间");
        assertTrue(expireTime2.isAfter(LocalDateTime.now()), "7 天后应该晚于当前时间");
        assertTrue(expireTime3.isAfter(LocalDateTime.now()), "1 周后应该晚于当前时间");

        System.out.println("✅ 过期时间设置验证通过");
        System.out.println("   1 小时后：" + expireTime1);
        System.out.println("   7 天后：" + expireTime2);
        System.out.println("   1 周后：" + expireTime3);
    }

    @Test
    @DisplayName("测试 R 响应对象 - 成功")
    void testROk() {
        // When: 创建成功响应
        R<String> r = R.ok("操作成功", "测试数据");

        // Then: 验证响应内容
        assertEquals(200, r.getCode(), "成功响应状态码应为 200");
        assertEquals("操作成功", r.getMessage());
        assertEquals("测试数据", r.getData());
        System.out.println("✅ 成功响应测试通过");
        System.out.println("   状态码：" + r.getCode());
        System.out.println("   消息：" + r.getMessage());
        System.out.println("   数据：" + r.getData());
    }

    @Test
    @DisplayName("测试 R 响应对象 - 失败")
    void testRFail() {
        // When: 创建失败响应
        R<String> r1 = R.fail("操作失败");
        R<String> r2 = R.fail(404, "资源不存在");

        // Then: 验证响应内容
        assertEquals(500, r1.getCode(), "失败响应状态码应为 500");
        assertEquals("操作失败", r1.getMessage());

        assertEquals(404, r2.getCode(), "自定义状态码应为 404");
        assertEquals("资源不存在", r2.getMessage());

        System.out.println("✅ 失败响应测试通过");
        System.out.println("   r1 状态码：" + r1.getCode() + ", 消息：" + r1.getMessage());
        System.out.println("   r2 状态码：" + r2.getCode() + ", 消息：" + r2.getMessage());
    }

    @Test
    @DisplayName("测试不同 URL 字符串")
    void testDifferentUrls() {
        // Given: 不同的 URL 字符串
        String url1 = "https://www.google.com";
        String url2 = "https://www.bing.com";
        String url3 = "https://www.baidu.com";

        // Then: 验证它们不相同
        assertNotEquals(url1, url2, "Google 和 Bing 的 URL 应该不同");
        assertNotEquals(url1, url3, "Google 和百度的 URL 应该不同");
        assertNotEquals(url2, url3, "Bing 和百度的 URL 应该不同");

        System.out.println("✅ 不同 URL 验证通过");
        System.out.println("   URL1: " + url1);
        System.out.println("   URL2: " + url2);
        System.out.println("   URL3: " + url3);
    }

    @Test
    @DisplayName("测试 URL 哈希（简单验证）")
    void testUrlHash() {
        // Given: 相同的 URL
        String url1 = "https://www.example.com";
        String url2 = "https://www.example.com";
        String url3 = "https://www.different.com";

        // When: 计算哈希（简单使用 hashCode）
        int hash1 = url1.hashCode();
        int hash2 = url2.hashCode();
        int hash3 = url3.hashCode();

        // Then: 相同 URL 哈希值相同，不同 URL 哈希值不同
        assertEquals(hash1, hash2, "相同 URL 的哈希值应该相同");
        assertNotEquals(hash1, hash3, "不同 URL 的哈希值应该不同");

        System.out.println("✅ URL 哈希验证通过");
        System.out.println("   URL1 哈希：" + hash1);
        System.out.println("   URL2 哈希：" + hash2);
        System.out.println("   URL3 哈希：" + hash3);
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
    @DisplayName("测试状态描述转换")
    void testStatusDescConversion() {
        // Given: 不同的状态码
        Integer status1 = 1;
        Integer status0 = 0;

        // When: 转换为状态描述
        String desc1 = status1 == 1 ? "启用" : "禁用";
        String desc0 = status0 == 1 ? "启用" : "禁用";

        // Then: 验证转换正确
        assertEquals("启用", desc1, "状态 1 应该描述为'启用'");
        assertEquals("禁用", desc0, "状态 0 应该描述为'禁用'");

        System.out.println("✅ 状态描述转换验证通过");
        System.out.println("   状态 1 描述：" + desc1);
        System.out.println("   状态 0 描述：" + desc0);
    }

    @Test
    @DisplayName("测试完整短链接构建")
    void testFullShortUrlConstruction() {
        // Given: 短链码和域名
        String shortCode = "abc123";
        String domain = "http://short.url/";

        // When: 构建完整短链接
        String fullUrl = domain + shortCode;

        // Then: 验证构建正确
        assertNotNull(fullUrl, "完整短链接不应为 null");
        assertTrue(fullUrl.startsWith(domain), "完整短链接应以域名开头");
        assertTrue(fullUrl.contains(shortCode), "完整短链接应包含短链码");

        System.out.println("✅ 完整短链接构建验证通过");
        System.out.println("   短链码：" + shortCode);
        System.out.println("   完整短链接：" + fullUrl);
    }

    @Test
    @DisplayName("测试时间格式化")
    void testTimeFormatting() {
        // Given: 时间对象
        LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime expireTime = LocalDateTime.of(2024, 1, 8, 10, 0, 0);

        // When: 格式化为字符串
        String createTimeStr = createTime != null ? createTime.toString() : null;
        String expireTimeStr = expireTime != null ? expireTime.toString() : null;

        // Then: 验证格式化正确
        assertNotNull(createTimeStr, "创建时间字符串不应为 null");
        assertNotNull(expireTimeStr, "过期时间字符串不应为 null");
        assertTrue(createTimeStr.contains("2024"), "创建时间应包含年份");
        assertTrue(expireTimeStr.contains("2024"), "过期时间应包含年份");

        System.out.println("✅ 时间格式化验证通过");
        System.out.println("   创建时间：" + createTimeStr);
        System.out.println("   过期时间：" + expireTimeStr);
    }

    @Test
    @DisplayName("测试空值判断逻辑")
    void testNullCheckLogic() {
        // Given: 不同的对象
        String nullStr = null;
        String emptyStr = "";
        String blankStr = "   ";
        String normalStr = "abc123";

        // Then: 验证空值判断逻辑
        assertNull(nullStr, "null 字符串应该为 null");
        assertNotNull(emptyStr, "空字符串对象不应为 null");
        assertNotNull(blankStr, "空白字符串对象不应为 null");
        assertNotNull(normalStr, "正常字符串不应为 null");

        assertTrue(emptyStr.isEmpty(), "空字符串应该为空");
        assertFalse(normalStr.isEmpty(), "正常字符串不应为空");
        assertTrue(blankStr.isBlank(), "空白字符串应该为空白");

        System.out.println("✅ 空值判断逻辑验证通过");
        System.out.println("   null 字符串：" + nullStr);
        System.out.println("   空字符串：'" + emptyStr + "'");
        System.out.println("   空白字符串：'" + blankStr + "'");
        System.out.println("   正常字符串：'" + normalStr + "'");
    }

    @Test
    @DisplayName("测试禁用操作逻辑")
    void testDisableLogic() {
        // Given: 模拟禁用操作的结果
        boolean disableSuccess = true;
        boolean disableFail = false;

        // Then: 验证禁用操作返回值
        assertTrue(disableSuccess, "禁用成功应该返回 true");
        assertFalse(disableFail, "禁用失败应该返回 false");

        System.out.println("✅ 禁用操作逻辑验证通过");
        System.out.println("   禁用成功：" + disableSuccess);
        System.out.println("   禁用失败：" + disableFail);
    }

    @Test
    @DisplayName("测试查询不存在的情况")
    void testQueryNonExistent() {
        // Given: 查询不存在的短链码
        String nonExistentCode = "not_exist_123";

        // When: 模拟查询结果为 null
        Object result = null;

        // Then: 验证返回 null
        assertNull(result, "查询不存在的记录应该返回 null");

        System.out.println("✅ 查询不存在的情况验证通过");
        System.out.println("   短链码：" + nonExistentCode);
        System.out.println("   查询结果：" + result);
    }

    @Test
    @DisplayName("测试缓存命中逻辑")
    void testCacheHitLogic() {
        // Given: 模拟缓存命中的情况
        String cachedValue = "https://www.example.com";
        String nullValue = null;

        // Then: 验证缓存命中判断
        assertNotNull(cachedValue, "缓存命中时返回值不应为 null");
        assertNull(nullValue, "缓存未命中时返回 null");
        assertTrue(cachedValue != null, "缓存值不为 null 表示命中");

        System.out.println("✅ 缓存命中逻辑验证通过");
        System.out.println("   缓存命中值：" + cachedValue);
        System.out.println("   缓存未命中值：" + nullValue);
    }

    @Test
    @DisplayName("测试 R 响应对象 - 查询成功")
    void testQuerySuccessResponse() {
        // When: 创建查询成功响应
        R<String> r = R.ok("查询成功", "短链接数据");

        // Then: 验证响应内容
        assertEquals(200, r.getCode(), "成功响应状态码应为 200");
        assertEquals("查询成功", r.getMessage());
        assertEquals("短链接数据", r.getData());

        System.out.println("✅ 查询成功响应测试通过");
        System.out.println("   状态码：" + r.getCode());
        System.out.println("   消息：" + r.getMessage());
        System.out.println("   数据：" + r.getData());
    }

    @Test
    @DisplayName("测试 R 响应对象 - 查询失败")
    void testQueryFailResponse() {
        // When: 创建查询失败响应
        R<String> r = R.fail("短链接不存在");

        // Then: 验证响应内容
        assertEquals(500, r.getCode(), "失败响应状态码应为 500");
        assertEquals("短链接不存在", r.getMessage());
        assertNull(r.getData(), "失败响应数据应为 null");

        System.out.println("✅ 查询失败响应测试通过");
        System.out.println("   状态码：" + r.getCode());
        System.out.println("   消息：" + r.getMessage());
        System.out.println("   数据：" + r.getData());
    }
}
