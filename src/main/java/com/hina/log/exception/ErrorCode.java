package com.hina.log.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    // 通用错误码
    SUCCESS("0000", "操作成功"),
    INTERNAL_ERROR("9999", "服务器内部错误"),
    VALIDATION_ERROR("1000", "参数校验失败"),

    // 数据源相关错误码
    DATASOURCE_NOT_FOUND("2001", "数据源不存在"),
    DATASOURCE_NAME_EXISTS("2002", "数据源名称已存在"),
    DATASOURCE_CONNECTION_FAILED("2003", "数据源连接失败"),
    DATASOURCE_TYPE_NOT_SUPPORTED("2004", "不支持的数据源类型"),

    // 用户相关错误码
    USER_NOT_FOUND("3001", "用户不存在"),
    USER_NAME_EXISTS("3002", "用户名已存在"),
    USER_PASSWORD_ERROR("3003", "用户名或密码错误"),

    // 权限相关错误码
    PERMISSION_DENIED("4001", "权限不足"),

    // 导出相关错误码
    EXPORT_FAILED("5001", "导出失败");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}