package com.hina.log.exception;

/**
 * 关键字语法异常
 * 用于表示关键字表达式解析时出现的语法错误
 */
public class KeywordSyntaxException extends RuntimeException {

    private final String expression;

    public KeywordSyntaxException(String message, String expression) {
        super(message);
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }
}