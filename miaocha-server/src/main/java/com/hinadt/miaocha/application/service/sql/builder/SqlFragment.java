package com.hinadt.miaocha.application.service.sql.builder;

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
                "SELECT %s AS %s, COUNT(1) AS %s", timeExpression, TIME_ALIAS, COUNT_ALIAS);
    }

    /** 构建FROM子句 */
    public static String from(String tableName) {
        return " FROM " + tableName;
    }

    /** 构建ORDER BY子句 */
    public static String orderBy(String field, String direction) {
        return String.format(" ORDER BY %s %s", field, direction);
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
        switch (timeUnit) {
            case "second":
                return intervalValue;
            case "minute":
                return intervalValue * 60;
            case "hour":
                return intervalValue * 3600;
            case "day":
                return intervalValue * 86400;
            default:
                throw new IllegalArgumentException("不支持的时间单位: " + timeUnit);
        }
    }

    /** 构建统计查询的SELECT子句 */
    public static String selectCount() {
        return "SELECT COUNT(1) AS " + TOTAL_ALIAS;
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
}
