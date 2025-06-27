package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.common.exception.QueryFieldNotExistsException;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import java.util.List;

/** SQL验证和处理服务接口 负责SQL语句的验证、类型检测、表名提取、LIMIT处理等功能 */
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
     * 检查是否为SELECT语句
     *
     * @param sql SQL语句
     * @return 是否为SELECT语句
     */
    boolean isSelectStatement(String sql);

    /**
     * 处理SQL语句的LIMIT限制 - 只对SELECT语句进行LIMIT处理 - 如果没有LIMIT，添加默认限制 - 如果有LIMIT，验证是否超过最大值
     *
     * @param sql 原始SQL语句
     * @return 处理后的SQL语句
     */
    String processSqlWithLimit(String sql);

    /**
     * 从SQL语句中提取表名（支持各种SQL语句类型）
     *
     * @param sql SQL语句
     * @return 表名集合
     */
    java.util.Set<String> extractTableNames(String sql);

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
