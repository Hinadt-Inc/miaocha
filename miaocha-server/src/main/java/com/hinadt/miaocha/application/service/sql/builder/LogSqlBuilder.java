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

    private final SearchConditionManager searchConditionManager;

    @Autowired
    public LogSqlBuilder(SearchConditionManager searchConditionManager) {
        this.searchConditionManager = searchConditionManager;
    }

    /** 构建日志分布统计SQL */
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
     * 构建字段分布TOP N查询SQL，使用Doris的TOPN函数
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

        sql.append("SELECT ")
                .append(topnColumns.toString())
                .append(" FROM ")
                .append(tableName)
                .append(" WHERE log_time >= '")
                .append(dto.getStartTime())
                .append("'")
                .append(" AND log_time < '")
                .append(dto.getEndTime())
                .append("'");

        appendSearchConditions(sql, dto);

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
