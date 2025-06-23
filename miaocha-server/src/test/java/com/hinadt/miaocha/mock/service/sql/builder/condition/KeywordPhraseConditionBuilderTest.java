package com.hinadt.miaocha.mock.service.sql.builder.condition;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.builder.condition.KeywordPhraseConditionBuilder;
import com.hinadt.miaocha.common.constants.FieldConstants;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import io.qameta.allure.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * KeywordPhraseConditionBuilder 单元测试类
 *
 * <p>测试基于MATCH_PHRASE的关键字查询构建器功能 验证MATCH_PHRASE查询语句的正确生成和表达式解析逻辑
 *
 * <p>测试目标：验证MATCH_PHRASE条件构建器的行为
 *
 * <p>支持的表达式类型： 1. 单个关键字：error -> message MATCH_PHRASE 'error' 2. 带引号的关键字：'database error' ->
 * message MATCH_PHRASE 'database error' 3. OR表达式：'error' || 'warning' -> ( message MATCH_PHRASE
 * 'error' OR message MATCH_PHRASE 'warning' ) 4. AND表达式：'error' && 'critical' -> ( message
 * MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical' ) 5. 复杂表达式：('error' || 'warning') &&
 * 'critical' -> ( ( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message
 * MATCH_PHRASE 'critical' )
 */
@Epic("秒查系统")
@Feature("SQL条件构建")
@Story("关键字MATCH_PHRASE条件构建")
@DisplayName("KeywordPhraseConditionBuilder测试")
public class KeywordPhraseConditionBuilderTest {

    private KeywordPhraseConditionBuilder builder;

    @BeforeEach
    public void setUp() {
        builder = new KeywordPhraseConditionBuilder();
    }

    // ==================== supports() 方法测试 ====================

    @Test
    @DisplayName("supports() - 空列表 - 应返回false")
    public void testSupportsEmptyList() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.emptyList());
        assertFalse(builder.supports(dto), "空的关键字列表不应该被支持");
    }

    @Test
    @DisplayName("supports() - null列表 - 应返回false")
    public void testSupportsNullList() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(null);
        assertFalse(builder.supports(dto), "null的关键字列表不应该被支持");
    }

    @Test
    @DisplayName("supports() - 全空白字符串 - 应返回false")
    public void testSupportsAllBlankStrings() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("", "   ", "\t", "\n", null));
        assertFalse(builder.supports(dto), "只包含空白字符串和null的列表不应该被支持");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "error",
                "'database error'",
                "error || warning",
                "error && critical",
                "('error' || 'warning') && 'critical'",
                "复杂的中文关键字",
                "'包含 空格 的 关键字'",
                "special-chars_123"
            })
    @DisplayName("supports() - 有效关键字 - 应返回true")
    public void testSupportsValidKeywords(String keyword) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(keyword));
        assertTrue(builder.supports(dto), "有效关键字应该被支持: " + keyword);
    }

    @Test
    @DisplayName("supports() - 混合有效和无效关键字 - 应返回true")
    public void testSupportsMixedKeywords() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("", "error", "   ", null, "warning"));
        assertTrue(builder.supports(dto), "包含有效关键字的混合列表应该被支持");
    }

    // ==================== buildCondition() 单个关键字测试 ====================

    @ParameterizedTest
    @MethodSource("singleKeywordTestCases")
    @DisplayName("buildCondition() - 单个关键字测试")
    public void testBuildConditionSingleKeyword(String input, String expected, String description) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(input));
        assertEquals(expected, builder.buildCondition(dto), description);
    }

    /** 单个关键字测试用例 */
    static Stream<Arguments> singleKeywordTestCases() {
        String field = FieldConstants.MESSAGE_FIELD;
        return Stream.of(
                Arguments.of("error", "(" + field + " MATCH_PHRASE 'error')", "简单关键字"),
                Arguments.of(
                        "'database error'",
                        "(" + field + " MATCH_PHRASE 'database error')",
                        "带引号的关键字"),
                Arguments.of("复杂的中文关键字", "(" + field + " MATCH_PHRASE '复杂的中文关键字')", "中文关键字"),
                Arguments.of(
                        "special-chars_123",
                        "(" + field + " MATCH_PHRASE 'special-chars_123')",
                        "特殊字符关键字"),
                Arguments.of(
                        "'包含 空格 的 关键字'", "(" + field + " MATCH_PHRASE '包含 空格 的 关键字')", "包含空格的关键字"),
                Arguments.of(
                        "'关键字with_特殊字符123'",
                        "(" + field + " MATCH_PHRASE '关键字with_特殊字符123')",
                        "混合字符关键字"),
                Arguments.of(
                        "'user@domain.com'",
                        "(" + field + " MATCH_PHRASE 'user@domain.com')",
                        "邮箱格式关键字"),
                Arguments.of(
                        "'192.168.1.1'", "(" + field + " MATCH_PHRASE '192.168.1.1')", "IP地址格式关键字"),
                Arguments.of(
                        "'2023-12-25'", "(" + field + " MATCH_PHRASE '2023-12-25')", "日期格式关键字"),
                Arguments.of("'HTTP/1.1'", "(" + field + " MATCH_PHRASE 'HTTP/1.1')", "协议格式关键字"),
                Arguments.of(
                        "'file.log.2023-12-25'",
                        "(" + field + " MATCH_PHRASE 'file.log.2023-12-25')",
                        "文件名格式关键字"));
    }

    // ==================== buildCondition() OR表达式测试 ====================

    @ParameterizedTest
    @MethodSource("orExpressionTestCases")
    @DisplayName("buildCondition() - OR表达式测试")
    public void testBuildConditionOrExpression(String input, String expected, String description) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(input));
        assertEquals(expected, builder.buildCondition(dto), description);
    }

    /** OR表达式测试用例 */
    static Stream<Arguments> orExpressionTestCases() {
        String field = FieldConstants.MESSAGE_FIELD;
        return Stream.of(
                Arguments.of(
                        "'error' || 'warning'",
                        "("
                                + field
                                + " MATCH_PHRASE 'error' OR "
                                + field
                                + " MATCH_PHRASE 'warning')",
                        "简单OR表达式"),
                Arguments.of(
                        "error || timeout || failure",
                        "("
                                + field
                                + " MATCH_PHRASE 'error' OR "
                                + field
                                + " MATCH_PHRASE 'timeout' OR "
                                + field
                                + " MATCH_PHRASE 'failure')",
                        "多项OR表达式"),
                Arguments.of(
                        "'database error' || 'connection timeout'",
                        "("
                                + field
                                + " MATCH_PHRASE 'database error' OR "
                                + field
                                + " MATCH_PHRASE 'connection timeout')",
                        "复杂短语OR表达式"),
                Arguments.of(
                        "'用户' || '系统' || '服务'",
                        "("
                                + field
                                + " MATCH_PHRASE '用户' OR "
                                + field
                                + " MATCH_PHRASE '系统' OR "
                                + field
                                + " MATCH_PHRASE '服务')",
                        "中文OR表达式"),
                Arguments.of(
                        "'ERROR' || 'WARNING' || 'INFO'",
                        "("
                                + field
                                + " MATCH_PHRASE 'ERROR' OR "
                                + field
                                + " MATCH_PHRASE 'WARNING' OR "
                                + field
                                + " MATCH_PHRASE 'INFO')",
                        "日志级别OR表达式"));
    }

    // ==================== buildCondition() AND表达式测试 ====================

    @ParameterizedTest
    @MethodSource("andExpressionTestCases")
    @DisplayName("buildCondition() - AND表达式测试")
    public void testBuildConditionAndExpression(String input, String expected, String description) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(input));
        assertEquals(expected, builder.buildCondition(dto), description);
    }

    /** AND表达式测试用例 */
    static Stream<Arguments> andExpressionTestCases() {
        String field = FieldConstants.MESSAGE_FIELD;
        return Stream.of(
                Arguments.of(
                        "'error' && 'critical'",
                        "("
                                + field
                                + " MATCH_PHRASE 'error' AND "
                                + field
                                + " MATCH_PHRASE 'critical')",
                        "简单AND表达式"),
                Arguments.of(
                        "database && connection && timeout",
                        "("
                                + field
                                + " MATCH_PHRASE 'database' AND "
                                + field
                                + " MATCH_PHRASE 'connection' AND "
                                + field
                                + " MATCH_PHRASE 'timeout')",
                        "多项AND表达式"),
                Arguments.of(
                        "'service unavailable' && 'retry failed'",
                        "("
                                + field
                                + " MATCH_PHRASE 'service unavailable' AND "
                                + field
                                + " MATCH_PHRASE 'retry failed')",
                        "复杂短语AND表达式"),
                Arguments.of(
                        "'用户' && '认证' && '失败'",
                        "("
                                + field
                                + " MATCH_PHRASE '用户' AND "
                                + field
                                + " MATCH_PHRASE '认证' AND "
                                + field
                                + " MATCH_PHRASE '失败')",
                        "中文AND表达式"));
    }

    // ==================== buildCondition() 复杂表达式测试 ====================

    @ParameterizedTest
    @MethodSource("complexExpressionTestCases")
    @DisplayName("buildCondition() - 复杂表达式测试")
    public void testBuildConditionComplexExpression(
            String input, String expected, String description) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(input));
        assertEquals(expected, builder.buildCondition(dto), description);
    }

    /** 复杂表达式测试用例 */
    static Stream<Arguments> complexExpressionTestCases() {
        String field = FieldConstants.MESSAGE_FIELD;
        return Stream.of(
                Arguments.of(
                        "('error' || 'warning') && 'critical'",
                        "("
                                + "( "
                                + field
                                + " MATCH_PHRASE 'error' OR "
                                + field
                                + " MATCH_PHRASE 'warning' ) AND "
                                + field
                                + " MATCH_PHRASE 'critical')",
                        "括号OR与AND组合"),
                Arguments.of(
                        "'urgent' && ('timeout' || 'failure')",
                        "("
                                + field
                                + " MATCH_PHRASE 'urgent' AND ( "
                                + field
                                + " MATCH_PHRASE 'timeout' OR "
                                + field
                                + " MATCH_PHRASE 'failure' ))",
                        "AND与括号OR组合"),
                Arguments.of(
                        "('database' || 'connection') && ('error' || 'exception')",
                        "("
                                + "( "
                                + field
                                + " MATCH_PHRASE 'database' OR "
                                + field
                                + " MATCH_PHRASE 'connection' ) AND ( "
                                + field
                                + " MATCH_PHRASE 'error' OR "
                                + field
                                + " MATCH_PHRASE 'exception' ))",
                        "双括号OR与AND组合"),
                Arguments.of(
                        "'error' || 'warning' && 'critical'",
                        "("
                                + field
                                + " MATCH_PHRASE 'error' OR "
                                + field
                                + " MATCH_PHRASE 'warning' AND "
                                + field
                                + " MATCH_PHRASE 'critical')",
                        "OR与AND混合（AND优先级更高）"));
    }

    // ==================== buildCondition() 多个关键字表达式测试 ====================

    @Test
    @DisplayName("buildCondition() - 多个简单关键字")
    public void testBuildConditionMultipleSimpleKeywords() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "timeout", "critical"));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        String expected =
                "("
                        + field
                        + " MATCH_PHRASE 'error' AND "
                        + field
                        + " MATCH_PHRASE 'timeout' AND "
                        + field
                        + " MATCH_PHRASE 'critical')";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("buildCondition() - 混合类型关键字表达式")
    public void testBuildConditionMixedExpressions() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "error", // 简单关键字
                        "'warning' || 'critical'", // OR表达式
                        "'timeout' && 'failure'", // AND表达式
                        "('service' || 'api') && 'down'" // 复杂表达式
                        ));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        // 验证包含各种类型的条件
        assertAll(
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'error'"), "应包含简单关键字"),
                () ->
                        assertTrue(
                                result.contains(
                                        field
                                                + " MATCH_PHRASE 'warning' OR "
                                                + field
                                                + " MATCH_PHRASE 'critical'"),
                                "应包含OR表达式"),
                () ->
                        assertTrue(
                                result.contains(
                                        field
                                                + " MATCH_PHRASE 'timeout' AND "
                                                + field
                                                + " MATCH_PHRASE 'failure'"),
                                "应包含AND表达式"),
                () ->
                        assertTrue(
                                result.contains(
                                        "( "
                                                + field
                                                + " MATCH_PHRASE 'service' OR "
                                                + field
                                                + " MATCH_PHRASE 'api' ) AND "
                                                + field
                                                + " MATCH_PHRASE 'down'"),
                                "应包含复杂表达式"),
                () -> assertTrue(countOccurrences(result, " AND ") >= 3, "应至少有3个AND连接符"));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("buildCondition() - 空白关键字过滤")
    public void testBuildConditionFilterBlankKeywords() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "", "   ", null, "timeout", "\t\n"));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        String expected =
                "(" + field + " MATCH_PHRASE 'error' AND " + field + " MATCH_PHRASE 'timeout')";
        assertEquals(expected, result, "应过滤掉空白关键字");
    }

    @Test
    @DisplayName("buildCondition() - 空引号处理")
    public void testBuildConditionEmptyQuotes() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("''", "error", "''"));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        assertEquals("(" + field + " MATCH_PHRASE 'error')", result, "应忽略空引号");
    }

    @Test
    @DisplayName("buildCondition() - 仅空引号")
    public void testBuildConditionOnlyEmptyQuotes() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("''"));

        String result = builder.buildCondition(dto);
        assertEquals("", result, "仅空引号应返回空字符串");
    }

    @Test
    @DisplayName("buildCondition() - 复杂空格处理")
    public void testBuildConditionComplexSpaceHandling() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "  error  ",
                        " 'database   error' ",
                        "  error   ||   warning  ",
                        " (  error || warning  )  &&  critical "));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        assertAll(
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'error'"), "应正确处理空格"),
                () ->
                        assertTrue(
                                result.contains(field + " MATCH_PHRASE 'database   error'"),
                                "应保留引号内空格"),
                () -> assertTrue(result.contains(" OR "), "应有正确的OR操作符"),
                () -> assertTrue(result.contains(" AND "), "应有正确的AND操作符"),
                () -> assertTrue(result.contains("( "), "应有正确的左括号"),
                () -> assertTrue(result.contains(" )"), "应有正确的右括号"));
    }

    // ==================== 特殊字符测试 ====================

    @ParameterizedTest
    @ValueSource(
            strings = {
                "'error-404'",
                "'user_service'",
                "'api.v2.error'",
                "'service@domain.com'",
                "'version-2.0.1-SNAPSHOT'",
                "'[ERROR] System failure'",
                "'HTTP/1.1 500 Internal Server Error'"
            })
    @DisplayName("buildCondition() - 特殊字符处理")
    public void testBuildConditionSpecialCharacters(String keyword) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(keyword));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        String extractedKeyword = keyword.substring(1, keyword.length() - 1); // 去除引号
        String expected = "(" + field + " MATCH_PHRASE '" + extractedKeyword + "')";
        assertEquals(expected, result, "应正确处理特殊字符: " + keyword);
    }

    // ==================== Unicode字符测试 ====================

    @ParameterizedTest
    @ValueSource(
            strings = {
                "'用户服务异常'",
                "'データベースエラー'",
                "'서비스 오류'",
                "'Ошибка системы'",
                "'🚨 系统告警 🚨'",
                "'用户-服务_v2.0'",
                "'混合English中文123'"
            })
    @DisplayName("buildCondition() - Unicode字符支持")
    public void testBuildConditionUnicodeCharacters(String keyword) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList(keyword));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        String extractedKeyword = keyword.substring(1, keyword.length() - 1);
        String expected = "(" + field + " MATCH_PHRASE '" + extractedKeyword + "')";
        assertEquals(expected, result, "应正确处理Unicode字符: " + keyword);
    }

    // ==================== 性能测试 ====================

    @Test
    @DisplayName("buildCondition() - 大量关键字性能测试")
    public void testBuildConditionPerformance() {
        LogSearchDTO dto = new LogSearchDTO();

        // 创建100个关键字
        List<String> keywords = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            keywords.add("error" + i);
        }

        dto.setKeywords(keywords);

        long startTime = System.currentTimeMillis();
        String result = builder.buildCondition(dto);
        long endTime = System.currentTimeMillis();

        assertAll(
                () -> assertNotNull(result, "结果不应为null"),
                () -> assertFalse(result.isEmpty(), "结果不应为空"),
                () -> assertTrue(endTime - startTime < 1000, "处理100个关键字应在1秒内完成"));
    }

    // ==================== 递归深度测试 ====================

    @Test
    @DisplayName("buildCondition() - 深度嵌套表达式")
    public void testBuildConditionDeepNesting() {
        LogSearchDTO dto = new LogSearchDTO();
        String deepExpression = "((('error' || 'warning') && 'critical') || 'timeout') && 'final'";
        dto.setKeywords(Collections.singletonList(deepExpression));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        assertAll(
                () -> assertNotNull(result, "深度嵌套表达式应能正确解析"),
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'error'"), "应包含error"),
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'warning'"), "应包含warning"),
                () ->
                        assertTrue(
                                result.contains(field + " MATCH_PHRASE 'critical'"), "应包含critical"),
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'timeout'"), "应包含timeout"),
                () -> assertTrue(result.contains(field + " MATCH_PHRASE 'final'"), "应包含final"),
                () -> assertTrue(result.contains(" OR "), "应包含OR操作符"),
                () -> assertTrue(result.contains(" AND "), "应包含AND操作符"),
                () -> assertTrue(result.contains("( "), "应包含左括号"),
                () -> assertTrue(result.contains(" )"), "应包含右括号"));
    }

    // ==================== 操作符优先级测试 ====================

    @Test
    @DisplayName("buildCondition() - 操作符优先级")
    public void testBuildConditionOperatorPrecedence() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'error' || 'warning' && 'critical'"));

        String result = builder.buildCondition(dto);
        String field = FieldConstants.MESSAGE_FIELD;

        // AND优先级高于OR，所以应该是 error || (warning && critical)
        String expected =
                "("
                        + field
                        + " MATCH_PHRASE 'error' OR "
                        + field
                        + " MATCH_PHRASE 'warning' AND "
                        + field
                        + " MATCH_PHRASE 'critical')";
        assertEquals(expected, result, "应正确处理操作符优先级");
    }

    // ==================== 辅助方法 ====================

    /** 计算字符串中子字符串出现的次数 */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    // ==================== 不支持情况的验证测试 ====================

    @Test
    @DisplayName("supports() - 不支持null DTO")
    public void testSupportsNullDTO() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    builder.supports(null);
                },
                "null DTO应该抛出NullPointerException");
    }

    @Test
    @DisplayName("buildCondition() - 处理null DTO")
    public void testBuildConditionNullDTO() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    builder.buildCondition(null);
                },
                "null DTO应该抛出NullPointerException");
    }

    @Test
    @DisplayName("buildCondition() - 处理不支持的DTO")
    public void testBuildConditionUnsupportedDTO() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.emptyList());

        assertEquals("", builder.buildCondition(dto), "不支持的DTO应返回空字符串");
    }
}
