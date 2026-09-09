package com.tianyi.railticket.common;


import lombok.Getter;

/** 业务异常：预期内的失败，被 GlobalExceptionHandler 捕获后原样透出 code + message */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    /** 直接指定 code + message（尽量用 ErrorCode 枚举版） */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
