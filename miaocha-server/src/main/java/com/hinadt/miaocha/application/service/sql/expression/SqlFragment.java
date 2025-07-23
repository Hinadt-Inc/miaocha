package com.hinadt.miaocha.application.service.sql.expression;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * SQL片段工具类
 *
 * <p>提供常用的SQL片段构建方法，避免重复代码
 */
public class SqlFragment {

    /** 时间字段别名常量 */
    public static final String TIME_ALIAS = "log_time_";

    /** 统计字段别名常量 */
    public static final String COUNT_ALIAS = "count";

    /** 总数字段别名常量 */
    public static final String TOTAL_ALIAS = "total";

    /** 构建时间范围条件 */
    public static String timeRange(String timeField, LogSearchDTO dto) {
        return String.format(
                "%s >= '%s' AND %s < '%s'",
                timeField, dto.getStartTime(), timeField, dto.getEndTime());
    }

    /** 构建SELECT字段列表 */
    public static String selectFields(List<String> fields) {
        if (fields != null && !fields.isEmpty()) {
            return "SELECT " + String.join(", ", fields);
        }
        return "SELECT *";
    }

    /** 构建时间分布查询的SELECT子句 */
    public static String selectTimeDistribution(String timeExpression) {
        return String.format(
                "SELECT %s AS %s, COUNT(*) AS %s", timeExpression, TIME_ALIAS, COUNT_ALIAS);
    }

    /** 构建FROM子句 */
    public static String from(String tableName) {
        return " FROM " + tableName;
    }

    /** 构建ORDER BY子句 */
    public static String orderBy(String field, String direction) {
        return String.format(" ORDER BY %s %s", field, direction);
    }

    /** 构建多字段ORDER BY子句 */
    public static String orderByMultiple(List<OrderField> orderFields) {
        if (orderFields == null || orderFields.isEmpty()) {
            return "";
        }

        List<String> orderClauses = new ArrayList<>();
        for (OrderField orderField : orderFields) {
            orderClauses.add(
                    String.format("%s %s", orderField.fieldName(), orderField.direction()));
        }

        return " ORDER BY " + String.join(", ", orderClauses);
    }

    /** 构建LIMIT子句 */
    public static String limit(int pageSize, int offset) {
        return String.format(" LIMIT %d OFFSET %d", pageSize, offset);
    }

    /** 构建GROUP BY子句 */
    public static String groupBy(String expression) {
        return " GROUP BY " + expression;
    }

    /** 构建自定义间隔分组表达式 */
    public static String customTimeBucket(String timeField, String timeUnit, int intervalValue) {
        if ("millisecond".equals(timeUnit)) {
            return String.format(
                    "FLOOR(MILLISECOND_TIMESTAMP(%s) / %d) * %d",
                    timeField, intervalValue, intervalValue);
        }

        int intervalSeconds = getIntervalInSeconds(timeUnit, intervalValue);
        return String.format(
                "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(%s) / %d) * %d)",
                timeField, intervalSeconds, intervalSeconds);
    }

    /** 获取时间间隔的秒数 - 包访问权限便于测试 */
    static int getIntervalInSeconds(String timeUnit, int intervalValue) {
        return switch (timeUnit) {
            case "second" -> intervalValue;
            case "minute" -> intervalValue * 60;
            case "hour" -> intervalValue * 3600;
            case "day" -> intervalValue * 86400;
            default -> throw new IllegalArgumentException("不支持的时间单位: " + timeUnit);
        };
    }

    /** 构建统计查询的SELECT子句 */
    public static String selectCount() {
        return "SELECT COUNT(*) AS " + TOTAL_ALIAS;
    }

    /** 构建子查询的SELECT子句 */
    public static String selectWithSubquery(String columns, String subQuery, String alias) {
        return String.format("SELECT %s FROM (%s) AS %s", columns, subQuery, alias);
    }

    /**
     * 智能构建WHERE条件 - 解决AND重复问题
     *
     * @param timeCondition 时间条件（必须）
     * @param keywordConditions 关键字条件
     * @param whereConditions 用户WHERE条件
     * @return 完整的WHERE子句
     */
    public static String buildWhereClause(
            String timeCondition, String keywordConditions, String whereConditions) {
        List<String> conditions = new ArrayList<>();

        // 添加时间条件（必须存在）
        if (StringUtils.isNotBlank(timeCondition)) {
            conditions.add(timeCondition);
        }

        // 添加关键字条件
        if (StringUtils.isNotBlank(keywordConditions)) {
            conditions.add(keywordConditions);
        }

        // 添加用户WHERE条件
        if (StringUtils.isNotBlank(whereConditions)) {
            conditions.add(whereConditions);
        }

        if (conditions.isEmpty()) {
            return "";
        }

        return " WHERE " + String.join(" AND ", conditions);
    }

    /** 排序字段封装类 */
    public record OrderField(String fieldName, String direction) {}

    // ==================== 统一括号规范 ====================

    /** 统一的字段条件格式化 规范：(fieldName OPERATOR 'value') 避免双重括号：如果已经有括号就不再加括号 */
    public static String formatFieldCondition(String condition) {
        if (StringUtils.isBlank(condition)) {
            return "";
        }

        // 如果条件已经有完整的括号包围，就不再添加
        String trimmed = condition.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return trimmed;
        }

        return "(" + condition + ")";
    }

    /** 统一的多字段OR连接格式化 规范：((field1 condition) OR (field2 condition)) 或 (field condition) 当只有一个字段时 */
    public static String formatMultiFieldOr(List<String> fieldConditions) {
        if (fieldConditions == null || fieldConditions.isEmpty()) {
            return "";
        }

        List<String> validConditions =
                fieldConditions.stream().filter(StringUtils::isNotBlank).toList();

        if (validConditions.isEmpty()) {
            return "";
        }

        if (validConditions.size() == 1) {
            // 单字段时只加一层括号
            return formatFieldCondition(validConditions.get(0));
        }

        // 多字段时，每个字段加括号，然后整体再加括号
        List<String> formattedConditions =
                validConditions.stream().map(SqlFragment::formatFieldCondition).toList();

        return """
            (%s)
            """
                .formatted(String.join(" OR ", formattedConditions))
                .trim();
    }

    /** 统一的多关键字AND连接格式化 规范：((keyword1 condition) AND (keyword2 condition)) */
    public static String formatMultiKeywordAnd(List<String> keywordConditions) {
        if (keywordConditions == null || keywordConditions.isEmpty()) {
            return "";
        }

        List<String> validConditions =
                keywordConditions.stream().filter(StringUtils::isNotBlank).toList();

        if (validConditions.isEmpty()) {
            return "";
        }

        if (validConditions.size() == 1) {
            return validConditions.get(0);
        }

        return """
            (%s)
            """
                .formatted(String.join(" AND ", validConditions))
                .trim();
    }

    /** 统一的NOT条件格式化 规范：NOT (condition1 OR condition2 OR ...) */
    public static String formatNotCondition(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        List<String> validConditions = conditions.stream().filter(StringUtils::isNotBlank).toList();

        if (validConditions.isEmpty()) {
            return "";
        }

        String combinedCondition =
                validConditions.size() == 1
                        ? validConditions.get(0)
                        : String.join(" OR ", validConditions);

        return """
            NOT (%s)
            """.formatted(combinedCondition).trim();
    }
}
