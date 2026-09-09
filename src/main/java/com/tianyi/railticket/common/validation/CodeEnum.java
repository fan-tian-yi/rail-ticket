package com.tianyi.railticket.common.validation;

/**
 * 带 {@code code} 字段的枚举需要实现的标记接口。
 *
 * <p>实现它之后，就可以在 DTO 字段上用 {@link InEnum} 声明式校验取值，
 * 不必在 Service 里手写 {@code if (Xxx.of(code) == null) throw ...}。
 */
public interface CodeEnum {

    /** 存库 / 传输用的数字码 */
    Integer getCode();
}
