package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 搜索方法枚举测试
 *
 * <p>验证SearchMethod枚举的基本功能和SQL生成逻辑
 */
@DisplayName("搜索方法枚举测试")
class SearchMethodTest {

    @ParameterizedTest
    @DisplayName("搜索方法字符串转换测试 - 验证fromString方法")
    @CsvSource({
        "LIKE, LIKE",
        "like, LIKE",
        "MATCH_PHRASE, MATCH_PHRASE",
        "match_phrase, MATCH_PHRASE",
        "MATCH_ANY, MATCH_ANY",
        "MATCH_ALL, MATCH_ALL"
    })
    void testFromString(String input, String expectedMethodName) {
        SearchMethod method = SearchMethod.fromString(input);
        assertEquals(expectedMethodName, method.getMethodName());
    }

    @Test
    @DisplayName("不支持的搜索方法测试 - 验证异常抛出")
    void testUnsupportedSearchMethod() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> SearchMethod.fromString("UNSUPPORTED"));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @ParameterizedTest
    @DisplayName("单个条件SQL生成测试 - 验证各搜索方法的SQL语法")
    @CsvSource({
        "LIKE, message, error, 'message LIKE ''%error%'''",
        "MATCH_PHRASE, content, 'java exception', 'content MATCH_PHRASE ''java exception'''",
        "MATCH_ANY, tags, critical, 'tags MATCH_ANY ''critical'''",
        "MATCH_ALL, keywords, payment, 'keywords MATCH_ALL ''payment'''"
    })
    void testBuildSingleCondition(
            String methodName, String fieldName, String keyword, String expected) {
        SearchMethod method = SearchMethod.fromString(methodName);
        String result = method.buildSingleCondition(fieldName, keyword);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("复杂表达式解析测试 - 验证parseExpression方法")
    void testParseExpression() {
        SearchMethod method = SearchMethod.LIKE;

        // 简单表达式
        String result1 = method.parseExpression("message", "'error'");
        assertEquals("message LIKE '%error%'", result1);

        // OR表达式
        String result2 = method.parseExpression("message", "'error' || 'warning'");
        assertEquals("message LIKE '%error%' OR message LIKE '%warning%'", result2);

        // AND表达式
        String result3 = method.parseExpression("message", "'java' && 'exception'");
        assertEquals("message LIKE '%java%' AND message LIKE '%exception%'", result3);
    }

    @Test
    @DisplayName("空表达式处理测试 - 验证边界条件")
    void testEmptyExpression() {
        SearchMethod method = SearchMethod.LIKE;

        String result1 = method.parseExpression("message", "");
        assertEquals("", result1);

        String result2 = method.parseExpression("message", null);
        assertEquals("", result2);

        String result3 = method.parseExpression("message", "   ");
        assertEquals("", result3);
    }
}
