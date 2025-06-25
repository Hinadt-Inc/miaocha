package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 字段表达式解析器 复用原有的完整解析逻辑 */
public class FieldExpressionParser {
    private final String fieldName;
    private final SearchMethod searchMethod;
    private static final Pattern QUOTED_PATTERN = Pattern.compile("'([^']*)'");

    public FieldExpressionParser(String fieldName, SearchMethod searchMethod) {
        this.fieldName = fieldName;
        this.searchMethod = searchMethod;
    }

    public String parseKeywordExpression(String expression) {
        expression = normalizeExpression(expression);
        return parseExpression(expression);
    }

    private String normalizeExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }

        StringBuilder normalized = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = expression.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '\'') {
                inQuotes = !inQuotes;
                normalized.append(c);
            } else if (!inQuotes) {
                if (c == '&' && i + 1 < chars.length && chars[i + 1] == '&') {
                    if (normalized.length() > 0
                            && normalized.charAt(normalized.length() - 1) != ' ') {
                        normalized.append(' ');
                    }
                    normalized.append("&&");
                    i++;
                    if (i + 1 < chars.length && chars[i + 1] != ' ') {
                        normalized.append(' ');
                    }
                } else if (c == '|' && i + 1 < chars.length && chars[i + 1] == '|') {
                    if (normalized.length() > 0
                            && normalized.charAt(normalized.length() - 1) != ' ') {
                        normalized.append(' ');
                    }
                    normalized.append("||");
                    i++;
                    if (i + 1 < chars.length && chars[i + 1] != ' ') {
                        normalized.append(' ');
                    }
                } else if (c == '(' || c == ')') {
                    if (c == '('
                            && normalized.length() > 0
                            && normalized.charAt(normalized.length() - 1) != ' ') {
                        normalized.append(' ');
                    }
                    normalized.append(c);
                    if (c == ')' && i + 1 < chars.length && chars[i + 1] != ' ') {
                        normalized.append(' ');
                    }
                } else {
                    normalized.append(c);
                }
            } else {
                normalized.append(c);
            }
        }

        return normalized.toString();
    }

    private String parseExpression(String expression) {
        expression = expression.trim();

        if (expression.isEmpty()) {
            return "";
        }

        // 处理括号表达式
        if (expression.startsWith("(") && expression.endsWith(")")) {
            int level = 0;
            boolean wholeExpression = true;
            for (int i = 0; i < expression.length(); i++) {
                if (expression.charAt(i) == '(') {
                    level++;
                } else if (expression.charAt(i) == ')') {
                    level--;
                    if (level == 0 && i < expression.length() - 1) {
                        wholeExpression = false;
                        break;
                    }
                }
            }

            if (wholeExpression) {
                String inner = expression.substring(1, expression.length() - 1).trim();
                return "( " + parseExpression(inner) + " )";
            }
        }

        // 查找顶层 OR 操作符
        int orIndex = findTopLevelOperatorIndex(expression, " || ");
        if (orIndex != -1) {
            String left = parseExpression(expression.substring(0, orIndex).trim());
            String right = parseExpression(expression.substring(orIndex + 4).trim());
            return left + " OR " + right;
        }

        // 查找顶层 AND 操作符
        int andIndex = findTopLevelOperatorIndex(expression, " && ");
        if (andIndex != -1) {
            String left = parseExpression(expression.substring(0, andIndex).trim());
            String right = parseExpression(expression.substring(andIndex + 4).trim());
            return left + " AND " + right;
        }

        // 如果没有操作符，处理单个项
        return parseSingleTerm(expression);
    }

    private int findTopLevelOperatorIndex(String expression, String operator) {
        int level = 0;
        boolean inQuotes = false;

        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            char c = expression.charAt(i);

            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '(') {
                    level++;
                } else if (c == ')') {
                    level--;
                } else if (level == 0
                        && expression.substring(i, i + operator.length()).equals(operator)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private String parseSingleTerm(String term) {
        term = term.trim();
        String keyword = extractKeyword(term);
        if (keyword.isEmpty()) {
            return "";
        }

        // 转义特殊字符防止SQL注入
        String escapedKeyword = escapeSpecialCharacters(keyword);

        // 使用枚举的SQL生成方法
        return searchMethod.buildSingleCondition(fieldName, escapedKeyword);
    }

    private String extractKeyword(String term) {
        Matcher matcher = QUOTED_PATTERN.matcher(term);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return term.trim();
    }

    private String escapeSpecialCharacters(String input) {
        if (input == null) return "";
        return input.replace("'", "''").replace("\\", "\\\\");
    }
}
