package com.hinadt.miaocha.application.service.sql.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 数字运算符转换器
 *
 * <p>用于解决Doris数据库在处理variant字段与数字比较时的缺陷。 通过在比较运算符后的数字添加"*1"来规避问题。
 *
 * <p>转换规则： - message.marker.duration > 100 -> message.marker.duration > 100*1 -
 * message.marker.duration >= 200 -> message.marker.duration >= 200*1 - message.marker.duration <
 * 300 -> message.marker.duration < 300*1 - message.marker.duration <= 400 ->
 * message.marker.duration <= 400*1 - message.marker.duration = 500 -> message.marker.duration =
 * 500*1 - message.marker.duration != 600 -> message.marker.duration != 600*1
 *
 * <p>注意事项： - 只处理variant字段（包含点语法的字段）与数字的比较 - 不处理引号内的内容 - 支持整数和小数 - 支持负数
 */
@Component
public class NumericOperatorConverter {

    /** 用于匹配variant字段与数字比较的正则表达式 匹配模式：字段名 运算符 数字 例如：message.data.count > 100 */
    private static final Pattern VARIANT_NUMERIC_PATTERN =
            Pattern.compile(
                    "(\\b[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z0-9_.]+)(\\s*(?:>=|<=|!=|>|<|=)\\s*)(-?\\d+(?:\\.\\d+)?)(?![*/+\\-eE]|\\d)");

    // 用于检测字符串是否在引号内的辅助模式
    private static final Pattern QUOTE_PATTERN = Pattern.compile("['\"].*?['\"]|['\"][^'\"]*$");

    /**
     * 转换WHERE条件中的数字运算符
     *
     * @param whereClause WHERE条件子句
     * @return 转换后的WHERE条件
     */
    public String convertNumericOperators(String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return whereClause;
        }

        return convertNumericOperatorsSafely(whereClause);
    }

    /**
     * 安全地转换数字运算符，正确处理引号内的内容
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    private String convertNumericOperatorsSafely(String input) {
        // 首先找出所有引号内的内容位置，避免在引号内进行转换
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        Matcher quoteMatcher = QUOTE_PATTERN.matcher(input);

        while (quoteMatcher.find()) {
            // 处理引号前的部分
            String beforeQuote = input.substring(lastEnd, quoteMatcher.start());
            result.append(convertInNonQuotedText(beforeQuote));

            // 直接添加引号内的内容，不进行转换
            result.append(quoteMatcher.group());

            lastEnd = quoteMatcher.end();
        }

        // 处理最后一部分（如果有的话）
        if (lastEnd < input.length()) {
            String remaining = input.substring(lastEnd);
            result.append(convertInNonQuotedText(remaining));
        }

        return result.toString();
    }

    /**
     * 在非引号文本中进行转换
     *
     * @param text 非引号文本
     * @return 转换后的文本
     */
    private String convertInNonQuotedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        Matcher matcher = VARIANT_NUMERIC_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String operator = matcher.group(2);
            String number = matcher.group(3);

            // 验证字段名是否确实是variant字段（包含点语法）
            if (isVariantField(fieldName)) {
                // 构建替换字符串：字段名 运算符 数字*1（保留原有空格）
                String replacement = fieldName + operator + number + "*1";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // 不是variant字段，保持原样
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 检查字段是否是variant字段（包含点语法）
     *
     * @param fieldName 字段名
     * @return 是否是variant字段
     */
    private boolean isVariantField(String fieldName) {
        return fieldName != null && fieldName.contains(".") && isValidVariantFieldName(fieldName);
    }

    /**
     * 验证是否是有效的variant字段名
     *
     * @param fieldName 字段名
     * @return 是否有效
     */
    private boolean isValidVariantFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }

        String[] parts = fieldName.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        // 检查根字段（第一个部分）必须是有效的标识符
        if (!isValidRootIdentifier(parts[0])) {
            return false;
        }

        // 检查子字段（后续部分）
        for (int i = 1; i < parts.length; i++) {
            if (!isValidSubIdentifier(parts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否是有效的根字段标识符
     *
     * @param identifier 标识符
     * @return 是否有效
     */
    private boolean isValidRootIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // 第一个字符必须是字母或下划线
        char firstChar = identifier.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            return false;
        }

        // 后续字符可以是字母、数字或下划线
        for (int i = 1; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否是有效的子字段标识符
     *
     * @param identifier 标识符
     * @return 是否有效
     */
    private boolean isValidSubIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // 子字段可以以数字开头
        for (char c : identifier.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查WHERE条件是否需要数字运算符转换
     *
     * @param whereClause WHERE条件
     * @return 是否需要转换
     */
    public boolean needsNumericOperatorConversion(String whereClause) {
        if (whereClause == null || whereClause.trim().isEmpty()) {
            return false;
        }

        // 简单检查是否包含variant字段和数字比较的模式
        return VARIANT_NUMERIC_PATTERN.matcher(whereClause).find();
    }
}
