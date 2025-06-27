package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import org.springframework.stereotype.Component;

/**
 * 详情查询SQL构建器
 *
 * <p>专门负责构建日志详情查询的SQL
 */
@Component
public class DetailSqlBuilder {

    private final KeywordConditionBuilder keywordConditionBuilder;
    private final WhereConditionBuilder whereConditionBuilder;

    public DetailSqlBuilder(
            KeywordConditionBuilder keywordConditionBuilder,
            WhereConditionBuilder whereConditionBuilder) {
        this.keywordConditionBuilder = keywordConditionBuilder;
        this.whereConditionBuilder = whereConditionBuilder;
    }

    /** 构建日志详情查询SQL */
    public String buildDetailQuery(LogSearchDTO dto, String tableName, String timeField) {
        String timeCondition = timeRange(timeField, dto);
        String keywordConditions = keywordConditionBuilder.buildKeywordConditions(dto);
        String whereConditions = whereConditionBuilder.buildWhereConditions(dto);

        return selectFields(dto.getFields())
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions)
                + orderBy(timeField, "DESC")
                + limit(dto.getPageSize(), dto.getOffset());
    }

    /** 构建总数查询SQL */
    public String buildCountQuery(LogSearchDTO dto, String tableName, String timeField) {
        String timeCondition = timeRange(timeField, dto);
        String keywordConditions = keywordConditionBuilder.buildKeywordConditions(dto);
        String whereConditions = whereConditionBuilder.buildWhereConditions(dto);

        return selectCount()
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions);
    }
}
