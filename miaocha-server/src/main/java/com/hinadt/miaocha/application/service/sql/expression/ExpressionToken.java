package com.hinadt.miaocha.application.service.sql.expression;

/**
 * 表达式词法单元
 *
 * <p>将复杂的字符串表达式分解为结构化的Token序列，便于后续的语法分析
 */
public record ExpressionToken(TokenType type, String value, int position) {

    public enum TokenType {
        /** 关键字词项 */
        TERM,
        /** NOT操作符 (-) */
        NOT,
        /** AND操作符 (&&) */
        AND,
        /** OR操作符 (||) */
        OR,
        /** 左括号 */
        LEFT_PAREN,
        /** 右括号 */
        RIGHT_PAREN,
        /** 表达式结束 */
        EOF
    }

    public static ExpressionToken term(String value, int position) {
        return new ExpressionToken(TokenType.TERM, value, position);
    }

    public static ExpressionToken not(int position) {
        return new ExpressionToken(TokenType.NOT, "-", position);
    }

    public static ExpressionToken and(int position) {
        return new ExpressionToken(TokenType.AND, "&&", position);
    }

    public static ExpressionToken or(int position) {
        return new ExpressionToken(TokenType.OR, "||", position);
    }

    public static ExpressionToken leftParen(int position) {
        return new ExpressionToken(TokenType.LEFT_PAREN, "(", position);
    }

    public static ExpressionToken rightParen(int position) {
        return new ExpressionToken(TokenType.RIGHT_PAREN, ")", position);
    }

    public static ExpressionToken eof(int position) {
        return new ExpressionToken(TokenType.EOF, "", position);
    }
}
