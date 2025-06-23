package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 表验证服务实现类 用于验证SQL语句和表相关操作 */
@Service
public class TableValidationServiceImpl implements TableValidationService {
    private static final Logger logger = LoggerFactory.getLogger(TableValidationServiceImpl.class);

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
}
