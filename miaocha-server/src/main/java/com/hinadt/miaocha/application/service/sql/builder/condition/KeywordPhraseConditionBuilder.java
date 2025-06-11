package com.hinadt.miaocha.application.service.sql.builder.condition;

import com.hinadt.miaocha.common.constants.FieldConstants;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 关键字MATCH_PHRASE条件构建器 使用MATCH_PHRASE实现简单的关键字搜索，不做复杂表达式优化 替代原有的MATCH_ANY和MATCH_ALL实现
 *
 * <p>处理规则： - 单个关键字：'error' -> MATCH_PHRASE 'error' - OR表达式：'error' || 'warning' -> ( MATCH_PHRASE
 * 'error' OR MATCH_PHRASE 'warning' ) - AND表达式：'error' && 'critical' -> ( MATCH_PHRASE 'error' AND
 * MATCH_PHRASE 'critical' ) - 复杂表达式：('error' || 'warning') && 'critical' -> ( ( MATCH_PHRASE
 * 'error' OR MATCH_PHRASE 'warning' ) AND MATCH_PHRASE 'critical' )
 */
@Component
@Order(5) // 最高优先级，优先于旧的构建器
public class KeywordPhraseConditionBuilder implements SearchConditionBuilder {

    private static final Pattern QUOTED_PATTERN = Pattern.compile("'([^']*)'");

    @Override
    public boolean supports(LogSearchDTO dto) {
        // 检查keywords列表
        return dto.getKeywords() != null
                && !dto.getKeywords().isEmpty()
                && dto.getKeywords().stream().anyMatch(StringUtils::isNotBlank);
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        if (!supports(dto)) {
            return "";
        }

        StringBuilder condition = new StringBuilder();
        boolean isFirstCondition = true;

        // 处理keywords列表
        for (String keyword : dto.getKeywords()) {
            if (StringUtils.isNotBlank(keyword)) {
                String trimmedKeyword = keyword.trim();
                String parsedCondition = parseKeywordExpression(trimmedKeyword);

                if (StringUtils.isNotBlank(parsedCondition)) {
                    if (!isFirstCondition) {
                        condition.append(" AND ");
                    }
                    condition.append(parsedCondition);
                    isFirstCondition = false;
                }
            }
        }

        // 如果有条件内容，用括号包起来
        String result = condition.toString();
        if (StringUtils.isNotBlank(result)) {
            return "(" + result + ")";
        }
        return result;
    }

    /** 解析关键字表达式，转换为MATCH_PHRASE条件 */
    private String parseKeywordExpression(String expression) {
        expression = normalizeExpression(expression);
        return parseExpression(expression);
    }

    /** 规范化表达式中的空格 确保运算符周围有空格，但保留引号内的空格不变 */
    private String normalizeExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }

        // 在引号外的运算符周围添加空格
        StringBuilder normalized = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = expression.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '\'') {
                inQuotes = !inQuotes;
                normalized.append(c);
            } else if (!inQuotes) {
                // 在引号外处理运算符
                if (c == '&' && i + 1 < chars.length && chars[i + 1] == '&') {
                    // 处理 &&
                    if (normalized.length() > 0
                            && normalized.charAt(normalized.length() - 1) != ' ') {
                        normalized.append(' ');
                    }
                    normalized.append("&&");
                    i++; // 跳过下一个 &
                    if (i + 1 < chars.length && chars[i + 1] != ' ') {
                        normalized.append(' ');
                    }
                } else if (c == '|' && i + 1 < chars.length && chars[i + 1] == '|') {
                    // 处理 ||
                    if (normalized.length() > 0
                            && normalized.charAt(normalized.length() - 1) != ' ') {
                        normalized.append(' ');
                    }
                    normalized.append("||");
                    i++; // 跳过下一个 |
                    if (i + 1 < chars.length && chars[i + 1] != ' ') {
                        normalized.append(' ');
                    }
                } else if (c == '(' || c == ')') {
                    // 处理括号
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
                // 在引号内，直接添加字符
                normalized.append(c);
            }
        }

        return normalized.toString();
    }

    /** 递归解析表达式 */
    private String parseExpression(String expression) {
        expression = expression.trim();

        if (expression.isEmpty()) {
            return "";
        }

        // 处理括号表达式
        if (expression.startsWith("(") && expression.endsWith(")")) {
            // 检查括号是否匹配整个表达式
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

    /** 查找顶层操作符的索引位置（不在括号内的） */
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

    /** 处理单个词项 */
    private String parseSingleTerm(String term) {
        term = term.trim();
        String keyword = extractKeyword(term);
        if (keyword.isEmpty()) {
            return "";
        }
        return FieldConstants.MESSAGE_FIELD + " MATCH_PHRASE '" + keyword + "'";
    }

    /** 提取关键字（去除引号） */
    private String extractKeyword(String term) {
        // 使用正则表达式提取引号中的内容
        Matcher matcher = QUOTED_PATTERN.matcher(term);
        if (matcher.find()) {
            return matcher.group(1).trim(); // 返回引号内的内容
        }

        // 如果没有引号，直接返回整个词
        return term.trim();
    }
}
