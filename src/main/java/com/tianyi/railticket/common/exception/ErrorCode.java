package com.tianyi.railticket.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== 通用 4xxxx =====
    PARAM_ERROR(40000, "参数错误"),

    // ===== 库存 / 车次 4xxxx =====
    TRAIN_NOT_PASS_STATION(40001, "该车次不经停所选车站"),
    INVALID_DIRECTION(40002, "出发站必须在到达站之前"),
    NO_TICKET(40003, "余票不足"),
    INVALID_DATE(40004, "乘车日期不能早于今天"),
    PASSENGER_NOT_MATCH(40005, "乘客不存在或不属于当前用户"),
    TRAIN_NOT_FOUND(40006, "车次不存在或已停运"),
    SEAT_TYPE_INVALID(40007, "席别不存在"),
    BEYOND_PRESALE(40008, "超出预售期"),

    // ===== 订单 4xxxx =====
    ORDER_NOT_FOUND(40009, "订单不存在"),
    ORDER_STATUS_INVALID(40010, "订单状态不允许该操作"),

    // ===== 服务端 5xxxx =====
    SYSTEM_ERROR(50001, "系统繁忙，请稍后重试"),
    ;

    private final int code;
    private final String message;
}
