package com.hinadt.miaocha.domain.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 排序字段验证注解 用于验证排序字段名格式，禁止 VARIANT 类型字段子字段语法 只允许普通字段名，不允许带 . 和 [] 的复杂字段引用 */
@Documented
@Constraint(validatedBy = SortFieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSortField {

    String message() default "排序字段名格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
