package com.hinadt.miaocha.application.service.sql.search;

import com.hinadt.miaocha.application.service.sql.builder.FieldExpressionParser;
import lombok.Getter;

/**
 * 搜索方法枚举
 *
 * <p>定义所有支持的搜索方法及其SQL生成逻辑，支持复杂表达式解析
 */
@Getter
public enum SearchMethod implements SearchMethodHandler {
    LIKE("LIKE") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " LIKE '%" + keyword + "%'";
        }
    },

    MATCH_PHRASE("MATCH_PHRASE") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_PHRASE '" + keyword + "'";
        }
    },

    MATCH_ANY("MATCH_ANY") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_ANY '" + keyword + "'";
        }
    },

    MATCH_ALL("MATCH_ALL") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_ALL '" + keyword + "'";
        }
    };

    private final String methodName;

    SearchMethod(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean supports(String searchMethod) {
        return this.methodName.equalsIgnoreCase(searchMethod);
    }

    @Override
    public String parseExpression(String fieldName, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "";
        }

        FieldExpressionParser parser = new FieldExpressionParser(fieldName, this);
        return parser.parseKeywordExpression(expression);
    }

    /**
     * 生成单个字段条件的SQL
     *
     * @param fieldName 字段名
     * @param keyword 已转义的关键字
     * @return SQL条件字符串
     */
    public abstract String buildSingleCondition(String fieldName, String keyword);

    /** 根据搜索方法名称获取对应的枚举 */
    public static SearchMethod fromString(String searchMethod) {
        for (SearchMethod method : values()) {
            if (method.supports(searchMethod)) {
                return method;
            }
        }
        throw new IllegalArgumentException("不支持的搜索方法: " + searchMethod);
    }
}
