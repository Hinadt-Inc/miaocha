package com.hinadt.miaocha.application.service.sql.expression;

import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 字段表达式解析器 - 重构版本
 *
 * <p>采用现代化的设计模式：
 *
 * <ul>
 *   <li>词法分析与语法分析分离
 *   <li>递归下降解析器
 *   <li>清晰的操作符优先级处理
 *   <li>优雅的错误处理
 * </ul>
 *
 * <p>操作符优先级（从高到低）：
 *
 * <ol>
 *   <li>括号表达式 ()
 *   <li>NOT操作符 (-)
 *   <li>AND操作符 (&&)
 *   <li>OR操作符 (||)
 * </ol>
 */
public class FieldExpressionParser {

    private final String fieldName;
    private final SearchMethod searchMethod;
    private List<ExpressionToken> tokens;
    private int currentTokenIndex;

    public FieldExpressionParser(String fieldName, SearchMethod searchMethod) {
        this.fieldName = fieldName;
        this.searchMethod = searchMethod;
    }

    /**
     * 解析关键字表达式为SQL条件
     *
     * @param expression 输入表达式
     * @return SQL条件字符串
     */
    public String parseKeywordExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }

        // 词法分析：将字符串分解为Token序列
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(expression);
        this.tokens = tokenizer.tokenize();
        this.currentTokenIndex = 0;

        // 语法分析：递归下降解析
        String result = parseOrExpression();
        return result != null ? result : "";
    }

    /** 解析OR表达式（最低优先级） 语法：AND_EXPR ('||' AND_EXPR)* */
    private String parseOrExpression() {
        String left = parseAndExpression();
        if (left == null) {
            return null;
        }

        while (currentToken().type() == ExpressionToken.TokenType.OR) {
            consumeToken(); // 消费 ||
            String right = parseAndExpression();
            if (right == null) {
                return left; // 忽略右侧无效表达式
            }
            left = left + " OR " + right;
        }

        return left;
    }

    /** 解析AND表达式 语法：NOT_EXPR ('&&' NOT_EXPR)* */
    private String parseAndExpression() {
        String left = parseNotExpression();
        if (left == null) {
            return null;
        }

        while (currentToken().type() == ExpressionToken.TokenType.AND) {
            consumeToken(); // 消费 &&
            String right = parseNotExpression();
            if (right == null) {
                return left; // 忽略右侧无效表达式
            }
            left = left + " AND " + right;
        }

        return left;
    }

    /** 解析NOT表达式（最高优先级，除了括号） 语法：'-' PRIMARY_EXPR | PRIMARY_EXPR */
    private String parseNotExpression() {
        if (currentToken().type() == ExpressionToken.TokenType.NOT) {
            consumeToken(); // 消费 -
            String operand = parsePrimaryExpression();
            if (operand == null) {
                return null; // NOT后面没有有效操作数
            }

            // 智能处理NOT操作数
            return buildNotExpression(operand);
        }

        return parsePrimaryExpression();
    }

    /** 构建NOT表达式，智能处理不同类型的操作数 */
    private String buildNotExpression(String operand) {
        if (operand == null || operand.trim().isEmpty()) {
            return null;
        }

        // 如果操作数已经有括号，直接在前面加NOT
        if (operand.startsWith("( ") && operand.endsWith(" )")) {
            return "NOT " + operand;
        }

        // 对于复杂表达式，用括号包围然后加NOT
        if (FieldExpressionParser.containsLogicalOperators(operand)) {
            return "NOT (" + operand + ")";
        }

        // 简单表达式直接加NOT
        return "NOT " + operand;
    }

    /** 检查OR表达式是否所有项都是负向的 */
    private boolean checkOrExpressionAllNegative() {
        if (!checkAndExpressionAllNegative()) {
            return false;
        }

        while (currentToken().type() == ExpressionToken.TokenType.OR) {
            consumeToken(); // 消费 ||
            if (!checkAndExpressionAllNegative()) {
                return false;
            }
        }
        return true;
    }

    /** 检查AND表达式是否所有项都是负向的 */
    private boolean checkAndExpressionAllNegative() {
        if (!checkNotExpressionIsNegative()) {
            return false;
        }

        while (currentToken().type() == ExpressionToken.TokenType.AND) {
            consumeToken(); // 消费 &&
            if (!checkNotExpressionIsNegative()) {
                return false;
            }
        }
        return true;
    }

    /** 检查NOT表达式是否是负向的 */
    private boolean checkNotExpressionIsNegative() {
        if (currentToken().type() == ExpressionToken.TokenType.NOT) {
            return true; // 有NOT前缀，是负向的
        }

        // 检查基本表达式
        return checkPrimaryExpressionIsNegative();
    }

    /** 检查基本表达式是否是负向的 */
    private boolean checkPrimaryExpressionIsNegative() {
        ExpressionToken token = currentToken();

        if (token.type() == ExpressionToken.TokenType.LEFT_PAREN) {
            consumeToken(); // 消费 (
            boolean result = checkOrExpressionAllNegative();
            if (currentToken().type() == ExpressionToken.TokenType.RIGHT_PAREN) {
                consumeToken(); // 消费 )
            }
            return result;
        } else if (token.type() == ExpressionToken.TokenType.TERM) {
            consumeToken(); // 消费term
            return false; // 普通term不是负向的
        }

        return false;
    }

    /** 从OR表达式中提取负向关键词 */
    private void extractNegativeTermsFromOrExpression(List<String> negativeTerms) {
        extractNegativeTermsFromAndExpression(negativeTerms);

        while (currentToken().type() == ExpressionToken.TokenType.OR) {
            consumeToken(); // 消费 ||
            extractNegativeTermsFromAndExpression(negativeTerms);
        }
    }

    /** 从AND表达式中提取负向关键词 */
    private void extractNegativeTermsFromAndExpression(List<String> negativeTerms) {
        extractNegativeTermsFromNotExpression(negativeTerms);

        while (currentToken().type() == ExpressionToken.TokenType.AND) {
            consumeToken(); // 消费 &&
            extractNegativeTermsFromNotExpression(negativeTerms);
        }
    }

    /** 从NOT表达式中提取负向关键词 */
    private void extractNegativeTermsFromNotExpression(List<String> negativeTerms) {
        if (currentToken().type() == ExpressionToken.TokenType.NOT) {
            consumeToken(); // 消费 -
            extractNegativeTermsFromPrimaryExpression(negativeTerms);
        } else {
            extractNegativeTermsFromPrimaryExpression(negativeTerms); // 跳过正向条件
        }
    }

    /** 从基本表达式中提取负向关键词 */
    private void extractNegativeTermsFromPrimaryExpression(List<String> negativeTerms) {
        ExpressionToken token = currentToken();

        if (token.type() == ExpressionToken.TokenType.LEFT_PAREN) {
            consumeToken(); // 消费 (
            extractNegativeTermsFromOrExpression(negativeTerms);
            if (currentToken().type() == ExpressionToken.TokenType.RIGHT_PAREN) {
                consumeToken(); // 消费 )
            }
        } else if (token.type() == ExpressionToken.TokenType.TERM) {
            // 这里需要检查上下文，如果前面有NOT，则添加到负向列表
            // 由于在extractNegativeTermsFromNotExpression中已经检查了NOT，这里直接添加
            String term = token.value().replace("'", ""); // 去掉引号
            if (!negativeTerms.contains(term)) {
                negativeTerms.add(term);
            }
            consumeToken(); // 消费term
        }
    }

    /** 从OR表达式中分离正向和负向条件 */
    private void separateTermsFromOrExpression(
            List<String> positiveTerms, List<String> negativeTerms) {
        separateTermsFromAndExpression(positiveTerms, negativeTerms);

        while (currentToken().type() == ExpressionToken.TokenType.OR) {
            consumeToken(); // 消费 ||
            separateTermsFromAndExpression(positiveTerms, negativeTerms);
        }
    }

    /** 从AND表达式中分离正向和负向条件 */
    private void separateTermsFromAndExpression(
            List<String> positiveTerms, List<String> negativeTerms) {
        separateTermsFromNotExpression(positiveTerms, negativeTerms);

        while (currentToken().type() == ExpressionToken.TokenType.AND) {
            consumeToken(); // 消费 &&
            separateTermsFromNotExpression(positiveTerms, negativeTerms);
        }
    }

    /** 从NOT表达式中分离正向和负向条件 */
    private void separateTermsFromNotExpression(
            List<String> positiveTerms, List<String> negativeTerms) {
        if (currentToken().type() == ExpressionToken.TokenType.NOT) {
            consumeToken(); // 消费 -
            separateTermsFromPrimaryExpression(positiveTerms, negativeTerms, true);
        } else {
            separateTermsFromPrimaryExpression(positiveTerms, negativeTerms, false);
        }
    }

    /** 从基本表达式中分离正向和负向条件 */
    private void separateTermsFromPrimaryExpression(
            List<String> positiveTerms, List<String> negativeTerms, boolean isNegative) {
        ExpressionToken token = currentToken();

        if (token.type() == ExpressionToken.TokenType.LEFT_PAREN) {
            consumeToken(); // 消费 (
            separateTermsFromOrExpression(positiveTerms, negativeTerms);
            if (currentToken().type() == ExpressionToken.TokenType.RIGHT_PAREN) {
                consumeToken(); // 消费 )
            }
        } else if (token.type() == ExpressionToken.TokenType.TERM) {
            String term = token.value().replace("'", ""); // 去掉引号
            if (isNegative) {
                if (!negativeTerms.contains(term)) {
                    negativeTerms.add(term);
                }
            } else {
                if (!positiveTerms.contains(term)) {
                    positiveTerms.add(term);
                }
            }
            consumeToken(); // 消费term
        }
    }

    /** 重置解析器状态 */
    private void reset() {
        this.currentTokenIndex = 0;
    }

    /** 解析基本表达式 语法：'(' OR_EXPR ')' | TERM */
    private String parsePrimaryExpression() {
        ExpressionToken token = currentToken();

        return switch (token.type()) {
            case LEFT_PAREN -> {
                consumeToken(); // 消费 (
                String inner = parseOrExpression();

                // 期望右括号
                if (currentToken().type() == ExpressionToken.TokenType.RIGHT_PAREN) {
                    consumeToken(); // 消费 )
                    yield inner != null ? "( " + inner + " )" : null;
                } else {
                    // 缺少右括号，但尽量返回已解析的内容
                    yield inner;
                }
            }
            case TERM -> {
                consumeToken();
                yield buildSqlCondition(token.value());
            }
            default -> null; // 无效的基本表达式
        };
    }

    /** 为单个词项构建SQL条件 */
    private String buildSqlCondition(String term) {
        if (term == null || term.trim().isEmpty()) {
            return null;
        }

        String trimmedTerm = term.trim();
        String escapedTerm = escapeSpecialCharacters(trimmedTerm);
        return searchMethod.buildSingleCondition(fieldName, escapedTerm);
    }

    /** 转义特殊字符防止SQL注入 */
    private String escapeSpecialCharacters(String input) {
        if (input == null) return "";
        return input.replace("'", "''").replace("\\", "\\\\");
    }

    /** 获取当前Token */
    private ExpressionToken currentToken() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        }
        return ExpressionToken.eof(0);
    }

    /** 消费当前Token，移动到下一个 */
    private void consumeToken() {
        if (currentTokenIndex < tokens.size()) {
            currentTokenIndex++;
        }
    }

    // ==================== 新增的表达式分析方法 ====================

    /** 检查表达式是否包含负向条件 使用词法分析器进行精确检查，避免误判普通连字符 */
    public static boolean containsNegativeTerms(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // 使用词法分析器解析表达式
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(expression);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        // 检查是否存在NOT类型的Token
        return tokens.stream().anyMatch(token -> token.type() == ExpressionToken.TokenType.NOT);
    }

    /** 检查表达式是否只包含负向条件 使用词法分析器进行精确检查 */
    public static boolean containsOnlyNegativeTerms(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // 使用词法分析器解析表达式
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(expression);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        boolean hasNegativeTerms = false;
        boolean hasPositiveTerms = false;

        for (int i = 0; i < tokens.size(); i++) {
            ExpressionToken token = tokens.get(i);

            if (token.type() == ExpressionToken.TokenType.NOT) {
                hasNegativeTerms = true;
                // 跳过下一个TERM token，因为它是被NOT修饰的
                if (i + 1 < tokens.size()
                        && tokens.get(i + 1).type() == ExpressionToken.TokenType.TERM) {
                    i++; // 跳过下一个token
                }
            } else if (token.type() == ExpressionToken.TokenType.TERM) {
                hasPositiveTerms = true;
            }
        }

        return hasNegativeTerms && !hasPositiveTerms;
    }

    /** 检查表达式是否包含逻辑操作符 */
    public static boolean containsLogicalOperators(String expression) {
        if (expression == null) return false;
        return expression.contains("||") || expression.contains("&&");
    }

    /** 分离混合表达式中的正向和负向部分 返回数组：[0] = 正向部分, [1] = 负向部分 */
    public static String[] separateMixedExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new String[] {"", ""};
        }

        // 智能分离逻辑：按 && 分割表达式
        StringBuilder positive = new StringBuilder();
        StringBuilder negative = new StringBuilder();

        String[] andParts = expression.split("\\s*&&\\s*");

        for (String part : andParts) {
            part = part.trim();

            if (isNegativePart(part)) {
                if (!negative.isEmpty()) {
                    negative.append(" && ");
                }
                negative.append(part);
            } else {
                if (!positive.isEmpty()) {
                    positive.append(" && ");
                }
                positive.append(part);
            }
        }

        return new String[] {positive.toString(), negative.toString()};
    }

    /** 判断一个表达式部分是否是负向的 */
    private static boolean isNegativePart(String part) {
        // 去掉括号检查内容
        String content = part.trim();
        if (content.startsWith("(") && content.endsWith(")")) {
            content = content.substring(1, content.length() - 1).trim();
        }

        // 检查是否包含负向条件
        String[] orParts = content.split("\\s*\\|\\|\\s*");
        boolean hasNegative = false;
        boolean hasPositive = false;

        for (String orPart : orParts) {
            orPart = orPart.trim();
            if (orPart.startsWith("-")) {
                hasNegative = true;
            } else {
                hasPositive = true;
            }
        }

        // 如果只有负向条件，认为是负向部分
        return hasNegative && !hasPositive;
    }

    /** 提取负向表达式中的所有关键词 */
    public static List<String> extractNegativeTermsFromExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> terms = new java.util.LinkedHashSet<>(); // 保持顺序

        // 使用词法分析器正确解析表达式
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(expression);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        for (int i = 0; i < tokens.size(); i++) {
            ExpressionToken token = tokens.get(i);

            // 找到NOT操作符，获取下一个TERM
            if (token.type() == ExpressionToken.TokenType.NOT && i + 1 < tokens.size()) {
                ExpressionToken nextToken = tokens.get(i + 1);
                if (nextToken.type() == ExpressionToken.TokenType.TERM) {
                    terms.add(nextToken.value()); // 这里的value已经是去掉引号的内容
                }
            }
        }

        return new ArrayList<>(terms);
    }

    /** 表达式分离结果 */
    public record ExpressionSeparationResult(
            List<String> positiveExpressions, List<String> negativeExpressions) {
        public ExpressionSeparationResult(
                List<String> positiveExpressions, List<String> negativeExpressions) {
            this.positiveExpressions =
                    positiveExpressions != null ? positiveExpressions : new ArrayList<>();
            this.negativeExpressions =
                    negativeExpressions != null ? negativeExpressions : new ArrayList<>();
        }

        public boolean hasPositiveExpressions() {
            return !positiveExpressions.isEmpty();
        }

        public boolean hasNegativeExpressions() {
            return !negativeExpressions.isEmpty();
        }
    }

    /** 分离关键字列表中的正向和负向表达式 */
    public static ExpressionSeparationResult separateKeywordExpressions(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ExpressionSeparationResult(new ArrayList<>(), new ArrayList<>());
        }

        List<String> positiveExpressions = new ArrayList<>();
        List<String> negativeExpressions = new ArrayList<>();

        for (String keyword : keywords) {
            if (containsOnlyNegativeTerms(keyword)) {
                negativeExpressions.add(keyword);
            } else if (containsNegativeTerms(keyword)) {
                // 包含混合条件的表达式，需要分离
                String[] separated = separateMixedExpression(keyword);
                if (!separated[0].isEmpty()) {
                    positiveExpressions.add(separated[0]);
                }
                if (!separated[1].isEmpty()) {
                    negativeExpressions.add(separated[1]);
                }
            } else {
                positiveExpressions.add(keyword);
            }
        }

        return new ExpressionSeparationResult(positiveExpressions, negativeExpressions);
    }
}
