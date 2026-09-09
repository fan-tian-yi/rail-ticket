package com.tianyi.railticket.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link InEnum} 的校验逻辑：入参 code 必须命中枚举里的某一项。
 */
public class InEnumValidator implements ConstraintValidator<InEnum, Integer> {

    private Class<? extends CodeEnum> enumClass;

    @Override
    public void initialize(InEnum annotation) {
        this.enumClass = annotation.enumClass();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;                    // 空值由 @NotNull 负责，这里放行避免重复报错
        }
        for (CodeEnum item : enumClass.getEnumConstants()) {
            if (value.equals(item.getCode())) {
                return true;
            }
        }
        return false;
    }
}
