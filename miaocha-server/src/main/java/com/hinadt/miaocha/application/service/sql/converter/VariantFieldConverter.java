package com.hinadt.miaocha.application.service.sql.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Variant字段转换器
 *
 * <p>用于将点语法转换为Doris的括号语法，支持WHERE条件和SELECT字段
 *
 * <p>转换规则： - message.logId -> message['logId'] - message.marker.data -> message['marker']['data'] -
 * 支持多层嵌套，支持Unicode字符，正确处理引号内的内容 - 根字段必须符合标识符规范，子字段可以以数字开头
 */
@Component
public class VariantFieldConverter {

    // ==================== 公共接口方法 ====================

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
     * 转换SELECT字段列表中的点语法为括号语法
     *
     * <p>⚠️ **重要：此方法必须严格保持输入与输出的顺序一致性！**
     *
     * @param fields 字段列表
     * @return 转换后的字段列表（仅转换语法，不添加AS别名）
     */
    public List<String> convertSelectFields(List<String> fields) {
        if (fields == null) {
            return null;
        }

        if (fields.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> convertedFields = new ArrayList<>();
        for (String field : fields) {
            convertedFields.add(convertSingleField(field));
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
        return convertSingleField(field);
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

    // ==================== 字段转换核心逻辑 ====================

    /**
     * 转换单个字段
     *
     * @param field 字段名
     * @return 转换后的字段名
     */
    private String convertSingleField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return field;
        }

        String trimmedField = field.trim();
        if (needsVariantConversion(trimmedField)) {
            return convertDotToBracketSyntax(trimmedField);
        }
        return field;
    }

    /**
     * 将点语法转换为括号语法
     *
     * <p>示例： - message.logId -> message['logId'] - message.marker.data -> message['marker']['data']
     * - a.b.c.d -> a['b']['c']['d']
     *
     * @param dotSyntax 点语法字段
     * @return 括号语法字段
     */
    private String convertDotToBracketSyntax(String dotSyntax) {
        String[] parts = dotSyntax.split("\\.");
        if (parts.length < 2) {
            return dotSyntax;
        }

        StringBuilder result = new StringBuilder();
        result.append(parts[0]); // 根字段

        // 为每个子字段添加括号语法
        for (int i = 1; i < parts.length; i++) {
            result.append("['").append(parts[i]).append("']");
        }

        return result.toString();
    }

    // ==================== WHERE条件安全转换 ====================

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
        if (!currentToken.isEmpty()) {
            String token = currentToken.toString().trim();
            if (!inQuote && needsVariantConversion(token)) {
                result.append(convertDotToBracketSyntax(token));
            } else {
                result.append(currentToken.toString());
            }
            currentToken.setLength(0);
        }
    }

    // ==================== 字段验证逻辑 ====================

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

        // 避免将数字字面量（如25.5、-123.45等）当作字段名处理
        if (isNumericLiteral(field)) {
            return false;
        }

        // 检查是否是有效的标识符.标识符格式
        String[] parts = field.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        // 检查根字段（第一个部分）必须是有效的标识符
        if (!isValidRootIdentifier(parts[0])) {
            return false;
        }

        // 检查子字段（后续部分）可以更宽松
        for (int i = 1; i < parts.length; i++) {
            if (!isValidSubIdentifier(parts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否是有效的根字段标识符（严格限制） 根字段必须以字母、下划线或Unicode字母开头
     *
     * @param identifier 标识符
     * @return 是否是有效的根字段标识符
     */
    private boolean isValidRootIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // 第一个字符必须是字母、下划线或Unicode字母
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return false;
        }

        // 后续字符可以是字母、数字、下划线或某些Unicode字符
        return isValidIdentifierChars(identifier);
    }

    /**
     * 检查是否是有效的子字段标识符（宽松限制） 子字段可以以数字开头，因为Doris的Variant字段支持这种情况
     *
     * @param identifier 标识符
     * @return 是否是有效的子字段标识符
     */
    private boolean isValidSubIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // 子字段的第一个字符可以是字母、数字、下划线或Unicode字母
        char firstChar = identifier.charAt(0);
        if (!Character.isLetterOrDigit(firstChar) && firstChar != '_') {
            return false;
        }

        // 后续字符可以是字母、数字、下划线或某些Unicode字符
        return isValidIdentifierChars(identifier);
    }

    /**
     * 检查标识符字符是否有效（公共逻辑）
     *
     * @param identifier 标识符
     * @return 字符是否有效
     */
    private boolean isValidIdentifierChars(String identifier) {
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && !isValidUnicodeChar(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查字符串是否是数字字面量（包括整数和小数）
     *
     * @param str 待检查的字符串
     * @return 是否是数字字面量
     */
    private boolean isNumericLiteral(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();

        // 尝试解析为数字
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 检查是否是有效的Unicode字符（如emoji等） */
    private boolean isValidUnicodeChar(char c) {
        int type = Character.getType(c);
        return type == Character.OTHER_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || Character.isHighSurrogate(c)
                || Character.isLowSurrogate(c)
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

    // ==================== 兼容性方法（已废弃） ====================

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
}
