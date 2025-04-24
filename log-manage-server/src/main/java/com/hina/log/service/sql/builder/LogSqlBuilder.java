package com.hina.log.service.sql.builder;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.service.sql.builder.condition.SearchConditionManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQL语句构建器
 * 负责构建各种日志查询相关的SQL语句
 */
@Component
public class LogSqlBuilder {

    // 常量定义，避免魔法值
    private static final String LOG_TIME_COLUMN = "log_time";
    private static final String COUNT_COLUMN = "count";
    private static final String LOG_TIME_ALIAS = "log_time_";

    private final SearchConditionManager searchConditionManager;

    @Autowired
    public LogSqlBuilder(SearchConditionManager searchConditionManager) {
        this.searchConditionManager = searchConditionManager;
    }

    /**
     * 构建日志分布统计SQL
     * 
     * @param dto      日志搜索DTO
     * @param timeUnit 时间单位(second, minute, hour, day, month)
     * @return 分布统计SQL
     */
    public String buildDistributionSql(LogSearchDTO dto, String timeUnit) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT date_trunc(").append(LOG_TIME_COLUMN).append(", '").append(timeUnit).append("') AS ")
                .append(LOG_TIME_ALIAS)
                .append(", COUNT(1) AS ").append(COUNT_COLUMN)
                .append(" FROM ").append(dto.getTableName())
                .append(" WHERE ").append(LOG_TIME_COLUMN).append(" >= '").append(dto.getStartTime()).append("'")
                .append(" AND ").append(LOG_TIME_COLUMN).append(" <= '").append(dto.getEndTime()).append("'");

        appendSearchConditions(sql, dto);

        sql.append(" GROUP BY date_trunc(").append(LOG_TIME_COLUMN).append(", '").append(timeUnit).append("')")
                .append(" ORDER BY ").append(LOG_TIME_ALIAS).append(" DESC");

        return sql.toString();
    }

    /**
     * 构建详细日志查询SQL
     * 
     * @param dto 日志搜索DTO
     * @return 详细日志查询SQL
     */
    public String buildDetailSql(LogSearchDTO dto) {
        StringBuilder sql = new StringBuilder();

        // 构建SELECT子句
        if (dto.getFields() != null && !dto.getFields().isEmpty()) {
            sql.append("SELECT ").append(String.join(", ", dto.getFields()));
        } else {
            sql.append("SELECT *");
        }

        // 构建FROM和WHERE子句
        sql.append(" FROM ").append(dto.getTableName())
                .append(" WHERE ").append(LOG_TIME_COLUMN).append(" >= '").append(dto.getStartTime()).append("'")
                .append(" AND ").append(LOG_TIME_COLUMN).append(" <= '").append(dto.getEndTime()).append("'");

        // 添加搜索条件
        appendSearchConditions(sql, dto);

        // 添加排序和分页
        sql.append(" ORDER BY ").append(LOG_TIME_COLUMN).append(" DESC")
                .append(" LIMIT ").append(dto.getPageSize())
                .append(" OFFSET ").append(dto.getOffset());

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

    /**
     * 添加搜索条件
     * 
     * @param sql SQL构建器
     * @param dto 日志搜索DTO
     */
    private void appendSearchConditions(StringBuilder sql, LogSearchDTO dto) {
        String conditions = searchConditionManager.buildSearchConditions(dto);
        if (StringUtils.isNotBlank(conditions)) {
            sql.append(" AND ").append(conditions);
        }
    }
}