package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.common.exception.QueryFieldNotExistsException;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import java.util.List;

/** 表验证服务接口 用于验证SQL语句和表相关操作 */
public interface TableValidationService {

    /**
     * 验证Doris SQL语句 校验规则： 1. 只能执行CREATE TABLE语句 2. 表名必须与模块配置的表名一致
     *
     * @param moduleInfo 模块信息
     * @param sql SQL语句
     * @throws BusinessException 如果SQL不符合要求
     */
    void validateDorisSql(ModuleInfo moduleInfo, String sql);

    /**
     * 检查表在数据库中是否存在
     *
     * @param moduleInfo 模块信息
     * @return true如果表在数据库中存在，false否则
     */
    boolean isTableExists(ModuleInfo moduleInfo);

    /**
     * 从CREATE TABLE SQL语句中解析出字段名列表
     *
     * @param sql CREATE TABLE SQL语句
     * @return 字段名列表，如果解析失败则返回空列表
     */
    List<String> parseFieldNamesFromCreateTableSql(String sql);

    /**
     * 根据模块ID获取表字段名列表（用于查询配置时的字段提示）
     *
     * @param moduleId 模块ID
     * @return 字段名列表，如果模块不存在或无建表SQL则返回空列表
     */
    List<String> getTableFieldNames(Long moduleId);

    /**
     * 验证查询配置中的字段是否在表字段列表中
     *
     * @param moduleInfo 模块信息
     * @param configuredFields 配置的查询字段名列表
     * @throws QueryFieldNotExistsException 如果有字段不在表字段列表中
     */
    void validateQueryConfigFields(ModuleInfo moduleInfo, List<String> configuredFields);
}
