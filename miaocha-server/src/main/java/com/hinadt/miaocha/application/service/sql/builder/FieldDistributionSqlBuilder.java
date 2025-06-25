package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 字段分布SQL构建器
 *
 * <p>专门负责构建字段分布统计查询的SQL
 */
@Component
public class FieldDistributionSqlBuilder {

    /** 字段分布统计的采样大小常量 */
    public static final int SAMPLE_SIZE = 5000;

    private final KeywordConditionBuilder keywordConditionBuilder;

    public FieldDistributionSqlBuilder(KeywordConditionBuilder keywordConditionBuilder) {
        this.keywordConditionBuilder = keywordConditionBuilder;
    }

    /**
     * 构建字段分布TOP N查询SQL
     *
     * <p>使用两层查询优化性能： - 内层：先筛选出前5000条数据 - 外层：对这5000条数据使用Doris的TOPN函数进行统计
     */
    public String buildFieldDistribution(
            LogSearchDTO dto,
            String tableName,
            String timeField,
            List<String> fields,
            List<String> originalFields,
            int topN) {
        String topnColumns = buildTopnColumns(fields, originalFields, topN);
        String innerQuery = buildInnerQuery(dto, tableName, timeField);

        return selectWithSubquery(topnColumns, innerQuery, "sub_query");
    }

    /** 构建TOPN函数调用列表 */
    private String buildTopnColumns(List<String> fields, List<String> originalFields, int topN) {
        StringBuilder columns = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                columns.append(", ");
            }

            String field = fields.get(i);
            String originalField = i < originalFields.size() ? originalFields.get(i) : field;
            columns.append(String.format("TOPN(%s, %d) AS '%s'", field, topN, originalField));
        }

        return columns.toString();
    }

    /** 构建内层查询（采样查询） */
    private String buildInnerQuery(LogSearchDTO dto, String tableName, String timeField) {
        String searchConditions = keywordConditionBuilder.buildKeywordConditions(dto);

        return selectFields(null)
                + from(tableName)
                + where(timeRange(timeField, dto))
                + and(searchConditions)
                + orderBy(timeField, "DESC")
                + limit(SAMPLE_SIZE, 0);
    }
}
