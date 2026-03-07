package rj.highlink.testUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.common.util.UrlBlackListUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * URL 黑名单工具类测试
 */
@SpringBootTest
@DisplayName("URL 黑名单测试")
class TestUrlBlackListUtil {

    @Autowired
    private UrlBlackListUtil urlBlackListUtil;

    @Test
    @DisplayName("测试黑名单域名 - 应该被拦截")
    void testBlackDomain() {
        String[] blackUrls = {
                "https://evil.com/test",
                "https://malicious-site.com/page",
                "https://phishing.com/login"
        };

        for (String url : blackUrls) {
            assertTrue(UrlBlackListUtil.isBlack(url),
                    "域名 " + url + " 应该在黑名单中");
        }

        System.out.println("✅ 黑名单域名测试通过");
    }

    @Test
    @DisplayName("测试黑名单关键词 - 应该被拦截")
    void testBlackKeyword() {
        String[] blackUrls = {
                "https://example.com/porn-video",
                "https://example.com/gambling-site",
                "https://example.com/fraud-alert"
        };

        for (String url : blackUrls) {
            assertTrue(UrlBlackListUtil.isBlack(url),
                    "URL " + url + " 应该包含黑名单关键词");
        }

        System.out.println("✅ 黑名单关键词测试通过");
    }

    @Test
    @DisplayName("测试正常 URL - 应该放行")
    void testNormalUrl() {
        String[] normalUrls = {
                "https://www.google.com",
                "https://www.github.com",
                "https://www.example.com/normal-page"
        };

        for (String url : normalUrls) {
            assertFalse(UrlBlackListUtil.isBlack(url),
                    "URL " + url + " 应该是安全的");
        }

        System.out.println("✅ 正常 URL 测试通过");
    }

    @Test
    @DisplayName("测试动态添加域名到黑名单")
    void testAddDomainToBlacklist() {
        String newDomain = "new-evil-domain.com";
        String testUrl = "https://" + newDomain + "/test";

        // 初始不在黑名单
        assertFalse(UrlBlackListUtil.isBlack(testUrl),
                "初始时域名不应在黑名单中");

        // 添加到黑名单
        UrlBlackListUtil.addDomainToBlacklist(newDomain);

        // 现在应该在黑名单中
        assertTrue(UrlBlackListUtil.isBlack(testUrl),
                "添加后域名应该在黑名单中");

        System.out.println("✅ 动态添加域名测试通过");
    }

    @Test
    @DisplayName("测试动态添加关键词到黑名单")
    void testAddKeywordToBlacklist() {
        String newKeyword = "illegal-content";
        String testUrl = "https://example.com/illegal-content-page";

        // 初始不在黑名单
        assertFalse(UrlBlackListUtil.isBlack(testUrl),
                "初始时关键词不应在黑名单中");

        // 添加到黑名单
        UrlBlackListUtil.addKeywordToBlacklist(newKeyword);

        // 现在应该在黑名单中
        assertTrue(UrlBlackListUtil.isBlack(testUrl),
                "添加后关键词应该在黑名单中");

        System.out.println("✅ 动态添加关键词测试通过");
    }

    @Test
    @DisplayName("测试无效 URL 格式 - 应该被拦截")
    void testInvalidUrlFormat() {
        String[] invalidUrls = {
                "not-a-url",
                "ht!tp://invalid.com",
                "",
                null
        };

        for (String url : invalidUrls) {
            assertTrue(UrlBlackListUtil.isBlack(url),
                    "无效 URL " + url + " 应该被拦截");
        }

        System.out.println("✅ 无效 URL 格式测试通过");
    }
}

