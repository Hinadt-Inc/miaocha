package com.hina.log.service.sql.builder;

import com.hina.log.dto.LogSearchDTO;
import com.hina.log.service.sql.builder.condition.SearchConditionManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQL语句构建器
 */
@Component
public class LogSqlBuilder {

    private final SearchConditionManager searchConditionManager;

    @Autowired
    public LogSqlBuilder(SearchConditionManager searchConditionManager) {
        this.searchConditionManager = searchConditionManager;
    }

    /**
     * 构建日志分布统计SQL
     */
    public String buildDistributionSql(LogSearchDTO dto, String timeUnit) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT date_trunc(log_time, '").append(timeUnit).append("') AS log_time_, ")
                .append(" COUNT(1) AS count ")
                .append("FROM ").append(dto.getTableName())
                .append(" WHERE log_time >= '").append(dto.getStartTime()).append("'")
                .append(" AND log_time <= '").append(dto.getEndTime()).append("'");

        appendSearchConditions(sql, dto);

        sql.append(" GROUP BY date_trunc(log_time, '").append(timeUnit).append("')")
                .append(" ORDER BY log_time_ DESC");

        return sql.toString();
    }

    /**
     * 构建详细日志查询SQL
     */
    public String buildDetailSql(LogSearchDTO dto) {
        StringBuilder sql = new StringBuilder();

        if (dto.getFields() != null && !dto.getFields().isEmpty()) {
            sql.append("SELECT ").append(String.join(", ", dto.getFields()));
        } else {
            sql.append("SELECT *");
        }

        sql.append(" FROM ").append(dto.getTableName())
                .append(" WHERE log_time >= '").append(dto.getStartTime()).append("'")
                .append(" AND log_time <= '").append(dto.getEndTime()).append("'");

        appendSearchConditions(sql, dto);

        sql.append(" ORDER BY log_time DESC")
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
     */
    private void appendSearchConditions(StringBuilder sql, LogSearchDTO dto) {
        String conditions = searchConditionManager.buildSearchConditions(dto);
        if (StringUtils.isNotBlank(conditions)) {
            sql.append(" AND ").append(conditions);
        }
    }
}