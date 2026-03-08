package rj.highlink.testException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rj.highlink.common.exception.BusinessException;
import rj.highlink.common.exception.GlobalExceptionHandler;
import rj.highlink.common.result.R;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器测试
 */
@SpringBootTest
@DisplayName("全局异常处理器测试")
public class TestGlobalExceptionHandler {

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("测试处理 BusinessException")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException(400, "参数错误");

        R<?> result = exceptionHandler.handleBusinessException(ex);

        assertNotNull(result, "返回结果不应为 null");
        assertEquals(400, result.getCode(), "错误码应该是 400");
        assertEquals("参数错误", result.getMessage(), "消息应该匹配");
    }

    @Test
    @DisplayName("测试处理普通 Exception")
    void testHandleException() {
        Exception ex = new RuntimeException("系统异常");

        R<?> result = exceptionHandler.handleException(ex);

        assertNotNull(result, "返回结果不应为 null");
        assertEquals(500, result.getCode(), "错误码应该是 500");
        assertEquals("系统繁忙，请稍后重试", result.getMessage(), "消息应该是固定的友好提示");
    }

    @Test
    @DisplayName("测试处理 IllegalArgumentException")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("非法参数");

        R<?> result = exceptionHandler.handleIllegalArgumentException(ex);

        assertNotNull(result, "返回结果不应为 null");
        assertEquals(400, result.getCode(), "错误码应该是 400");
        assertEquals("非法参数", result.getMessage(), "消息应该匹配");
    }
}
