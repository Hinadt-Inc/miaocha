package com.hinadt.miaocha.application.service.sql.converter;

import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LogSearchDTO转换服务
 *
 * <p>职责： 1. 协调各种字段转换器 2. 创建装饰器包装转换后的数据 3. 提供统一的转换入口
 *
 * <p>设计模式： - 装饰器模式：包装原始DTO而不修改 - 策略模式：不同类型字段使用不同转换策略 - 单一职责：只负责转换协调，不涉及SQL构建
 */
@Service
public class LogSearchDTOConverter {

    private final VariantFieldConverter variantFieldConverter;

    @Autowired
    public LogSearchDTOConverter(VariantFieldConverter variantFieldConverter) {
        this.variantFieldConverter = variantFieldConverter;
    }

    /**
     * 转换LogSearchDTO，返回包含转换后字段的装饰器
     *
     * @param original 原始DTO
     * @return 装饰后的DTO
     */
    public LogSearchDTO convert(LogSearchDTO original) {
        if (original == null) {
            return null;
        }

        // 转换各个字段
        List<String> convertedFields = convertSelectFields(original.getFields());
        List<String> convertedWhereSqls = convertWhereClauses(original.getWhereSqls());
        List<KeywordConditionDTO> convertedKeywordConditions =
                convertKeywordConditions(original.getKeywordConditions());

        // 构建字段映射
        Map<String, String> fieldMapping = buildFieldMapping(original, convertedKeywordConditions);

        // 始终创建装饰器，提供统一的接口给下游使用
        return new LogSearchDTODecorator(
                original,
                convertedFields,
                convertedWhereSqls,
                convertedKeywordConditions,
                fieldMapping);
    }

    /** 构建字段映射关系 */
    private Map<String, String> buildFieldMapping(
            LogSearchDTO original, List<KeywordConditionDTO> convertedKeywordConditions) {
        Map<String, String> mapping = new HashMap<>();

        if (original.getKeywordConditions() != null
                && convertedKeywordConditions != null
                && original.getKeywordConditions().size() == convertedKeywordConditions.size()) {

            for (int i = 0; i < original.getKeywordConditions().size(); i++) {
                String originalField = original.getKeywordConditions().get(i).getFieldName();
                String convertedField = convertedKeywordConditions.get(i).getFieldName();

                if (!originalField.equals(convertedField)) {
                    mapping.put(convertedField, originalField);
                }
            }
        }

        return mapping;
    }

    /**
     * 转换SELECT字段列表
     *
     * @param fields 原始字段列表
     * @return 转换后的字段列表，如果不需要转换则返回原始对象
     */
    private List<String> convertSelectFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return fields;
        }

        // 检查是否需要转换
        boolean needsConversion = fields.stream().anyMatch(this::needsVariantConversion);

        if (!needsConversion) {
            return fields; // 返回原始对象，避免不必要的复制
        }

        return variantFieldConverter.convertSelectFields(fields);
    }

    /**
     * 转换WHERE条件列表
     *
     * @param whereClauses 原始WHERE条件列表
     * @return 转换后的WHERE条件列表，如果不需要转换则返回原始对象
     */
    private List<String> convertWhereClauses(List<String> whereClauses) {
        if (whereClauses == null || whereClauses.isEmpty()) {
            return whereClauses;
        }

        // 检查是否需要转换
        boolean needsConversion = whereClauses.stream().anyMatch(this::needsVariantConversion);

        if (!needsConversion) {
            return whereClauses; // 返回原始对象，避免不必要的复制
        }

        return variantFieldConverter.convertWhereClauses(whereClauses);
    }

    /**
     * 转换关键字条件列表
     *
     * @param keywordConditions 原始关键字条件列表
     * @return 转换后的关键字条件列表，如果不需要转换则返回原始对象
     */
    private List<KeywordConditionDTO> convertKeywordConditions(
            List<KeywordConditionDTO> keywordConditions) {
        if (keywordConditions == null || keywordConditions.isEmpty()) {
            return keywordConditions;
        }

        // 检查是否需要转换
        boolean needsConversion =
                keywordConditions.stream()
                        .anyMatch(condition -> needsVariantConversion(condition.getFieldName()));

        if (!needsConversion) {
            return keywordConditions; // 返回原始对象，避免不必要的复制
        }

        // 转换字段名
        return keywordConditions.stream()
                .map(this::convertKeywordCondition)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个关键字条件
     *
     * @param original 原始关键字条件
     * @return 转换后的关键字条件
     */
    private KeywordConditionDTO convertKeywordCondition(KeywordConditionDTO original) {
        if (original == null || !needsVariantConversion(original.getFieldName())) {
            return original;
        }

        // 创建新的条件对象，只转换字段名
        KeywordConditionDTO converted = new KeywordConditionDTO();
        converted.setFieldName(variantFieldConverter.convertTopnField(original.getFieldName()));
        converted.setSearchValue(original.getSearchValue());
        converted.setSearchMethod(original.getSearchMethod());

        return converted;
    }

    /**
     * 检查字符串是否需要variant转换
     *
     * @param text 待检查的文本
     * @return 是否需要转换
     */
    private boolean needsVariantConversion(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 如果已经包含bracket语法格式，则认为已转换过
        if (text.matches(".*\\w+\\['[^']*'\\].*")) {
            return false;
        }

        // 如果字符串很简单（只包含字母、数字、点、下划线），可能是单纯的字段名
        if (text.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$")) {
            // 简单字段名：直接检查是否包含点
            return text.contains(".");
        }

        // 复杂字符串（包含空格、等号等）：检查是否包含点语法字段名（不在引号内的点）
        return containsValidDotSyntaxField(text);
    }

    /** 检查字符串是否包含有效的点语法字段（不在引号内） */
    private boolean containsValidDotSyntaxField(String text) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '.' && !inSingleQuote && !inDoubleQuote) {
                // 检查这个点是否在有效的字段名中
                if (isValidDotInFieldName(text, i)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** 检查指定位置的点是否是有效字段名中的点 */
    private boolean isValidDotInFieldName(String text, int dotIndex) {
        if (dotIndex == 0 || dotIndex == text.length() - 1) {
            return false; // 点不能在开头或结尾
        }

        // 向前查找字段名开始位置
        int fieldStart = dotIndex - 1;
        while (fieldStart >= 0
                && (Character.isLetterOrDigit(text.charAt(fieldStart))
                        || text.charAt(fieldStart) == '_')) {
            fieldStart--;
        }
        fieldStart++; // 调整到字段名的开始位置

        // 向后查找字段名结束位置
        int fieldEnd = dotIndex + 1;
        while (fieldEnd < text.length()
                && (Character.isLetterOrDigit(text.charAt(fieldEnd))
                        || text.charAt(fieldEnd) == '_')) {
            fieldEnd++;
        }

        // 检查点前后是否都有有效的标识符部分
        if (fieldStart >= dotIndex || fieldEnd <= dotIndex + 1) {
            return false;
        }

        // 检查点前的部分是否是有效的标识符（不能以数字开头）
        char firstChar = text.charAt(fieldStart);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return false; // 标识符必须以字母或下划线开头
        }

        // 检查点后的部分是否是有效的标识符开头
        char afterDotChar = text.charAt(dotIndex + 1);
        if (!Character.isLetter(afterDotChar) && afterDotChar != '_') {
            return false; // 点后也必须是有效标识符开头
        }

        return true;
    }
}
