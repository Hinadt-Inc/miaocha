package com.hinadt.miaocha.application.service.sql.search;

/** 搜索方法处理器接口 */
public interface SearchMethodHandler {

    /**
     * 解析字段表达式并生成SQL条件
     *
     * @param fieldName 字段名
     * @param expression 搜索表达式
     * @return SQL条件字符串
     */
    String parseExpression(String fieldName, String expression);

    /**
     * 判断是否支持指定的搜索方法
     *
     * @param searchMethod 搜索方法名称
     * @return 是否支持
     */
    boolean supports(String searchMethod);
}
