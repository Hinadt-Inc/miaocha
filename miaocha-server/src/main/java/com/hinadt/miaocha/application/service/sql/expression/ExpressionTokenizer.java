package com.hinadt.miaocha.application.service.sql.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式词法分析器
 *
 * <p>将字符串表达式分解为Token序列，为后续的语法分析做准备
 *
 * <p>支持的语法元素：
 *
 * <ul>
 *   <li>词项：普通单词或引号包围的字符串
 *   <li>操作符：- (NOT), && (AND), || (OR)
 *   <li>括号：( )
 *   <li>空格：用于分隔，但会被忽略
 * </ul>
 */
public class ExpressionTokenizer {

    private final String input;
    private int position;

    public ExpressionTokenizer(String input) {
        this.input = input != null ? input.trim() : "";
        this.position = 0;
    }

    /**
     * 将输入字符串分解为Token序列
     *
     * @return Token列表，总是以EOF结尾
     */
    public List<ExpressionToken> tokenize() {
        List<ExpressionToken> tokens = new ArrayList<>();

        while (position < input.length()) {
            skipWhitespace();

            if (position >= input.length()) {
                break;
            }

            ExpressionToken token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        tokens.add(ExpressionToken.eof(position));
        return tokens;
    }

    private ExpressionToken nextToken() {
        char current = input.charAt(position);

        return switch (current) {
            case '(' -> {
                position++;
                yield ExpressionToken.leftParen(position - 1);
            }
            case ')' -> {
                position++;
                yield ExpressionToken.rightParen(position - 1);
            }
            case '\'' -> parseQuotedString();
            case '-' -> parseNotOperatorOrTerm();
            case '&' -> parseAndOperator();
            case '|' -> parseOrOperator();
            default -> parseTerm();
        };
    }

    private ExpressionToken parseQuotedString() {
        int start = position;
        position++; // 跳过开始的引号

        StringBuilder content = new StringBuilder();
        while (position < input.length() && input.charAt(position) != '\'') {
            content.append(input.charAt(position));
            position++;
        }

        if (position < input.length()) {
            position++; // 跳过结束的引号
        }

        return ExpressionToken.term(content.toString(), start);
    }

    private ExpressionToken parseNotOperatorOrTerm() {
        int start = position;

        // 检查是否是NOT操作符：减号后面跟空格或词项开始
        if (isNotOperator()) {
            position++;
            return ExpressionToken.not(start);
        }

        // 否则当作普通词项处理
        return parseTerm();
    }

    private boolean isNotOperator() {
        // NOT操作符的条件：
        // 1. 后面必须有有效内容（空格、左括号、字母或引号）
        // 2. 在行首，或者前面是空格或左括号

        // 首先检查后面是否有有效内容
        if (position + 1 >= input.length()) {
            return false; // 后面没有内容，不能是NOT操作符
        }

        char next = input.charAt(position + 1);
        if (!(next == ' ' || next == '(' || Character.isLetterOrDigit(next) || next == '\'')) {
            return false; // 后面不是有效的内容
        }

        // 检查位置条件
        if (position == 0) {
            return true; // 在行首且后面有有效内容
        }

        char prev = input.charAt(position - 1);
        return prev == ' ' || prev == '('; // 前面是空格或左括号
    }

    private ExpressionToken parseAndOperator() {
        if (position + 1 < input.length() && input.charAt(position + 1) == '&') {
            int start = position;
            position += 2;
            return ExpressionToken.and(start);
        }

        // 单个&当作普通字符处理
        return parseTerm();
    }

    private ExpressionToken parseOrOperator() {
        if (position + 1 < input.length() && input.charAt(position + 1) == '|') {
            int start = position;
            position += 2;
            return ExpressionToken.or(start);
        }

        // 单个|当作普通字符处理
        return parseTerm();
    }

    private ExpressionToken parseTerm() {
        int start = position;
        StringBuilder term = new StringBuilder();

        while (position < input.length()) {
            char c = input.charAt(position);

            // 遇到特殊字符或空格则停止
            if (c == ' ' || c == '(' || c == ')' || c == '\'' || isOperatorStart(c)) {
                break;
            }

            term.append(c);
            position++;
        }

        String termValue = term.toString().trim();
        return termValue.isEmpty() ? null : ExpressionToken.term(termValue, start);
    }

    private boolean isOperatorStart(char c) {
        if (c == '-') {
            // 如果是减号，需要判断是否是NOT操作符
            int savedPosition = position;
            boolean isNot = isNotOperator();
            position = savedPosition;
            return isNot;
        }
        return c == '&' || c == '|';
    }

    private void skipWhitespace() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
    }
}
