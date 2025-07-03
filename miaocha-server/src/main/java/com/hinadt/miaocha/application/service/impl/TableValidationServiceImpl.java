package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    // ==================== CREATE TABLE 相关常量 ====================

    /** CREATE TABLE 语句的正则表达式 */
    private static final Pattern CREATE_TABLE_PATTERN =
            Pattern.compile(
                    "^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`?([\\w\\u4e00-\\u9fff\\-\\.@#$\\s]+)`?\\.)?`?([\\w\\u4e00-\\u9fff\\-\\.@#$\\s]+)`?\\s*\\(",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** CREATE TABLE 语句匹配的正则表达式（用于验证是否为CREATE TABLE语句） */
    private static final Pattern CREATE_TABLE_CHECK_PATTERN =
            Pattern.compile(
                    "^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 字段定义的正则表达式 - 匹配字段名、类型等 */
    private static final Pattern FIELD_DEFINITION_PATTERN =
            Pattern.compile(
                    "\\s*(?:`([^`]+)`|([a-zA-Z_][a-zA-Z0-9_]*))\\s+([a-zA-Z0-9_()\\s,]+?)(?:,|\\s*\\)|$)",
                    Pattern.CASE_INSENSITIVE);

    // ==================== SQL 语句处理相关常量 ====================

    /** SELECT 语句检测正则 */
    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);

    /** LIMIT 子句检测正则 */
    private static final Pattern LIMIT_PATTERN =
            Pattern.compile(
                    "\\blimit\\s+\\d+(?:\\s*,\\s*\\d+)?\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);

    /** 默认查询限制条数 */
    private static final int DEFAULT_QUERY_LIMIT = 1000;

    /** 最大查询限制条数 */
    private static final int MAX_QUERY_LIMIT = 10000;

    /** 表名提取正则 - FROM 子句 */
    private static final Pattern FROM_PATTERN =
            Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?", Pattern.CASE_INSENSITIVE);

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
        if (!CREATE_TABLE_CHECK_PATTERN.matcher(sql).find()) {
            logger.warn("SQL不是CREATE TABLE语句: {}", sql);
            throw new BusinessException(
                    ErrorCode.SQL_NOT_CREATE_TABLE, "只允许执行CREATE TABLE语句，当前SQL类型不符合要求");
        }

        // 2. 提取SQL中的表名并验证是否与模块配置一致
        String extractedTableName = extractTableNameFromCreateSql(sql);
        if (!configuredTableName.equals(extractedTableName)) {
            logger.warn("SQL中的表名[{}]与模块配置的表名[{}]不一致", extractedTableName, configuredTableName);
            throw new BusinessException(
                    ErrorCode.SQL_TABLE_NAME_MISMATCH,
                    String.format(
                            "SQL中的表名'%s'与模块配置的表名'%s'不一致", extractedTableName, configuredTableName));
        }

        logger.info("Doris SQL校验通过: 模块名={}, 表名={}", moduleInfo.getName(), configuredTableName);
    }

    /**
     * 从CREATE TABLE语句中提取表名
     *
     * @param sql CREATE TABLE SQL语句
     * @return 表名
     * @throws BusinessException 如果无法解析表名
     */
    private String extractTableNameFromCreateSql(String sql) {
        // 标准化SQL语句，将多个空白字符替换为单个空格
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();

        // 使用正则表达式匹配CREATE TABLE语句
        Pattern pattern =
                Pattern.compile(
                        "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(.+?)\\s*\\(",
                        Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(normalizedSql);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无法从SQL语句中解析出表名");
        }

        String tableNamePart = matcher.group(1).trim();
        if (tableNamePart.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名不能为空");
        }

        // 检查提取的表名部分是否包含SQL关键字，这可能表示语法错误
        if (tableNamePart.toUpperCase().matches(".*\\b(IF|NOT|EXISTS|EXIST)\\b.*")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误或SQL语法错误");
        }

        return parseTableName(tableNamePart);
    }

    /**
     * 解析表名部分，提取实际的表名
     *
     * @param tableNamePart 表名部分字符串
     * @return 实际的表名
     */
    private String parseTableName(String tableNamePart) {
        // 情况1: 包含点号的格式，可能是数据库.表名 (优先处理这种情况)
        if (tableNamePart.contains(".")) {
            return parseTableNameWithDatabase(tableNamePart);
        }

        // 情况2: 被反引号完全包围的表名，如 `table_name`
        // 这种情况下，反引号内的所有内容都是表名，包括特殊字符
        if (tableNamePart.startsWith("`")
                && tableNamePart.endsWith("`")
                && tableNamePart.length() > 2) {
            return tableNamePart.substring(1, tableNamePart.length() - 1);
        }

        // 情况3: 普通表名
        return tableNamePart;
    }

    /**
     * 解析包含数据库前缀的表名格式 支持以下格式： - database.table - `database`.table - database.`table` -
     * `database`.`table`
     */
    private String parseTableNameWithDatabase(String tableNamePart) {
        // 处理各种可能的格式：
        // 1. database.table
        // 2. `database`.table
        // 3. database.`table`
        // 4. `database`.`table`

        String input = tableNamePart.trim();

        // 找到最后一个不在反引号内的点号
        int lastDotIndex = -1;
        boolean inBackticks = false;

        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            if (c == '`') {
                inBackticks = !inBackticks;
            } else if (c == '.' && !inBackticks) {
                lastDotIndex = i;
                break;
            }
        }

        if (lastDotIndex == -1) {
            // 没有找到点号，说明没有数据库前缀，直接返回去掉反引号的表名
            return input.replaceAll("`", "").trim();
        }

        if (lastDotIndex == input.length() - 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误：表名不能为空");
        }

        if (lastDotIndex == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误：数据库名不能为空");
        }

        // 取点号后面的部分作为表名
        String tablePart = input.substring(lastDotIndex + 1).trim();

        if (tablePart.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名不能为空");
        }

        // 去掉反引号
        return tablePart.replaceAll("`", "").trim();
    }

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
            // 提取字段定义部分
            String fieldsSection = extractFieldsSection(sql);
            if (fieldsSection == null) {
                logger.warn("无法从SQL中提取字段定义部分: {}", sql);
                return fieldNames;
            }

            // 解析字段名
            fieldNames = parseFieldNames(fieldsSection);

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

    /** 提取CREATE TABLE语句中的字段定义部分 */
    private String extractFieldsSection(String sql) {
        // 标准化SQL
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();

        // 查找第一个左括号
        int startIndex = normalizedSql.indexOf('(');
        if (startIndex == -1) {
            return null;
        }

        // 查找匹配的右括号
        int bracketCount = 0;
        int endIndex = -1;

        for (int i = startIndex; i < normalizedSql.length(); i++) {
            char c = normalizedSql.charAt(i);
            if (c == '(') {
                bracketCount++;
            } else if (c == ')') {
                bracketCount--;
                if (bracketCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        if (endIndex == -1) {
            return null;
        }

        return normalizedSql.substring(startIndex + 1, endIndex);
    }

    /** 从字段定义部分解析字段名 */
    private List<String> parseFieldNames(String fieldsSection) {
        List<String> fieldNames = new ArrayList<>();

        // 移除所有索引、约束、外键定义等非字段定义的行
        String cleanedFieldsSection = removeNonFieldDefinitions(fieldsSection);

        // 智能分割字段定义（考虑括号内的逗号）
        List<String> fieldDefinitions = smartSplitFieldDefinitions(cleanedFieldsSection);

        for (String fieldDef : fieldDefinitions) {
            String fieldName = extractFieldName(fieldDef.trim());
            if (fieldName != null && !fieldName.isEmpty()) {
                fieldNames.add(fieldName);
            }
        }

        return fieldNames;
    }

    /** 移除非字段定义（INDEX、PRIMARY KEY、FOREIGN KEY等） */
    private String removeNonFieldDefinitions(String fieldsSection) {
        String[] lines = fieldsSection.split("\\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过各种非字段定义
            if (trimmedLine.isEmpty()
                    || trimmedLine.matches(
                            "(?i)^(PRIMARY\\s+KEY|UNIQUE\\s+KEY|KEY|INDEX|FOREIGN\\s+KEY|CONSTRAINT)\\b.*")
                    || trimmedLine.matches(
                            "(?i)^\\)\\s*(ENGINE|DUPLICATE|AUTO|DISTRIBUTED|PROPERTIES)\\b.*")) {
                continue;
            }

            cleaned.append(line).append("\\n");
        }

        return cleaned.toString();
    }

    /** 智能分割字段定义，考虑括号内的逗号 */
    private List<String> smartSplitFieldDefinitions(String fieldsSection) {
        List<String> definitions = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesisDepth = 0;
        boolean inQuotes = false;
        char lastChar = ' ';

        for (char c : fieldsSection.toCharArray()) {
            if (c == '\'' || c == '"') {
                if (lastChar != '\\') {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes) {
                if (c == '(') {
                    parenthesisDepth++;
                } else if (c == ')') {
                    parenthesisDepth--;
                } else if (c == ',' && parenthesisDepth == 0) {
                    // 只有在括号外且不在引号内的逗号才是字段分隔符
                    String def = current.toString().trim();
                    if (!def.isEmpty()) {
                        definitions.add(def);
                    }
                    current = new StringBuilder();
                    lastChar = c;
                    continue;
                }
            }
            current.append(c);
            lastChar = c;
        }

        // 添加最后一个字段定义
        String def = current.toString().trim();
        if (!def.isEmpty()) {
            definitions.add(def);
        }

        return definitions;
    }

    /** 从单个字段定义中提取字段名 */
    private String extractFieldName(String fieldDefinition) {
        if (fieldDefinition == null || fieldDefinition.trim().isEmpty()) {
            return null;
        }

        String trimmed = fieldDefinition.trim();

        // 跳过非字段定义
        if (trimmed.matches(
                "(?i)^(PRIMARY\\s+KEY|UNIQUE\\s+KEY|KEY|INDEX|FOREIGN\\s+KEY|CONSTRAINT)\\b.*")) {
            return null;
        }

        // 提取字段名（第一个单词，可能被反引号包围）
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 0) {
            return null;
        }

        String fieldName = parts[0];

        // 移除反引号
        if (fieldName.startsWith("`") && fieldName.endsWith("`") && fieldName.length() > 2) {
            fieldName = fieldName.substring(1, fieldName.length() - 1);
        }

        // 验证字段名是否有效（包含字母、数字、下划线，支持数字开头的字段名）
        if (fieldName.matches("^[a-zA-Z0-9_]+$")) {
            return fieldName;
        }

        return null;
    }

    // ==================== 新增的 SQL 处理方法 ====================

    @Override
    public boolean isSelectStatement(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return SELECT_PATTERN.matcher(sql.trim()).find();
    }

    @Override
    public String processSqlWithLimit(String sql) {
        if (sql == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }

        // 只对SELECT语句进行LIMIT处理
        if (!isSelectStatement(sql)) {
            // 非SELECT语句直接返回，不做任何修改
            return sql;
        }

        // 将SQL转为小写，用于检查LIMIT子句
        String sqlLower = sql.trim().toLowerCase();

        // 检查是否已经包含LIMIT子句
        java.util.regex.Matcher limitMatcher = LIMIT_PATTERN.matcher(sqlLower);
        if (limitMatcher.find()) {
            // 提取LIMIT值
            String limitClause = limitMatcher.group(0);
            // 解析LIMIT子句中的数字
            int limitValue = extractLimitValue(limitClause);

            // 检查LIMIT是否超过最大允许值
            if (limitValue > MAX_QUERY_LIMIT) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "查询结果数量限制不能超过" + MAX_QUERY_LIMIT + "条，请调整您的LIMIT语句");
            }

            // LIMIT在合法范围内，直接返回原SQL
            return sql;
        } else {
            // SELECT语句没有LIMIT子句，添加默认LIMIT
            // 检查SQL是否以分号结束，如果是，在分号前添加LIMIT
            if (sqlLower.endsWith(";")) {
                return sql.substring(0, sql.length() - 1) + " LIMIT " + DEFAULT_QUERY_LIMIT + ";";
            } else {
                return sql + " LIMIT " + DEFAULT_QUERY_LIMIT;
            }
        }
    }

    @Override
    public java.util.Set<String> extractTableNames(String sql) {
        java.util.Set<String> tableNames = new java.util.HashSet<>();

        if (sql == null || sql.trim().isEmpty()) {
            return tableNames;
        }

        // 使用简单的FROM子句提取（目前先实现基本功能）
        java.util.regex.Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            String tableName = fromMatcher.group(1);
            if (tableName != null && !tableName.trim().isEmpty()) {
                // 处理 schema.table 格式，只取表名部分
                if (tableName.contains(".")) {
                    String[] parts = tableName.split("\\.");
                    tableName = parts[parts.length - 1]; // 取最后一部分作为表名
                }
                tableNames.add(tableName.trim());
            }
        }

        return tableNames;
    }

    /**
     * 从LIMIT子句中提取限制值 处理如下几种情况: - LIMIT X - LIMIT X, Y (MySQL语法，返回偏移X后的Y条记录)
     *
     * @param limitClause LIMIT子句
     * @return 提取的限制值
     */
    private int extractLimitValue(String limitClause) {
        // 移除LIMIT关键字，只保留数字部分
        String numbers = limitClause.replaceAll("\\blimit\\s+", "").trim();

        // 移除末尾的分号（如果有）
        if (numbers.endsWith(";")) {
            numbers = numbers.substring(0, numbers.length() - 1).trim();
        }

        // 处理LIMIT X, Y格式
        if (numbers.contains(",")) {
            String[] parts = numbers.split(",");
            // 返回第二个数字，即实际的限制条数
            return Integer.parseInt(parts[1].trim());
        } else {
            // 处理LIMIT X格式
            return Integer.parseInt(numbers.trim());
        }
    }
}
