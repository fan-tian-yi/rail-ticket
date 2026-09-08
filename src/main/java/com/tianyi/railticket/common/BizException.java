package com.tianyi.railticket.common;


import lombok.Getter;

/**
 * 业务异常：预期内的失败（参数不合法、余票不足等），会被
 * GlobalExceptionHandler 捕获后原样透出 code + message。
 *
 * <p>优先使用 {@link #BizException(ErrorCode)}，不要在业务代码里硬编码数字码。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);          // message 给父类（日志/getMessage 用）
        this.code = code;
    }

    /** 推荐用法：throw new BizException(ErrorCode.NO_TICKET); */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
