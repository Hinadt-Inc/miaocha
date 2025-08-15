package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.CreateTableSqlParser;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.SqlQueryUtils;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.QueryFieldNotExistsException;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** SQL验证和处理服务实现类 负责SQL语句的验证、类型检测、表名提取、LIMIT处理等功能 */
@Service
public class TableValidationServiceImpl implements TableValidationService {
    private static final Logger logger = LoggerFactory.getLogger(TableValidationServiceImpl.class);

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private JdbcQueryExecutor jdbcQueryExecutor;

    @Autowired private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Override
    public void validateDorisSql(ModuleInfo moduleInfo, String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }

        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块信息不能为空");
        }

        String configuredTableName = moduleInfo.getTableName();
        if (!StringUtils.hasText(configuredTableName)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块的表名配置不能为空");
        }

        // 1. 检查是否为CREATE TABLE语句
        if (!CreateTableSqlParser.isCreateTable(sql)) {
            logger.warn("SQL不是CREATE TABLE语句: {}", sql);
            throw new BusinessException(
                    ErrorCode.SQL_NOT_CREATE_TABLE, "只允许执行CREATE TABLE语句，当前SQL类型不符合要求");
        }

        // 2. 提取SQL中的表名并验证是否与模块配置一致
        String extractedTableName = CreateTableSqlParser.extractTableName(sql);
        if (!configuredTableName.equals(extractedTableName)) {
            logger.warn("SQL中的表名[{}]与模块配置的表名[{}]不一致", extractedTableName, configuredTableName);
            throw new BusinessException(
                    ErrorCode.SQL_TABLE_NAME_MISMATCH,
                    String.format(
                            "SQL中的表名'%s'与模块配置的表名'%s'不一致", extractedTableName, configuredTableName));
        }

        logger.info("Doris SQL校验通过: 模块名={}, 表名={}", moduleInfo.getName(), configuredTableName);
    }

    // 表名解析逻辑已迁移至 CreateTableSqlParser

    @Override
    public boolean isTableExists(ModuleInfo moduleInfo) {
        if (moduleInfo == null) {
            return false;
        }

        // 检查数据库中是否存在表
        if (!StringUtils.hasText(moduleInfo.getTableName())) {
            logger.debug("模块 {} 未配置表名", moduleInfo.getName());
            return false;
        }

        try {
            DatasourceInfo datasourceInfo =
                    datasourceMapper.selectById(moduleInfo.getDatasourceId());
            if (datasourceInfo == null) {
                logger.warn("模块 {} 对应的数据源不存在", moduleInfo.getName());
                return false;
            }

            try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
                DatabaseMetadataService metadataService =
                        metadataServiceFactory.getService(datasourceInfo.getType());

                List<String> allTables = metadataService.getAllTables(conn);
                boolean tableExists = allTables.contains(moduleInfo.getTableName());

                logger.debug(
                        "模块 {} 的表 {} 在数据库中{}存在",
                        moduleInfo.getName(),
                        moduleInfo.getTableName(),
                        tableExists ? "" : "不");
                return tableExists;
            }
        } catch (Exception e) {
            logger.error("检查模块 {} 的表是否存在时发生错误: {}", moduleInfo.getName(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> parseFieldNamesFromCreateTableSql(String sql) {
        List<String> fieldNames = new ArrayList<>();

        if (!StringUtils.hasText(sql)) {
            return fieldNames;
        }

        try {
            List<String> parsed = CreateTableSqlParser.extractFieldNames(sql);
            if (parsed.isEmpty()) {
                logger.warn("无法从SQL中提取字段定义部分: {}", sql);
                return fieldNames;
            }
            fieldNames = parsed;

            logger.debug("从建表SQL中解析出 {} 个字段: {}", fieldNames.size(), fieldNames);
            return fieldNames;

        } catch (Exception e) {
            logger.error("解析建表SQL字段名时发生错误: {}", e.getMessage());
            return fieldNames;
        }
    }

    @Override
    public List<String> getTableFieldNames(Long moduleId) {
        if (moduleId == null) {
            return new ArrayList<>();
        }

        ModuleInfo moduleInfo = moduleInfoMapper.selectById(moduleId);
        if (moduleInfo == null) {
            logger.warn("模块不存在: {}", moduleId);
            return new ArrayList<>();
        }

        // 优先从建表SQL解析字段名
        if (StringUtils.hasText(moduleInfo.getDorisSql())) {
            List<String> fieldsFromSql =
                    parseFieldNamesFromCreateTableSql(moduleInfo.getDorisSql());
            if (!fieldsFromSql.isEmpty()) {
                return fieldsFromSql;
            }
        }

        // 如果SQL解析失败或没有SQL，从数据库元数据获取
        if (StringUtils.hasText(moduleInfo.getTableName())) {
            try {
                DatasourceInfo datasourceInfo =
                        datasourceMapper.selectById(moduleInfo.getDatasourceId());
                if (datasourceInfo != null) {
                    try (Connection conn = jdbcQueryExecutor.getConnection(datasourceInfo)) {
                        DatabaseMetadataService metadataService =
                                metadataServiceFactory.getService(datasourceInfo.getType());

                        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                                metadataService.getColumnInfo(conn, moduleInfo.getTableName());

                        return columns.stream()
                                .map(SchemaInfoDTO.ColumnInfoDTO::getColumnName)
                                .collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                logger.error("从数据库获取模块 {} 字段信息时发生错误: {}", moduleId, e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    @Override
    public void validateQueryConfigFields(ModuleInfo moduleInfo, List<String> configuredFields) {
        if (moduleInfo == null || configuredFields == null || configuredFields.isEmpty()) {
            return;
        }

        // 如果没有建表SQL，跳过验证
        if (!StringUtils.hasText(moduleInfo.getDorisSql())) {
            logger.debug("模块 {} 没有建表SQL，跳过字段验证", moduleInfo.getName());
            return;
        }

        // 获取表的所有字段名
        List<String> tableFields = getTableFieldNames(moduleInfo.getId());
        if (tableFields.isEmpty()) {
            logger.warn("无法获取模块 {} 的字段列表，跳过验证", moduleInfo.getName());
            return;
        }

        // 查找不存在的字段
        List<String> nonExistentFields =
                configuredFields.stream()
                        .filter(field -> !tableFields.contains(field))
                        .collect(Collectors.toList());

        if (!nonExistentFields.isEmpty()) {
            throw new QueryFieldNotExistsException(
                    moduleInfo.getName(), moduleInfo.getTableName(), nonExistentFields);
        }

        logger.debug("模块 {} 的查询配置字段验证通过", moduleInfo.getName());
    }

    // ==================== 新增的 SQL 处理方法 ====================

    @Override
    public boolean isSelectStatement(String sql) {
        return SqlQueryUtils.isSelectStatement(sql);
    }

    @Override
    public String processSqlWithLimit(String sql) {
        return SqlQueryUtils.processSqlWithLimit(sql);
    }

    @Override
    public java.util.Set<String> extractTableNames(String sql) {
        return SqlQueryUtils.extractTableNames(sql);
    }
}
