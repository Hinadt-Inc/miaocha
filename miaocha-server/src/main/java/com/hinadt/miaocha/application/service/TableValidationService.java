package com.hinadt.miaocha.application.service;

/** 表验证服务接口 用于验证数据源中表是否存在 */
public interface TableValidationService {

    /**
     * 检查指定数据源中是否存在指定的表
     *
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 表是否存在
     */
    boolean isTableExists(Long datasourceId, String tableName);
}
