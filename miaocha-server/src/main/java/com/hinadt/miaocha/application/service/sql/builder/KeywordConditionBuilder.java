package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.application.service.sql.expression.FieldExpressionParser;
import com.hinadt.miaocha.application.service.sql.expression.SqlFragment;
import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO.KeywordFieldConfigDTO;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关键字条件构建器 - 重构版本
 *
 * <p>职责清晰： - 使用 FieldExpressionParser 进行表达式分析 - 使用 SqlFragment 统一括号规范 - 根据分析结果构建 SQL 条件
 */
@Component
public class KeywordConditionBuilder {

    @Autowired private QueryConfigValidationService queryConfigValidationService;
    @Autowired private VariantFieldConverter variantFieldConverter;

    /** 构建关键字查询条件 */
    public String buildKeywords(LogSearchDTO logSearchDTO) {
        if (CollectionUtils.isEmpty(logSearchDTO.getKeywords())) {
            return "";
        }

        List<KeywordFieldConfigDTO> keywordFields =
                getKeywordFieldsWithVariantConversion(logSearchDTO.getModule());
        if (CollectionUtils.isEmpty(keywordFields)) {
            return "";
        }

        // 检查是否包含负向条件
        boolean hasNegativeTerms =
                logSearchDTO.getKeywords().stream()
                        .anyMatch(FieldExpressionParser::containsNegativeTerms);

        if (hasNegativeTerms) {
            return buildComplexKeywords(logSearchDTO, keywordFields);
        } else {
            return buildSimpleKeywords(logSearchDTO, keywordFields);
        }
    }

    /** 构建复杂关键字查询（包含负向条件） */
    private String buildComplexKeywords(
            LogSearchDTO logSearchDTO, List<KeywordFieldConfigDTO> keywordFields) {
        FieldExpressionParser.ExpressionSeparationResult separationResult =
                FieldExpressionParser.separateKeywordExpressions(logSearchDTO.getKeywords());

        List<String> conditions = new ArrayList<>();

        // 处理正向条件
        if (separationResult.hasPositiveExpressions()) {
            String positiveCondition =
                    buildPositiveConditions(keywordFields, separationResult.positiveExpressions());
            if (StringUtils.isNotBlank(positiveCondition)) {
                conditions.add(positiveCondition);
            }
        }

        // 处理负向条件
        if (separationResult.hasNegativeExpressions()) {
            String negativeCondition =
                    buildNegativeConditions(keywordFields, separationResult.negativeExpressions());
            if (StringUtils.isNotBlank(negativeCondition)) {
                conditions.add(negativeCondition);
            }
        }

        return SqlFragment.formatMultiKeywordAnd(conditions);
    }

    /** 构建简单关键字查询（不包含负向条件） */
    private String buildSimpleKeywords(
            LogSearchDTO dto, List<KeywordFieldConfigDTO> keywordFields) {
        List<String> keywordConditions = new ArrayList<>();

        for (String keyword : dto.getKeywords()) {
            if (StringUtils.isBlank(keyword)) {
                continue;
            }

            String keywordCondition = buildSingleKeywordCondition(keywordFields, keyword.trim());
            if (StringUtils.isNotBlank(keywordCondition)) {
                keywordConditions.add(keywordCondition);
            }
        }

        return SqlFragment.formatMultiKeywordAnd(keywordConditions);
    }

    /** 构建单个关键字的多字段条件 */
    private String buildSingleKeywordCondition(
            List<KeywordFieldConfigDTO> keywordFields, String keyword) {
        List<String> fieldConditions = new ArrayList<>();

        for (KeywordFieldConfigDTO fieldConfig : keywordFields) {
            String fieldName = fieldConfig.getFieldName();
            String searchMethodName = fieldConfig.getSearchMethod();

            SearchMethod searchMethod = SearchMethod.fromString(searchMethodName);
            String parsedCondition = searchMethod.parseExpression(fieldName, keyword);

            if (StringUtils.isNotBlank(parsedCondition)) {
                fieldConditions.add(parsedCondition);
            }
        }

        return SqlFragment.formatMultiFieldOr(fieldConditions);
    }

    /** 构建正向条件 */
    private String buildPositiveConditions(
            List<KeywordFieldConfigDTO> keywordFields, List<String> positiveExpressions) {
        if (positiveExpressions.isEmpty()) {
            return "";
        }

        String combinedExpression = combineExpressions(positiveExpressions);

        List<String> fieldConditions =
                keywordFields.stream()
                        .map(field -> buildFieldExpressionCondition(field, combinedExpression))
                        .filter(StringUtils::isNotBlank)
                        .toList();

        return SqlFragment.formatMultiFieldOr(fieldConditions);
    }

    /** 构建负向条件 */
    private String buildNegativeConditions(
            List<KeywordFieldConfigDTO> keywordFields, List<String> negativeExpressions) {
        if (negativeExpressions.isEmpty()) {
            return "";
        }

        // 提取所有负向条件中的关键词
        Set<String> negativeTerms = new LinkedHashSet<>();
        for (String expression : negativeExpressions) {
            negativeTerms.addAll(
                    FieldExpressionParser.extractNegativeTermsFromExpression(expression));
        }

        if (negativeTerms.isEmpty()) {
            return "";
        }

        // 为每个负向关键词构建所有字段的条件
        List<String> allFieldConditions = new ArrayList<>();
        for (String term : negativeTerms) {
            for (KeywordFieldConfigDTO field : keywordFields) {
                String condition = buildTermCondition(field, term);
                if (StringUtils.isNotBlank(condition)) {
                    allFieldConditions.add(condition);
                }
            }
        }

        return SqlFragment.formatNotCondition(allFieldConditions);
    }

    /** 为单个字段构建表达式条件 */
    private String buildFieldExpressionCondition(KeywordFieldConfigDTO field, String expression) {
        String fieldName = field.getFieldName();
        String searchMethodName = field.getSearchMethod();

        SearchMethod searchMethod = SearchMethod.fromString(searchMethodName);
        return searchMethod.parseExpression(fieldName, expression);
    }

    /** 为字段和关键词构建单个条件 */
    private String buildTermCondition(KeywordFieldConfigDTO field, String term) {
        String fieldName = field.getFieldName();
        String searchMethodName = field.getSearchMethod();

        SearchMethod searchMethod = SearchMethod.fromString(searchMethodName);
        return searchMethod.buildSingleCondition(fieldName, escapeSpecialCharacters(term));
    }

    /** 组合多个表达式 */
    private String combineExpressions(List<String> expressions) {
        if (expressions.isEmpty()) {
            return "";
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        return expressions.stream()
                .map(
                        expr ->
                                """
                    (%s)
                    """
                                        .formatted(expr)
                                        .trim())
                .collect(Collectors.joining(" AND "));
    }

    /** 转义特殊字符防止SQL注入 */
    private String escapeSpecialCharacters(String input) {
        if (input == null) return "";
        return input.replace("'", "''").replace("\\", "\\\\");
    }

    /** 获取模块配置的关键字字段，并处理 variant 转换 */
    private List<KeywordFieldConfigDTO> getKeywordFieldsWithVariantConversion(String module) {
        var queryConfig = queryConfigValidationService.validateAndGetQueryConfig(module);
        List<KeywordFieldConfigDTO> keywordFields = queryConfig.getKeywordFields();

        if (keywordFields == null || keywordFields.isEmpty()) {
            return List.of();
        }

        return keywordFields.stream().map(this::convertVariantField).toList();
    }

    /** 转换单个字段配置中的 variant 字段 */
    private KeywordFieldConfigDTO convertVariantField(KeywordFieldConfigDTO original) {
        String fieldName = original.getFieldName();

        if (variantFieldConverter.needsVariantConversion(fieldName)) {
            KeywordFieldConfigDTO converted = new KeywordFieldConfigDTO();
            converted.setFieldName(variantFieldConverter.convertTopnField(fieldName));
            converted.setSearchMethod(original.getSearchMethod());
            return converted;
        }

        return original;
    }
}
