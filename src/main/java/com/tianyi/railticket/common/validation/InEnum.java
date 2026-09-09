package com.tianyi.railticket.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验数字参数必须是指定枚举的合法 code。
 *
 * <pre>
 * &#064;NotNull(message = "席别不能为空")
 * &#064;InEnum(enumClass = SeatType.class, message = "席别不存在")
 * private Integer seatType;
 * </pre>
 *
 * <p><b>为什么叫 InEnum 而不是 EnumValue：</b>避免和 MyBatis-Plus 的
 * {@code com.baomidou.mybatisplus.annotation.EnumValue} 撞名，两个注解语义完全不同。
 *
 * <p><b>空值不拦：</b>{@code null} 交给 {@code @NotNull} 处理，
 * 否则同一个字段会一次性报两条错，前端拿到的是第一条，等于白报。
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InEnumValidator.class)
public @interface InEnum {

    String message() default "取值不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 目标枚举，必须实现 {@link CodeEnum} */
    Class<? extends CodeEnum> enumClass();
}
