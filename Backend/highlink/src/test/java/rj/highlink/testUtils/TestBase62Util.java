package rj.highlink.testUtils;

import org.junit.jupiter.api.Test;
import rj.highlink.utils.Base62Util;
import rj.highlink.utils.IdGenerator;

// 测试 Base62Util 类
public class TestBase62Util {
    @Test
    public void testEncode() { // 测试编码方法
        // 生成一个雪花Id
        long id = IdGenerator.nextId();

        System.out.println("雪花ID："  + id);
        System.out.println("Base62编码："+Base62Util.encode(id, null));
    }
}
