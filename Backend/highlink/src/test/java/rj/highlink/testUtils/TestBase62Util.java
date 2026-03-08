package rj.highlink.testUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rj.highlink.utils.Base62Util;
import rj.highlink.utils.IdGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base62Util 编码工具测试
 */
@DisplayName("Base62 编码工具测试")
public class TestBase62Util {

    @Test
    @DisplayName("测试基础编码功能")
    public void testEncode_Basic() {
        long id = IdGenerator.nextId();
        String encoded = Base62Util.encode(id, null);

        assertNotNull(encoded, "编码结果不应为 null");
        assertFalse(encoded.isEmpty(), "编码结果不应为空");
        System.out.println("✅ 基础编码测试通过");
        System.out.println("   雪花 ID: " + id);
        System.out.println("   Base62 编码：" + encoded);
    }

    @Test
    @DisplayName("测试边界值 - 0")
    public void testEncode_Zero() {
        String encoded = Base62Util.encode(0, null);

        // 当 num = 0 时，while 循环不执行，返回空字符串
        assertEquals("", encoded, "0 的 Base62 编码应该是空字符串（while 循环不执行）");
        System.out.println("✅ 边界值 0 测试通过");
        System.out.println("   0 的 Base62 编码：'" + encoded + "'");
    }

    @Test
    @DisplayName("测试边界值 - 1")
    public void testEncode_One() {
        String encoded = Base62Util.encode(1, null);

        assertNotNull(encoded, "编码结果不应为 null");
        assertEquals("1", encoded, "1 的 Base62 编码应该是 '1'");
        System.out.println("✅ 边界值 1 测试通过：" + encoded);
    }

    @Test
    @DisplayName("测试边界值 - Long.MAX_VALUE")
    public void testEncode_MaxValue() {
        String encoded = Base62Util.encode(Long.MAX_VALUE, null);

        assertNotNull(encoded, "编码结果不应为 null");
        assertFalse(encoded.isEmpty(), "编码结果不应为空");
        System.out.println("✅ 边界值 Long.MAX_VALUE 测试通过：" + encoded);
    }

    @Test
    @DisplayName("测试编码唯一性")
    public void testEncode_Uniqueness() {
        long id1 = IdGenerator.nextId();
        long id2 = IdGenerator.nextId();

        String encoded1 = Base62Util.encode(id1, null);
        String encoded2 = Base62Util.encode(id2, null);

        assertNotEquals(encoded1, encoded2, "不同 ID 的 Base62 编码应该不同");
        System.out.println("✅ 编码唯一性测试通过");
    }

    @Test
    @DisplayName("测试编码长度")
    public void testEncode_Length() {
        long smallId = 1000;
        long largeId = IdGenerator.nextId();

        String smallEncoded = Base62Util.encode(smallId, null);
        String largeEncoded = Base62Util.encode(largeId, null);

        assertNotNull(smallEncoded, "小编码结果不应为 null");
        assertNotNull(largeEncoded, "大编码结果不应为 null");
        assertTrue(largeEncoded.length() >= smallEncoded.length(),
                "大 ID 的编码长度应该不小于小 ID 的编码长度");

        System.out.println("✅ 编码长度测试通过");
        System.out.println("   小 ID 编码：" + smallEncoded + " (长度：" + smallEncoded.length() + ")");
        System.out.println("   大 ID 编码：" + largeEncoded + " (长度：" + largeEncoded.length() + ")");
    }

    @Test
    @DisplayName("测试编码字符集")
    public void testEncode_CharacterSet() {
        for (int i = 0; i < 100; i++) {
            long id = IdGenerator.nextId();
            String encoded = Base62Util.encode(id, null);

            // 验证编码只包含 Base62 字符（0-9, a-z, A-Z）
            assertTrue(encoded.matches("[0-9a-zA-Z]+"),
                    "Base62 编码应该只包含数字和字母：" + encoded);
        }
        System.out.println("✅ 编码字符集测试通过");
    }

    @Test
    @DisplayName("测试负数处理")
    public void testEncode_Negative() {
        // Base62 会对负数进行运算，最终结果为空字符串
        String encoded = Base62Util.encode(-1, null);

        // 负数编码结果为空字符串（因为 num > 0 条件不满足，循环不执行）
        assertEquals("", encoded, "负数编码结果应该为空字符串");

        System.out.println("✅ 负数处理测试通过");
        System.out.println("   负数 -1 编码结果：'" + encoded + "'");
    }
}
