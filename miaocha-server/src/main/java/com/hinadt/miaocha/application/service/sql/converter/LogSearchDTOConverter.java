package com.hinadt.miaocha.application.service.sql.converter;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LogSearchDTO转换器
 *
 * <p>负责转换LogSearchDTO中需要variant转换的字段，包括： - SELECT字段列表转换 - WHERE条件列表转换
 */
@Service
public class LogSearchDTOConverter {

    private final VariantFieldConverter variantFieldConverter;
    private final NumericOperatorConverter numericOperatorConverter;

    @Autowired
    public LogSearchDTOConverter(
            VariantFieldConverter variantFieldConverter,
            NumericOperatorConverter numericOperatorConverter) {
        this.variantFieldConverter = variantFieldConverter;
        this.numericOperatorConverter = numericOperatorConverter;
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

        // 始终创建装饰器，提供统一的接口给下游使用
        return new LogSearchDTODecorator(original, convertedFields, convertedWhereSqls);
    }

    /**
     * 转换SELECT字段列表
     *
     * <p>⚠️ **顺序依赖契约：输出必须与输入保持严格的顺序一致性**
     *
     * @param fields 原始字段列表
     * @return 转换后的字段列表，如果不需要转换则返回原始对象，**顺序与输入严格一致**
     */
    private List<String> convertSelectFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return fields;
        }

        // 检查是否需要转换
        boolean needsConversion =
                fields.stream().anyMatch(variantFieldConverter::needsVariantConversion);

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

        // 检查是否需要variant字段转换或数字运算符转换
        boolean needsVariantConversion =
                whereClauses.stream().anyMatch(this::containsVariantFields);
        boolean needsNumericConversion =
                whereClauses.stream()
                        .anyMatch(numericOperatorConverter::needsNumericOperatorConversion);

        if (!needsVariantConversion && !needsNumericConversion) {
            return whereClauses; // 返回原始对象，避免不必要的复制
        }

        // 应用转换链：先进行数字运算符转换，再进行variant字段转换
        List<String> convertedClauses = new ArrayList<>(whereClauses.size());
        for (String clause : whereClauses) {
            String processedClause = clause;

            // 1. 先进行数字运算符转换
            if (numericOperatorConverter.needsNumericOperatorConversion(processedClause)) {
                processedClause = numericOperatorConverter.convertNumericOperators(processedClause);
            }

            // 2. 再进行variant字段转换
            if (containsVariantFields(processedClause)) {
                processedClause = variantFieldConverter.convertWhereClause(processedClause);
            }

            convertedClauses.add(processedClause);
        }

        return convertedClauses;
    }

    /**
     * 检查WHERE条件是否包含需要转换的variant字段
     *
     * @param whereClause WHERE条件
     * @return 是否包含variant字段
     */
    private boolean containsVariantFields(String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return false;
        }

        return containsValidDotSyntaxField(whereClause);
    }

    /**
     * 检查字符串是否包含有效的点语法字段（不在引号内的点语法）
     *
     * @param text 待检查的字符串
     * @return 是否包含有效的点语法字段
     */
    private boolean containsValidDotSyntaxField(String text) {
        if (text == null || text.trim().isEmpty() || !text.contains(".")) {
            return false;
        }

        // 使用简单的状态机来检查引号状态
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                if (!currentWord.isEmpty() && !inSingleQuote) {
                    // 检查引号前的词
                    if (variantFieldConverter.needsVariantConversion(
                            currentWord.toString().trim())) {
                        return true;
                    }
                    currentWord.setLength(0);
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                if (!currentWord.isEmpty() && !inDoubleQuote) {
                    // 检查引号前的词
                    if (variantFieldConverter.needsVariantConversion(
                            currentWord.toString().trim())) {
                        return true;
                    }
                    currentWord.setLength(0);
                }
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (inSingleQuote || inDoubleQuote) {
                // 在引号内，跳过
                continue;
            }

            if (Character.isWhitespace(c) || isSpecialChar(c)) {
                // 词结束
                if (!currentWord.isEmpty()) {
                    if (variantFieldConverter.needsVariantConversion(
                            currentWord.toString().trim())) {
                        return true;
                    }
                    currentWord.setLength(0);
                }
            } else {
                currentWord.append(c);
            }
        }

        // 检查最后一个词
        if (!currentWord.isEmpty()) {
            return variantFieldConverter.needsVariantConversion(currentWord.toString().trim());
        }

        return false;
    }

    /**
     * 检查字符是否是特殊字符（用于词边界判断）
     *
     * @param c 字符
     * @return 是否是特殊字符
     */
    private boolean isSpecialChar(char c) {
        return c == '(' || c == ')' || c == ',' || c == '=' || c == '<' || c == '>' || c == '!'
                || c == '&' || c == '|' || c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }
}
