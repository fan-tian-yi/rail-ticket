package com.tianyi.railticket.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j                      // Lombok 注入 log 对象（需要 pom 里有 lombok，已加）
@RestControllerAdvice       // = @ControllerAdvice + @ResponseBody：返回值走 Jackson 变 JSON
public class GlobalExceptionHandler {

    /** 业务异常：Service 抛的 BizException，code/message 原样透出 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：GET+@Valid 抛 BindException（MethodArgumentNotValidException 是它子类，一并接住） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .findFirst()                        // 只取第一条错误，别把一坨都塞给前端
                .orElse("参数错误");
        return Result.fail(400, msg);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handle404(NoResourceFoundException e) {
        return Result.fail(404, "接口不存在: " + e.getResourcePath());
    }

    /** 兜底：所有没被上面两条接住的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);                   // 完整堆栈进日志——给自己人查问题用
        return Result.fail(500, "系统繁忙，请稍后重试"); // 模糊话术给前端——防内部信息泄露
    }
}
