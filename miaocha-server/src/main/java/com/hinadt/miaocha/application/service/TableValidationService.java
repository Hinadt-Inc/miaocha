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

    /**
     * 验证表结构，检查是否包含必需的message字段 用于logstash管理模块的建表校验
     *
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @throws BusinessException 如果表不存在或缺少message字段
     */
    void validateTableStructure(Long datasourceId, String tableName);
}
