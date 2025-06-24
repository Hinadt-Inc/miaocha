package com.hinadt.miaocha.common.exception;

import java.util.List;

/** 查询字段不存在异常 当配置的查询字段不在表结构中时抛出此异常 */
public class QueryFieldNotExistsException extends RuntimeException {

    private final List<String> nonExistentFields;
    private final String moduleName;
    private final String tableName;

    public QueryFieldNotExistsException(
            String moduleName, String tableName, List<String> nonExistentFields) {
        super(buildMessage(moduleName, tableName, nonExistentFields));
        this.moduleName = moduleName;
        this.tableName = tableName;
        this.nonExistentFields = nonExistentFields;
    }

    private static String buildMessage(
            String moduleName, String tableName, List<String> nonExistentFields) {
        return String.format(
                "模块 '%s' 的表 '%s' 中不存在以下字段: %s",
                moduleName, tableName, String.join(", ", nonExistentFields));
    }

    public List<String> getNonExistentFields() {
        return nonExistentFields;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getTableName() {
        return tableName;
    }
}
