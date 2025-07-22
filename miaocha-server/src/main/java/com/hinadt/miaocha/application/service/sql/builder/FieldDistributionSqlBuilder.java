package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.expression.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
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
    public static final int SAMPLE_SIZE = 1000;

    private final KeywordConditionBuilder keywordConditionBuilder;
    private final WhereConditionBuilder whereConditionBuilder;

    public FieldDistributionSqlBuilder(
            KeywordConditionBuilder keywordConditionBuilder,
            WhereConditionBuilder whereConditionBuilder) {
        this.keywordConditionBuilder = keywordConditionBuilder;
        this.whereConditionBuilder = whereConditionBuilder;
    }

    /**
     * 构建字段分布TOP N查询SQL
     *
     * <p>使用两层查询优化性能： - 内层：先筛选出前1000条数据 - 外层：对这1000条数据使用Doris的TOPN函数进行统计
     */
    public String buildFieldDistribution(
            LogSearchDTO dto,
            String tableName,
            String timeField,
            List<String> fields,
            List<String> originalFields,
            int topN) {

        // 如果是装饰器，使用转换后的字段进行TOPN，使用原始字段作为AS别名
        if (dto instanceof LogSearchDTODecorator decorator) {

            List<String> convertedFields = decorator.getFields(); // 转换后的纯字段名，用于TOPN函数
            List<String> originalFieldNames = decorator.getOriginalFields(); // 原始字段名，用于AS别名

            String selectColumns =
                    buildTopnColumnsWithTotal(convertedFields, originalFieldNames, topN);
            String innerQuery = buildInnerQuery(dto, tableName, timeField);

            return selectWithSubquery(selectColumns, innerQuery, "sub_query");
        }

        // 普通DTO的处理逻辑保持不变
        String selectColumns = buildTopnColumnsWithTotal(fields, originalFields, topN);
        String innerQuery = buildInnerQuery(dto, tableName, timeField);

        return selectWithSubquery(selectColumns, innerQuery, "sub_query");
    }

    /** 构建包含TOPN函数和总数统计的SELECT列 */
    private String buildTopnColumnsWithTotal(
            List<String> fields, List<String> originalFields, int topN) {
        String topnColumns = buildTopnColumns(fields, originalFields, topN);
        return topnColumns + ", count(*) AS '" + TOTAL_ALIAS + "'";
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
        String timeCondition = timeRange(timeField, dto);
        String keywordConditions = keywordConditionBuilder.buildKeywords(dto);
        String whereConditions = whereConditionBuilder.buildWhereConditions(dto);

        return selectFields(null)
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions)
                + orderBy(timeField, "DESC")
                + limit(SAMPLE_SIZE, 0);
    }
}
