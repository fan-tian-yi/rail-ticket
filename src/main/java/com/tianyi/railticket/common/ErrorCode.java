package com.tianyi.railticket.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码。
 *规则（本项目约定，非业界标准）：
 *  {@code Result.code = 0} 表示成功，非 0 均为失败
 *  4xxxx：调用方传参有误，改请求即可成功（对应 HTTP 语义的客户端错误）
 *  5xxxx：服务端自身问题（依赖不可用、数据异常等）
 *
 * HTTP 状态码仍固定返回 200，业务成败只看 {@code Result.code}。
 * 新增错误码必须加在这里，禁止在 Service 里硬编码数字。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== 库存 / 车次 4xxxx =====
    TRAIN_NOT_PASS_STATION(40001, "该车次不经停所选车站"),
    INVALID_DIRECTION(40002, "出发站必须在到达站之前"),
    NO_TICKET(40003, "余票不足"),

    ;

    private final int code;
    private final String message;
}
