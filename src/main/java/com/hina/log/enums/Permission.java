package com.hina.log.enums;

import lombok.Getter;

/**
 * 权限枚举
 */
@Getter
public enum Permission {
    // 用户管理权限
    USER_MANAGE("user:manage", "用户管理"),
    USER_CREATE("user:create", "创建用户"),
    USER_UPDATE("user:update", "更新用户"),
    USER_DELETE("user:delete", "删除用户"),
    USER_VIEW("user:view", "查看用户"),

    // 数据源管理权限
    DATASOURCE_MANAGE("datasource:manage", "数据源管理"),
    DATASOURCE_CREATE("datasource:create", "创建数据源"),
    DATASOURCE_UPDATE("datasource:update", "更新数据源"),
    DATASOURCE_DELETE("datasource:delete", "删除数据源"),
    DATASOURCE_VIEW("datasource:view", "查看数据源"),

    // SQL执行权限
    SQL_EXECUTE("sql:execute", "执行SQL"),

    // 日志检索权限
    LOG_SEARCH("log:search", "日志检索"),

    // 权限管理权限
    PERMISSION_MANAGE("permission:manage", "权限管理");

    private final String value;
    private final String description;

    Permission(String value, String description) {
        this.value = value;
        this.description = description;
    }
}