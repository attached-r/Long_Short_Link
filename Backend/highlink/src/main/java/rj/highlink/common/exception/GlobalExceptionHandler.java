package rj.highlink.common.exception;

import rj.highlink.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice // 注册为 Spring Bean，统一处理异常
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return R.fail(500, "系统繁忙，请稍后重试");
    }
}