package com.hina.log.service.builder;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * SQL语句构建器
 */
@Component
public class LogSqlBuilder {

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

        if (StringUtils.isNotBlank(dto.getKeyword())) {
            sql.append(" AND message LIKE '%").append(dto.getKeyword()).append("%'");
        }

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

        if (StringUtils.isNotBlank(dto.getKeyword())) {
            sql.append(" AND message LIKE '%").append(dto.getKeyword()).append("%'");
        }

        sql.append(" ORDER BY log_time DESC")
                .append(" LIMIT ").append(dto.getPageSize())
                .append(" OFFSET ").append(dto.getOffset());

        return sql.toString();
    }
}