package com.hinadt.miaocha.application.service.sql.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Variant字段转换器 用于将点语法转换为Doris的括号语法，支持WHERE条件和SELECT字段
 *
 * <p>转换规则： - message.logId -> message['logId'] - message.marker.data -> message['marker']['data'] -
 * 支持多层嵌套，支持Unicode字符，正确处理引号内的内容
 */
@Component
public class VariantFieldConverter {

    /**
     * 转换WHERE条件中的点语法为括号语法
     *
     * @param whereClause WHERE条件子句
     * @return 转换后的WHERE条件
     */
    public String convertWhereClause(String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return whereClause;
        }

        return convertDotSyntaxSafely(whereClause);
    }

    /**
     * 安全地转换点语法，正确处理引号内的内容
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    private String convertDotSyntaxSafely(String input) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // 处理引号状态
            if (c == '\'' && !inDoubleQuote) {
                flushToken(result, currentToken, inSingleQuote || inDoubleQuote);
                inSingleQuote = !inSingleQuote;
                result.append(c);
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                flushToken(result, currentToken, inSingleQuote || inDoubleQuote);
                inDoubleQuote = !inDoubleQuote;
                result.append(c);
                continue;
            }

            // 如果在引号内，直接添加字符到token
            if (inSingleQuote || inDoubleQuote) {
                currentToken.append(c);
                continue;
            }

            // 处理分隔符
            if (isTokenSeparator(c)) {
                flushToken(result, currentToken, false);
                result.append(c);
            } else {
                currentToken.append(c);
            }
        }

        // 处理最后的token
        flushToken(result, currentToken, inSingleQuote || inDoubleQuote);
        return result.toString();
    }

    /** 判断是否是token分隔符 */
    private boolean isTokenSeparator(char c) {
        return Character.isWhitespace(c)
                || c == '('
                || c == ')'
                || c == ','
                || c == '='
                || c == '<'
                || c == '>'
                || c == '!'
                || c == '&'
                || c == '|'
                || c == '+'
                || c == '-'
                || c == '*'
                || c == '/'
                || c == '%';
    }

    /** 处理累积的token */
    private void flushToken(StringBuilder result, StringBuilder currentToken, boolean inQuote) {
        if (currentToken.length() > 0) {
            String token = currentToken.toString().trim();
            if (!inQuote && isDotSyntax(token)) {
                result.append(convertDotToBracketSyntax(token));
            } else {
                result.append(currentToken.toString());
            }
            currentToken.setLength(0);
        }
    }

    /**
     * 转换SELECT字段列表中的点语法为括号语法
     *
     * <p>⚠️ **重要：此方法必须严格保持输入与输出的顺序一致性！**
     *
     * <p>顺序依赖说明： - 输出列表的每个索引位置必须对应输入列表的相同索引位置 - 禁止在此方法中进行排序、去重、过滤等改变顺序的操作
     *
     * @param fields 字段列表
     * @return 转换后的字段列表（仅转换语法，不添加AS别名）**顺序与输入严格一致**
     */
    public List<String> convertSelectFields(List<String> fields) {
        if (fields == null) {
            return null;
        }

        if (fields.isEmpty()) {
            // 返回新的空列表，避免返回同一个对象引用
            return new ArrayList<>();
        }

        List<String> convertedFields = new ArrayList<>();
        for (String field : fields) {
            // 处理null字段
            if (field == null) {
                convertedFields.add(field);
                continue;
            }

            // 处理空字符串字段
            if (field.trim().isEmpty()) {
                convertedFields.add(field);
                continue;
            }

            String trimmedField = field.trim();

            // 检查是否是点语法
            if (isDotSyntax(trimmedField)) {
                String bracketSyntax = convertDotToBracketSyntax(trimmedField);
                convertedFields.add(bracketSyntax); // 只转换语法，不添加AS别名
            } else {
                convertedFields.add(field);
            }
        }

        return convertedFields;
    }

    /**
     * 转换TOPN函数中的字段
     *
     * @param field 字段名
     * @return 转换后的字段名（仅转换，不添加别名）
     */
    public String convertTopnField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return field;
        }

        String trimmedField = field.trim();
        if (isDotSyntax(trimmedField)) {
            return convertDotToBracketSyntax(trimmedField);
        }

        return field;
    }

    /**
     * 检查字段是否需要 variant 转换（点语法转括号语法）
     *
     * @param field 字段名
     * @return 是否需要转换
     */
    public boolean needsVariantConversion(String field) {
        // 空值检查
        if (field == null) {
            return false;
        }

        // 基本检查
        if (!field.contains(".")
                || field.contains("[")
                || field.contains("'")
                || field.contains("\"")) {
            return false;
        }

        // 检查是否是有效的标识符.标识符格式
        String[] parts = field.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        // 检查每个部分是否是有效的标识符
        for (String part : parts) {
            if (!isValidIdentifier(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查字段是否使用点语法
     *
     * @param field 字段名
     * @return 是否是点语法
     * @deprecated 使用 {@link #needsVariantConversion(String)} 代替
     */
    @Deprecated
    private boolean isDotSyntax(String field) {
        return needsVariantConversion(field);
    }

    /** 检查是否是有效的标识符（支持Unicode字符） */
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // 第一个字符必须是字母、下划线或Unicode字母
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return false;
        }

        // 后续字符可以是字母、数字、下划线或某些Unicode字符
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && !isValidUnicodeChar(c)) {
                return false;
            }
        }

        return true;
    }

    /** 检查是否是有效的Unicode字符（如emoji等） */
    private boolean isValidUnicodeChar(char c) {
        // 允许一些常见的Unicode符号，如emoji
        int type = Character.getType(c);
        return type == Character.OTHER_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                // 增加对emoji等特殊字符的支持
                || Character.isHighSurrogate(c)
                || Character.isLowSurrogate(c)
                // 处理更多Unicode字符类型，包括emoji
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.EMOTICONS)
                || (Character.UnicodeBlock.of(c)
                        == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS)
                || (Character.UnicodeBlock.of(c)
                        == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS)
                || (Character.UnicodeBlock.of(c)
                        == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS)
                || (Character.UnicodeBlock.of(c)
                        == Character.UnicodeBlock.SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A);
    }

    /**
     * 将点语法转换为括号语法
     *
     * <p>示例： - message.logId -> message['logId'] - message.marker.data -> message['marker']['data']
     * - a.b.c.d -> a['b']['c']['d'] - message.中文字段 -> message['中文字段'] - message.emoji😀 ->
     * message['emoji😀']
     *
     * @param dotSyntax 点语法字段
     * @return 括号语法字段
     */
    private String convertDotToBracketSyntax(String dotSyntax) {
        String[] parts = dotSyntax.split("\\.");
        if (parts.length < 2) {
            return dotSyntax; // 没有点，直接返回
        }

        StringBuilder result = new StringBuilder();
        result.append(parts[0]); // 根字段

        // 为每个子字段添加括号语法
        for (int i = 1; i < parts.length; i++) {
            result.append("['").append(parts[i]).append("']");
        }

        return result.toString();
    }

    /**
     * 批量转换WHERE条件列表
     *
     * @param whereClauses WHERE条件列表
     * @return 转换后的WHERE条件列表
     */
    public List<String> convertWhereClauses(List<String> whereClauses) {
        if (whereClauses == null || whereClauses.isEmpty()) {
            return whereClauses;
        }

        List<String> convertedClauses = new ArrayList<>();
        for (String clause : whereClauses) {
            convertedClauses.add(convertWhereClause(clause));
        }

        return convertedClauses;
    }
}
