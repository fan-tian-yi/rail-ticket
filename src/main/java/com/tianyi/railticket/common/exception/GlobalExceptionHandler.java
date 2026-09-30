package com.tianyi.railticket.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tianyi.railticket.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（GET/POST 的 @Valid + 类型绑定失败） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        ObjectError err = e.getBindingResult().getAllErrors().get(0);
        // typeMismatch 是类型转换失败，默认英文，单独给中文；其余用注解里的中文
        String msg = err.getCode() != null && err.getCode().startsWith("typeMismatch")
                ? "参数格式不正确"
                : err.getDefaultMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 请求体解析失败（POST @RequestBody，Jackson 抛） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), resolveBodyError(e));
    }

    /** 接口不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handle404(NoResourceFoundException e) {
        return Result.fail(404, "接口不存在: " + e.getResourcePath());
    }

    /** 未登录 / token 失效（Sa-Token 拦截器与 StpUtil 抛） */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问 | type={}", e.getType());
        return Result.fail(ErrorCode.NOT_LOGIN.getCode(), ErrorCode.NOT_LOGIN.getMessage());
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }

    /** 反序列化根因 → 人话 */
    private String resolveBodyError(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() == LocalDate.class) {
            return "日期格式不正确，应为 yyyy-MM-dd";
        }
        return "请求体格式错误或为空";
    }
}
