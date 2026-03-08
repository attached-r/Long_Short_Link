package rj.highlink.testException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.common.exception.BusinessException;
import rj.highlink.entity.dto.CreateShortLinkDTO;
import rj.highlink.service.ShortLinkService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常测试
 */
@SpringBootTest
@DisplayName("业务异常测试")
public class TestBusinessException {

    @Autowired
    private ShortLinkService shortLinkService;

    @Test
    @DisplayName("测试黑名单 URL 创建短链 - 应抛异常")
    void testCreateBlackListUrl() {
        CreateShortLinkDTO dto = new CreateShortLinkDTO();
        // 使用黑名单中的域名（evil.com 在配置文件的黑名单中）
        dto.setLongUrl("http://evil.com/test");

        assertThrows(IllegalArgumentException.class, () -> {
            shortLinkService.create(dto);
        }, "黑名单 URL 应该抛出异常");
    }

    @Test
    @DisplayName("测试禁用不存在的短链接")
    void testDisableNonExistentShortLink() {
        boolean result = shortLinkService.disable("nonexistent_shortcode");

        assertFalse(result, "禁用不存在的短链接应该返回 false");
    }

    @Test
    @DisplayName("测试查询不存在的短链接信息")
    void testGetNonExistentShortLinkInfo() {
        var result = shortLinkService.getInfo("nonexistent_shortcode");

        assertNull(result, "不存在的短链接应该返回 null");
    }

    @Test
    @DisplayName("测试重复禁用同一短链接")
    void testDisableAlreadyDisabledShortLink() {
        CreateShortLinkDTO dto = new CreateShortLinkDTO();
        dto.setLongUrl("https://www.example.com/test_disable");
        dto.setExpireTime(LocalDateTime.now().plusHours(1));

        String shortCode = shortLinkService.create(dto);
        String code = extractShortCode(shortCode);

        boolean firstDisable = shortLinkService.disable(code);
        assertTrue(firstDisable, "第一次禁用应该成功");

        boolean secondDisable = shortLinkService.disable(code);
        assertTrue(secondDisable, "重复禁用应该返回成功（幂等性）");
    }

    @Test
    @DisplayName("测试 BusinessException 构造函数")
    void testBusinessException_Constructor() {
        BusinessException ex1 = new BusinessException("测试异常消息");
        assertEquals(500, ex1.getCode(), "默认错误码应该是 500");
        assertEquals("测试异常消息", ex1.getMessage());

        BusinessException ex2 = new BusinessException(404, "资源不存在");
        assertEquals(404, ex2.getCode(), "自定义错误码应该是 404");
        assertEquals("资源不存在", ex2.getMessage());
    }

    @Test
    @DisplayName("测试 BusinessException 可序列化")
    void testBusinessException_Serializable() {
        BusinessException ex = new BusinessException(400, "参数错误");

        assertNotNull(ex.getCode(), "错误码不应为 null");
        assertNotNull(ex.getMessage(), "消息不应为 null");
        assertInstanceOf(RuntimeException.class, ex, "应该是 RuntimeException 的子类");
    }

    private String extractShortCode(String fullShortUrl) {
        if (fullShortUrl == null || fullShortUrl.isEmpty()) {
            return null;
        }
        int lastIndex = fullShortUrl.lastIndexOf("/");
        if (lastIndex != -1 && lastIndex < fullShortUrl.length() - 1) {
            return fullShortUrl.substring(lastIndex + 1);
        }
        return fullShortUrl;
    }
}
