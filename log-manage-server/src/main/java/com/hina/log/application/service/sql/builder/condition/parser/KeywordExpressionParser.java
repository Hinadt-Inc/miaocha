package com.hina.log.application.service.sql.builder.condition.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** 关键字表达式解析器 用于解析复杂的关键字表达式，支持最多两层嵌套 */
public class KeywordExpressionParser {

    // 表达式操作符
    private static final String AND_OPERATOR = "&&";
    private static final String OR_OPERATOR = "||";

    // 括号匹配的正则表达式 - 匹配括号内容
    private static final Pattern PARENTHESIS_PATTERN = Pattern.compile("\\(([^()]+)\\)");

    // 匹配引号中的内容 - 保留引号内的内容作为完整关键字
    private static final Pattern QUOTED_PATTERN = Pattern.compile("'([^']*)'");

    // 语法错误消息
    private static final String SYNTAX_ERROR_PREFIX = "语法错误: ";
    private static final String UNBALANCED_PARENTHESES = "括号不匹配";
    private static final String EMPTY_PARENTHESES = "空括号";
    private static final String INVALID_OPERATOR_USAGE = "运算符使用不正确";
    private static final String NESTED_TOO_DEEP = "嵌套层级过深，最多支持两层嵌套";
    private static final String UNBALANCED_QUOTES = "引号不匹配";

    /**
     * 验证表达式语法是否正确
     *
     * @param expression 要验证的表达式
     * @return 验证结果对象，包含是否有效和错误消息
     */
    public static ValidationResult validateSyntax(String expression) {
        if (StringUtils.isBlank(expression)) {
            return ValidationResult.valid();
        }

        expression = expression.trim();

        // 检查引号是否匹配
        int quoteCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '\'') {
                quoteCount++;
            }
        }
        if (quoteCount % 2 != 0) {
            return ValidationResult.invalid(UNBALANCED_QUOTES);
        }

        // 检查括号嵌套层级是否超过两层
        int maxNestingLevel = getMaxNestingLevel(expression);
        if (maxNestingLevel > 2) {
            return ValidationResult.invalid(NESTED_TOO_DEEP);
        }

        // 检查括号是否匹配
        int openParenCount = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openParenCount++;
            else if (c == ')') openParenCount--;

            // 如果右括号数量大于左括号，不平衡
            if (openParenCount < 0) {
                return ValidationResult.invalid(UNBALANCED_PARENTHESES);
            }
        }

        // 检查最终是否所有括号都匹配
        if (openParenCount != 0) {
            return ValidationResult.invalid(UNBALANCED_PARENTHESES);
        }

        // 检查空括号 ()
        if (expression.contains("()")) {
            return ValidationResult.invalid(EMPTY_PARENTHESES);
        }

        // 规范化表达式中的空格以进行运算符检查
        String normalizedForOperatorCheck = normalizeExpression(expression);

        // 检查运算符使用
        if (normalizedForOperatorCheck.startsWith(AND_OPERATOR)
                || normalizedForOperatorCheck.startsWith(OR_OPERATOR)
                || normalizedForOperatorCheck.endsWith(AND_OPERATOR)
                || normalizedForOperatorCheck.endsWith(OR_OPERATOR)) {
            return ValidationResult.invalid(INVALID_OPERATOR_USAGE + ": 表达式不能以运算符开始或结束");
        }

        // 检查连续运算符
        if (normalizedForOperatorCheck.contains(AND_OPERATOR + " " + AND_OPERATOR)
                || normalizedForOperatorCheck.contains(OR_OPERATOR + " " + OR_OPERATOR)
                || normalizedForOperatorCheck.contains(AND_OPERATOR + " " + OR_OPERATOR)
                || normalizedForOperatorCheck.contains(OR_OPERATOR + " " + AND_OPERATOR)) {
            return ValidationResult.invalid(INVALID_OPERATOR_USAGE + ": 不能有连续的运算符");
        }

        // 检查括号内容
        Matcher matcher = PARENTHESIS_PATTERN.matcher(expression);
        while (matcher.find()) {
            String innerExpr = matcher.group(1).trim();
            if (innerExpr.isEmpty()) {
                return ValidationResult.invalid(EMPTY_PARENTHESES);
            }

            // 递归检查括号内表达式
            ValidationResult innerResult = validateSyntax(innerExpr);
            if (!innerResult.isValid()) {
                return innerResult;
            }
        }

        return ValidationResult.valid();
    }

    /** 获取表达式的最大嵌套层级 */
    private static int getMaxNestingLevel(String expression) {
        int maxLevel = 0;
        int currentLevel = 0;

        for (char c : expression.toCharArray()) {
            if (c == '(') {
                currentLevel++;
                maxLevel = Math.max(maxLevel, currentLevel);
            } else if (c == ')') {
                currentLevel--;
            }
        }

        return maxLevel;
    }

    /**
     * 解析关键字表达式，转换为Doris SQL条件
     *
     * @param expression 关键字表达式，如 "key1"、"'key1' || 'key2'"、"('key1' && 'key2') || 'key3'"等
     * @return Doris SQL条件或语法错误信息
     */
    public static ParseResult parse(String expression) {
        if (StringUtils.isBlank(expression)) {
            return ParseResult.success("");
        }

        expression = expression.trim();

        // 先验证语法
        ValidationResult validationResult = validateSyntax(expression);
        if (!validationResult.isValid()) {
            return ParseResult.error(SYNTAX_ERROR_PREFIX + validationResult.getErrorMessage());
        }

        try {
            // 解析表达式
            String result = parseExpression(expression);
            return ParseResult.success(result);
        } catch (Exception e) {
            // 捕获解析过程中的任何异常
            return ParseResult.error(SYNTAX_ERROR_PREFIX + "解析表达式时出错: " + e.getMessage());
        }
    }

    /** 解析表达式 */
    private static String parseExpression(String expression) {
        expression = normalizeExpression(expression);
        expression = replaceParenthesesWithParsedExpressions(expression);

        // 查找顶层 OR 操作符
        int orIndex = findTopLevelOperatorIndex(expression, " || ");
        if (orIndex != -1) {
            // 对于OR表达式，我们需要收集所有的关键字到一个MATCH_ANY中
            List<String> orTerms = splitByTopLevelOperator(expression, " || ");
            List<String> keywords = new ArrayList<>();

            for (String term : orTerms) {
                String keyword = extractKeyword(term.trim());
                if (!keyword.isEmpty()) {
                    keywords.add(keyword);
                }
            }

            if (keywords.size() == 1) {
                return "message MATCH_ANY '" + keywords.get(0) + "'";
            } else if (keywords.size() > 1) {
                // 将所有OR关键字合并到一个MATCH_ANY中
                return "message MATCH_ANY '" + String.join(" ", keywords) + "'";
            } else {
                // 如果没有提取到关键字，回退到原有逻辑
                String left = parseExpression(expression.substring(0, orIndex).trim());
                String right = parseExpression(expression.substring(orIndex + 4).trim());
                return left + " OR " + right;
            }
        }

        // 查找顶层 AND 操作符
        int andIndex = findTopLevelOperatorIndex(expression, " && ");
        if (andIndex != -1) {
            String left = parseExpression(expression.substring(0, andIndex).trim());
            String right = parseExpression(expression.substring(andIndex + 4).trim());

            // 只有当左右两边都是简单的MATCH_ANY时，才能合并为MATCH_ALL
            if (isSimpleMatchAny(left) && isSimpleMatchAny(right)) {
                String keywords = extractKeywordsFromExpressions(left, right);
                return "message MATCH_ALL '" + keywords + "'";
            } else {
                // 包含复杂表达式时，保持AND逻辑
                // 去除不必要的外层括号
                String leftClean = removeOuterParentheses(left);
                String rightClean = removeOuterParentheses(right);
                return leftClean + " AND " + rightClean;
            }
        }

        // 如果没有操作符，处理单个项
        return parseSingleTerm(expression);
    }

    /** 规范化表达式中的空格 确保运算符周围有空格，但保留引号内的空格不变 */
    private static String normalizeExpression(String expression) {
        // 替换引号内的内容为占位符
        List<String> quotedStrings = new ArrayList<>();
        Matcher quoteMatcher = QUOTED_PATTERN.matcher(expression);
        StringBuffer tempBuffer = new StringBuffer();
        while (quoteMatcher.find()) {
            String quoted = quoteMatcher.group(0); // 包含引号的完整字符串
            quotedStrings.add(quoted);
            quoteMatcher.appendReplacement(tempBuffer, "QUOTED_" + (quotedStrings.size() - 1));
        }
        quoteMatcher.appendTail(tempBuffer);
        String tempExpression = tempBuffer.toString();

        // 规范化运算符周围的空格
        tempExpression =
                tempExpression
                        .replace("(", " ( ")
                        .replace(")", " ) ")
                        .replace("&&", " && ")
                        .replace("||", " || ");

        // 去除多余的空格
        tempExpression = tempExpression.replaceAll("\\s+", " ").trim();

        // 恢复引号内的内容
        for (int i = 0; i < quotedStrings.size(); i++) {
            tempExpression = tempExpression.replace("QUOTED_" + i, quotedStrings.get(i));
        }

        return tempExpression;
    }

    /** 替换所有括号表达式为其解析结果 */
    private static String replaceParenthesesWithParsedExpressions(String expression) {
        Matcher matcher = PARENTHESIS_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String innerExpr = matcher.group(1).trim();
            String parsedInner = parseExpression(innerExpr); // 递归解析内部表达式
            parsedInner = Matcher.quoteReplacement(parsedInner);
            matcher.appendReplacement(result, "(" + parsedInner + ")");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** 查找顶层操作符的索引 */
    private static int findTopLevelOperatorIndex(String expression, String operator) {
        int parenCount = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (parenCount == 0 && expression.startsWith(operator, i)) {
                return i;
            }
        }
        return -1;
    }

    /** 按照顶层操作符分割表达式 */
    private static List<String> splitByTopLevelOperator(String expression, String operator) {
        List<String> terms = new ArrayList<>();
        int parenCount = 0;
        int start = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (parenCount == 0 && expression.startsWith(operator, i)) {
                terms.add(expression.substring(start, i).trim());
                start = i + operator.length();
                i += operator.length() - 1; // 跳过操作符
            }
        }
        terms.add(expression.substring(start).trim());
        return terms;
    }

    /** 解析单个项 */
    private static String parseSingleTerm(String term) {
        term = term.trim();
        if (term.startsWith("(") && term.endsWith(")")) {
            // 如果括号内已经是解析后的SQL表达式，直接返回
            String inner = term.substring(1, term.length() - 1).trim();
            if (inner.contains("MATCH_ANY") || inner.contains("MATCH_ALL")) {
                return term; // 保持括号，这是已解析的复杂表达式
            }
            // 否则递归解析括号内容
            return parseExpression(inner);
        }
        String keyword = extractKeyword(term);
        if (keyword.isEmpty()) {
            return "";
        }
        return "message MATCH_ANY '" + keyword + "'";
    }

    /** 提取关键字（去除引号） */
    private static String extractKeyword(String term) {
        // 使用正则表达式提取引号中的内容
        Matcher matcher = QUOTED_PATTERN.matcher(term);
        if (matcher.find()) {
            return matcher.group(1).trim(); // 返回引号内的内容
        }

        // 如果没有引号，直接返回整个词
        return term.trim();
    }

    /** 检查表达式是否为简单的MATCH_ANY */
    private static boolean isSimpleMatchAny(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        // 匹配形如 "message MATCH_ANY 'keyword'" 的简单表达式
        return expression.matches("^message\\s+MATCH_ANY\\s+'[^']+'$");
    }

    /** 去除表达式外层不必要的括号 */
    private static String removeOuterParentheses(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return expression;
        }
        expression = expression.trim();
        if (expression.startsWith("(") && expression.endsWith(")")) {
            // 检查是否是完整的外层括号（而不是表达式中间的括号）
            int parenCount = 0;
            for (int i = 0; i < expression.length(); i++) {
                if (expression.charAt(i) == '(') parenCount++;
                else if (expression.charAt(i) == ')') parenCount--;

                // 如果在中间某处括号计数为0，说明不是完整的外层括号
                if (parenCount == 0 && i < expression.length() - 1) {
                    return expression;
                }
            }
            // 如果是完整的外层括号，去除它们
            return expression.substring(1, expression.length() - 1).trim();
        }
        return expression;
    }

    /** 从简单的MATCH_ANY表达式中提取关键字 */
    private static String extractKeywordsFromExpressions(String... expressions) {
        List<String> keywords = new ArrayList<>();
        for (String expr : expressions) {
            if (expr.contains("MATCH_ANY")) {
                // 提取MATCH_ANY中的关键字
                Matcher matcher = Pattern.compile("MATCH_ANY\\s+'([^']+)'").matcher(expr);
                if (matcher.find()) {
                    keywords.add(matcher.group(1));
                }
            } else if (expr.contains("MATCH_ALL")) {
                // 提取MATCH_ALL中的关键字
                Matcher matcher = Pattern.compile("MATCH_ALL\\s+'([^']+)'").matcher(expr);
                if (matcher.find()) {
                    keywords.addAll(List.of(matcher.group(1).split("\\s+")));
                }
            } else {
                // 假设是单个关键字
                keywords.add(expr);
            }
        }
        return String.join(" ", keywords);
    }

    /** 表达式验证结果类 */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }

    /** 解析结果类 */
    public static class ParseResult {
        private final boolean success;
        private final String result;
        private final String errorMessage;

        private ParseResult(boolean success, String result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResult() {
            return result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ParseResult success(String result) {
            return new ParseResult(true, result, null);
        }

        public static ParseResult error(String errorMessage) {
            return new ParseResult(false, null, errorMessage);
        }
    }
}
