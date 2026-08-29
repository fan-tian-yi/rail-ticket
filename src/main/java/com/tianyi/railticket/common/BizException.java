package com.tianyi.railticket.common;


import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);          // message 给父类（日志/getMessage 用）
        this.code = code;
    }
}

