package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.sql.builder.condition.SearchConditionManager;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** SQL语句构建器 */
@Component
public class LogSqlBuilder {

    /** 字段分布统计的采样大小常量 */
    public static final int FIELD_DISTRIBUTION_SAMPLE_SIZE = 5000;

    private final SearchConditionManager searchConditionManager;

    @Autowired
    public LogSqlBuilder(SearchConditionManager searchConditionManager) {
        this.searchConditionManager = searchConditionManager;
    }

    /** 构建日志分布统计SQL - 支持自定义间隔分组 */
    public String buildDistributionSql(LogSearchDTO dto, String tableName, String timeUnit) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT date_trunc(log_time, '")
                .append(timeUnit)
                .append("') AS log_time_, ")
                .append(" COUNT(1) AS count ")
                .append("FROM ")
                .append(tableName)
                .append(" WHERE log_time >= '")
                .append(dto.getStartTime())
                .append("'")
                .append(" AND log_time < '")
                .append(dto.getEndTime())
                .append("'");

        appendSearchConditions(sql, dto);

        sql.append(" GROUP BY date_trunc(log_time, '")
                .append(timeUnit)
                .append("')")
                .append(" ORDER BY log_time_ ASC");

        return sql.toString();
    }

    /** 构建支持自定义间隔的日志分布统计SQL */
    public String buildDistributionSqlWithInterval(
            LogSearchDTO dto, String tableName, String timeUnit, int intervalValue) {
        StringBuilder sql = new StringBuilder();

        if (intervalValue == 1) {
            // 间隔为1时，使用原有的date_trunc逻辑
            return buildDistributionSql(dto, tableName, timeUnit);
        }

        // 间隔大于1时，使用FLOOR函数实现自定义间隔分组
        String bucketExpression = generateBucketExpression(timeUnit, intervalValue);

        sql.append("SELECT ")
                .append(bucketExpression)
                .append(" AS log_time_, ")
                .append(" COUNT(1) AS count ")
                .append("FROM ")
                .append(tableName)
                .append(" WHERE log_time >= '")
                .append(dto.getStartTime())
                .append("'")
                .append(" AND log_time < '")
                .append(dto.getEndTime())
                .append("'");

        appendSearchConditions(sql, dto);

        sql.append(" GROUP BY ").append(bucketExpression).append(" ORDER BY log_time_ ASC");

        return sql.toString();
    }

    /** 生成桶分组表达式，支持自定义间隔 */
    private String generateBucketExpression(String timeUnit, int intervalValue) {
        if ("millisecond".equals(timeUnit)) {
            // 毫秒级间隔：使用DATE_FORMAT获取带毫秒的时间格式
            double intervalSeconds = intervalValue / 1000.0;
            return String.format(
                    "DATE_FORMAT(FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / %.3f) * %.3f),"
                            + " '%%Y-%%m-%%d %%H:%%i:%%s.%%f')",
                    intervalSeconds, intervalSeconds);
        }

        String intervalSeconds;
        switch (timeUnit) {
            case "second":
                intervalSeconds = String.valueOf(intervalValue);
                break;
            case "minute":
                intervalSeconds = String.valueOf(intervalValue * 60);
                break;
            case "hour":
                intervalSeconds = String.valueOf(intervalValue * 3600);
                break;
            case "day":
                intervalSeconds = String.valueOf(intervalValue * 86400);
                break;
            default:
                throw new IllegalArgumentException("不支持的时间单位: " + timeUnit);
        }

        // 使用FLOOR函数实现自定义间隔分组
        // 原理：将时间转为秒数，除以间隔秒数取整，再乘以间隔秒数，最后转回时间
        return String.format(
                "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / %s) * %s)",
                intervalSeconds, intervalSeconds);
    }

    /** 构建详细日志查询SQL */
    public String buildDetailSql(LogSearchDTO dto, String tableName) {
        StringBuilder sql = new StringBuilder();

        if (dto.getFields() != null && !dto.getFields().isEmpty()) {
            sql.append("SELECT ").append(String.join(", ", dto.getFields()));
        } else {
            sql.append("SELECT *");
        }

        sql.append(" FROM ")
                .append(tableName)
                .append(" WHERE log_time >= '")
                .append(dto.getStartTime())
                .append("'")
                .append(" AND log_time < '")
                .append(dto.getEndTime())
                .append("'");

        appendSearchConditions(sql, dto);

        sql.append(" ORDER BY log_time DESC")
                .append(" LIMIT ")
                .append(dto.getPageSize())
                .append(" OFFSET ")
                .append(dto.getOffset());

        return sql.toString();
    }

    /**
     * 构建字段分布TOP N查询SQL，使用两层查询优化性能 内层：先筛选出前5000条数据 外层：对这5000条数据使用Doris的TOPN函数进行统计
     *
     * @param dto 日志搜索DTO
     * @param tableName 表名
     * @param fields 需要统计的字段列表（已转换为括号语法，用于TOPN函数）
     * @param originalFields 原始字段列表（点语法，用于AS别名）
     * @param topN 每个字段取前N个值
     * @return 字段分布查询SQL
     */
    public String buildFieldDistributionSql(
            LogSearchDTO dto,
            String tableName,
            List<String> fields,
            List<String> originalFields,
            int topN) {
        StringBuilder sql = new StringBuilder();

        // 构建TOPN函数调用列表，字段使用括号语法，AS别名使用原始点语法
        StringBuilder topnColumns = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                topnColumns.append(", ");
            }
            String field = fields.get(i);
            String originalField = i < originalFields.size() ? originalFields.get(i) : field;
            topnColumns.append(String.format("TOPN(%s, %d) AS '%s'", field, topN, originalField));
        }

        // 构建两层查询：外层TOPN，内层限制5000条数据
        sql.append("SELECT ")
                .append(topnColumns.toString())
                .append(" FROM (")
                .append("SELECT * FROM ")
                .append(tableName)
                .append(" WHERE log_time >= '")
                .append(dto.getStartTime())
                .append("'")
                .append(" AND log_time < '")
                .append(dto.getEndTime())
                .append("'");

        appendSearchConditions(sql, dto);

        // 内层查询：按时间倒序排序，限制采样数据
        sql.append(" ORDER BY log_time DESC")
                .append(" LIMIT ")
                .append(FIELD_DISTRIBUTION_SAMPLE_SIZE)
                .append(") AS sub_query");

        return sql.toString();
    }

    /**
     * 仅构建搜索条件字符串
     *
     * @param dto 日志搜索DTO
     * @return 搜索条件字符串（不包含前置AND）
     */
    public String buildSearchConditionsOnly(LogSearchDTO dto) {
        return searchConditionManager.buildSearchConditions(dto);
    }

    /** 添加搜索条件 */
    private void appendSearchConditions(StringBuilder sql, LogSearchDTO dto) {
        String conditions = searchConditionManager.buildSearchConditions(dto);
        if (StringUtils.isNotBlank(conditions)) {
            sql.append(" AND ").append(conditions);
        }
    }
}
