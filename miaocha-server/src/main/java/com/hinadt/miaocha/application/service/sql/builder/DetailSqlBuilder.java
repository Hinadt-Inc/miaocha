package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import org.springframework.stereotype.Component;

/**
 * 详情查询SQL构建器
 *
 * <p>专门负责构建日志详情查询的SQL
 */
@Component
public class DetailSqlBuilder {

    private final KeywordConditionBuilder keywordConditionBuilder;

    public DetailSqlBuilder(KeywordConditionBuilder keywordConditionBuilder) {
        this.keywordConditionBuilder = keywordConditionBuilder;
    }

    /** 构建日志详情查询SQL */
    public String buildDetailQuery(LogSearchDTO dto, String tableName, String timeField) {
        String searchConditions = keywordConditionBuilder.buildKeywordConditions(dto);

        return selectFields(dto.getFields())
                + from(tableName)
                + where(timeRange(timeField, dto))
                + and(searchConditions)
                + orderBy(timeField, "DESC")
                + limit(dto.getPageSize(), dto.getOffset());
    }

    /** 构建总数查询SQL */
    public String buildCountQuery(LogSearchDTO dto, String tableName, String timeField) {
        String searchConditions = keywordConditionBuilder.buildKeywordConditions(dto);

        return selectCount()
                + from(tableName)
                + where(timeRange(timeField, dto))
                + and(searchConditions);
    }
}
