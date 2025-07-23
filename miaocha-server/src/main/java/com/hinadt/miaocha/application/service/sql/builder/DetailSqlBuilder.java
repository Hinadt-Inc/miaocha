package com.hinadt.miaocha.application.service.sql.builder;

import static com.hinadt.miaocha.application.service.sql.expression.SqlFragment.*;

import com.hinadt.miaocha.application.service.sql.expression.SqlFragment;
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
        String keywordConditions = keywordConditionBuilder.buildKeywords(dto);
        String whereConditions = whereConditionBuilder.buildWhereConditions(dto);

        // 为字段添加AS别名（如果需要）
        String selectClause = buildSelectFieldsWithAlias(dto);

        // 构建排序子句
        String orderClause = buildOrderClause(dto, timeField);

        return selectClause
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions)
                + orderClause
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

    /**
     * 为转换后的字段构建AS别名
     *
     * <p>⚠️ **顺序依赖契约：此方法严格依赖 convertedFields 与 originalFields 的索引对应关系**
     *
     * <p>前置条件： - convertedFields 和 originalFields 必须保持相同的顺序 - convertedFields[i] 必须是
     * originalFields[i] 的variant转换结果 - 此契约由 VariantFieldConverter.convertSelectFields() 方法保证
     *
     * <p>处理逻辑： - 如果字段被转换：生成 "convertedField AS 'originalField'" - 如果字段未转换：直接使用字段名
     *
     * @param convertedFields 转换后的字段列表（由VariantFieldConverter转换）
     * @param originalFields 原始字段列表（与convertedFields顺序严格对应）
     * @return 带AS别名的字段列表
     */
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
        String keywordConditions = keywordConditionBuilder.buildKeywords(dto);
        String whereConditions = whereConditionBuilder.buildWhereConditions(dto);

        return selectCount()
                + from(tableName)
                + buildWhereClause(timeCondition, keywordConditions, whereConditions);
    }

    /**
     * 构建排序子句
     *
     * <p>逻辑说明： 1. 如果用户指定了排序字段，优先使用用户指定的排序 2. 如果用户指定的排序中包含时间字段，以用户指定的为准 3.
     * 如果用户没有指定排序，或指定的排序中不包含时间字段，则追加默认的时间字段倒序排序
     *
     * @param dto 查询DTO
     * @param timeField 时间字段名
     * @return ORDER BY子句
     */
    private String buildOrderClause(LogSearchDTO dto, String timeField) {
        List<SqlFragment.OrderField> orderFields = new ArrayList<>();

        // 1. 添加用户指定的排序字段
        if (dto.getSortFields() != null && !dto.getSortFields().isEmpty()) {
            List<SqlFragment.OrderField> userOrderFields =
                    dto.getSortFields().stream()
                            .map(
                                    sortField ->
                                            new SqlFragment.OrderField(
                                                    sortField.getFieldName(),
                                                    sortField.getDirection()))
                            .toList();
            orderFields.addAll(userOrderFields);
        }

        // 2. 检查是否已经包含时间字段排序
        boolean hasTimeFieldSort =
                orderFields.stream()
                        .anyMatch(orderField -> timeField.equals(orderField.fieldName()));

        // 3. 如果没有时间字段排序，添加默认的时间字段排序
        if (!hasTimeFieldSort) {
            orderFields.add(new SqlFragment.OrderField(timeField, "DESC"));
        }

        // 4. 构建ORDER BY子句
        return orderByMultiple(orderFields);
    }
}
