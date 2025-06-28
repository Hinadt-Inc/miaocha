package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.*;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.util.ArrayList;
import java.util.List;
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

        // 为字段添加AS别名（如果需要）
        String selectClause = buildSelectFieldsWithAlias(dto);

        return selectClause
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions)
                + orderBy(timeField, "DESC")
                + limit(dto.getPageSize(), dto.getOffset());
    }

    /** 构建带AS别名的SELECT字段列表 */
    private String buildSelectFieldsWithAlias(LogSearchDTO dto) {
        List<String> fields = dto.getFields();
        if (fields == null || fields.isEmpty()) {
            return "SELECT *";
        }

        // 如果是装饰器，需要为转换后的字段添加AS别名
        if (dto instanceof LogSearchDTODecorator decorator) {

            List<String> convertedFields = decorator.getFields(); // 转换后的纯字段名
            List<String> originalFields = decorator.getOriginalFields(); // 原始字段名
            List<String> fieldsWithAlias = obtainsFieldsWithAlias(convertedFields, originalFields);

            return "SELECT " + String.join(", ", fieldsWithAlias);
        }

        return selectFields(fields);
    }

    private static List<String> obtainsFieldsWithAlias(
            List<String> convertedFields, List<String> originalFields) {
        List<String> fieldsWithAlias = new ArrayList<>();
        for (int i = 0; i < convertedFields.size(); i++) {
            String convertedField = convertedFields.get(i);
            String originalField =
                    i < originalFields.size() ? originalFields.get(i) : convertedField;

            // 如果字段被转换了，添加AS别名
            if (!convertedField.equals(originalField)) {
                fieldsWithAlias.add(convertedField + " AS '" + originalField + "'");
            } else {
                fieldsWithAlias.add(convertedField);
            }
        }
        return fieldsWithAlias;
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
