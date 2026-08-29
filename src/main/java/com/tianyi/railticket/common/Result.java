package com.tianyi.railticket.common;

import lombok.Getter;

@Getter
public class Result<T> {
    // 默认SUCCESS给前端返回0，其余数字都是失败
    public static final int SUCCESS = 0;

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功 + 数据
    public static <T> Result<T> ok(T data){
        return new Result<>(SUCCESS, "success", data);
    }
    // 成功无数据
    public static Result<Void> ok(){
        return new Result<>(SUCCESS, "success", null);
    }
    // 失败
    public static <T> Result<T> fail(int code, String message){
       return new Result<>(code, message, null);
    }
}
