package com.nan.waveform.common.exception;
import com.nan.waveform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * @author nan chao
 * @since 2026/6/9 21:07
 */

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice // 这个注解会自动拦截所有 Controller 抛出的异常
public class GlobalExceptionHandler {

    /**
     * 捕获所有的 Exception.class
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        // 在控制台打印异常堆栈信息，方便排查问题
        log.error("系统发生未知异常: ", e);
        // 返回统一的 JSON 格式给前端
        return Result.error(500, "系统繁忙，请稍后再试：" + e.getMessage());
    }

    // 后续如果有自定义的业务异常 (如 BusinessException)，也可以在这里加方法拦截
}
