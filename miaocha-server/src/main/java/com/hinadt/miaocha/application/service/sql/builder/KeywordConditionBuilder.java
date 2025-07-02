package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO.KeywordFieldConfigDTO;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关键字条件构建器
 *
 * <p>处理基于keywords字段的查询条件构建，支持： - 自动应用模块配置中的所有关键字字段 - 配置驱动的搜索方法（LIKE, MATCH_PHRASE, MATCH_ANY,
 * MATCH_ALL） - 复杂表达式解析（&& 和 || 运算符，括号嵌套） - 自动处理 variant 字段转换 - 多个关键字之间使用AND连接，每个关键字对所有配置字段使用OR连接
 */
@Component
public class KeywordConditionBuilder {

    @Autowired private QueryConfigValidationService queryConfigValidationService;
    @Autowired private VariantFieldConverter variantFieldConverter;

    /**
     * 构建关键字查询条件
     *
     * <p>处理keywords字段，自动应用模块配置中的所有关键字字段
     */
    public String buildKeywords(LogSearchDTO dto) {
        if (dto.getKeywords() == null || dto.getKeywords().isEmpty()) {
            return "";
        }

        String module = dto.getModule();

        // 获取模块配置的关键字字段（包含variant转换）
        List<KeywordFieldConfigDTO> keywordFields = getKeywordFieldsWithVariantConversion(module);
        if (keywordFields.isEmpty()) {
            return ""; // 没有配置关键字字段，不处理
        }

        StringBuilder condition = new StringBuilder();
        boolean isFirstKeyword = true;

        // 处理每个关键字
        for (String keyword : dto.getKeywords()) {
            if (StringUtils.isBlank(keyword)) {
                continue;
            }

            String trimmedKeyword = keyword.trim();
            StringBuilder keywordCondition = new StringBuilder();
            boolean isFirstField = true;

            // 对每个配置的字段应用关键字
            for (KeywordFieldConfigDTO fieldConfig : keywordFields) {
                String fieldName = fieldConfig.getFieldName();
                String searchMethodName = fieldConfig.getSearchMethod();

                // 获取搜索方法并解析表达式
                SearchMethod searchMethod = SearchMethod.fromString(searchMethodName);
                String parsedCondition = searchMethod.parseExpression(fieldName, trimmedKeyword);

                if (StringUtils.isNotBlank(parsedCondition)) {
                    if (!isFirstField) {
                        keywordCondition.append(" OR ");
                    }
                    // 每个字段条件都用括号包围
                    keywordCondition.append("(").append(parsedCondition).append(")");
                    isFirstField = false;
                }
            }

            String currentKeywordCondition = keywordCondition.toString();
            if (StringUtils.isNotBlank(currentKeywordCondition)) {
                if (!isFirstKeyword) {
                    condition.append(" AND ");
                }

                // 如果是多字段，需要外层括号；单字段直接使用
                if (keywordFields.size() > 1) {
                    condition.append("(").append(currentKeywordCondition).append(")");
                } else {
                    condition.append(currentKeywordCondition);
                }
                isFirstKeyword = false;
            }
        }

        String result = condition.toString();

        // 只有多个关键字时才需要最外层括号
        if (StringUtils.isNotBlank(result) && dto.getKeywords().size() > 1) {
            // 计算实际有效关键字的数量
            long validKeywordCount =
                    dto.getKeywords().stream().filter(StringUtils::isNotBlank).count();

            if (validKeywordCount > 1) {
                return "(" + result + ")";
            }
        }

        return result;
    }

    /**
     * 获取模块配置的关键字字段，并处理 variant 转换
     *
     * @param module 模块名
     * @return 关键字字段配置列表（字段名已处理 variant 转换）
     */
    private List<KeywordFieldConfigDTO> getKeywordFieldsWithVariantConversion(String module) {

        // 验证并获取查询配置
        var queryConfig = queryConfigValidationService.validateAndGetQueryConfig(module);
        List<KeywordFieldConfigDTO> keywordFields = queryConfig.getKeywordFields();

        if (keywordFields == null || keywordFields.isEmpty()) {
            // 如果没有配置关键字字段，返回空列表，不抛异常
            return List.of();
        }

        // 处理 variant 字段转换
        return keywordFields.stream().map(this::convertVariantField).toList();
    }

    /**
     * 转换单个字段配置中的 variant 字段
     *
     * @param original 原始字段配置
     * @return 转换后的字段配置
     */
    private KeywordFieldConfigDTO convertVariantField(KeywordFieldConfigDTO original) {
        String fieldName = original.getFieldName();

        // 检查是否需要 variant 转换
        if (variantFieldConverter.needsVariantConversion(fieldName)) {
            // 创建新的配置对象，转换字段名
            KeywordFieldConfigDTO converted = new KeywordFieldConfigDTO();
            converted.setFieldName(variantFieldConverter.convertTopnField(fieldName));
            converted.setSearchMethod(original.getSearchMethod());
            return converted;
        }

        return original; // 不需要转换，返回原始对象
    }
}
