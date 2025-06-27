package com.hinadt.miaocha.domain.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/** 时间字段验证器 用于验证时间字段名格式，并提供不同的错误消息 */
public class TimeFieldValidator implements ConstraintValidator<ValidTimeField, String> {

    // 匹配 VARIANT 类型字段子字段语法：a.b 或 a['b'] 或 a["b"]
    private static final Pattern VARIANT_FIELD_PATTERN = Pattern.compile(".*[.\\['\"].*");

    // 匹配合法的字段名格式
    private static final Pattern VALID_FIELD_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Override
    public void initialize(ValidTimeField constraintAnnotation) {
        // 初始化方法，这里不需要特殊处理
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // null 或空值由其他验证器处理
        }

        // 禁用默认错误消息
        context.disableDefaultConstraintViolation();

        // 检查是否是 VARIANT 类型字段子字段语法
        if (VARIANT_FIELD_PATTERN.matcher(value).matches()) {
            context.buildConstraintViolationWithTemplate("不支持 VARIANT 类型字段子字段作为时间字段")
                    .addConstraintViolation();
            return false;
        }

        // 检查是否是合法的字段名格式
        if (!VALID_FIELD_PATTERN.matcher(value).matches()) {
            context.buildConstraintViolationWithTemplate("时间字段名格式不正确").addConstraintViolation();
            return false;
        }

        return true;
    }
}
