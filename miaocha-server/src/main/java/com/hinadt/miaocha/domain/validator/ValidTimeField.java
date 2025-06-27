package com.hinadt.miaocha.domain.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 时间字段验证注解 用于验证时间字段名格式，禁止 VARIANT 类型字段子字段语法 */
@Documented
@Constraint(validatedBy = TimeFieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeField {

    String message() default "时间字段名格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
