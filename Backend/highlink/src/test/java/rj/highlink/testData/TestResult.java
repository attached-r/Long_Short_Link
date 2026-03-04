package rj.highlink.testData;

import org.junit.jupiter.api.Test;
import rj.highlink.common.result.R;

/*
 * 测试结果类
 */
public class TestResult {

    @Test //测试 统一成功返回
    public void testRok() {
        System.out.println("测试成功返回");
        R<String> r = R.ok("操作成功", "数据"); // 使用 R 类的静态方法创建一个成功结果
        System.out.println(r);
    }

    @Test
    public void testRfail() {
        System.out.println("测试失败返回");
        R<String> r = R.fail("操作失败");
        System.out.println(r);
        R<String> r1 = R.fail(404, "操作失败");
        System.out.println(r1);
    }
}
