package com.hina.log.service.sql.builder.condition;

import static org.junit.jupiter.api.Assertions.*;

import com.hina.log.application.service.sql.builder.condition.*;
import com.hina.log.domain.dto.LogSearchDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * SearchConditionBuilder集成测试类
 *
 * <p>这个测试类直接使用真实的条件构建器实现，不使用Mock 用于验证整个条件构建系统的实际行为
 *
 * <p>测试覆盖： 1. 各种关键字表达式的正确处理 2. 条件构建器的优先级和支持判断 3. SQL生成的正确性 4. 边界情况和错误处理
 */
@SpringJUnitConfig
@DisplayName("搜索条件构建器集成测试")
public class SearchConditionBuilderIntegrationTest {

    private SearchConditionManager searchConditionManager;
    private List<SearchConditionBuilder> conditionBuilders;

    @BeforeEach
    void setUp() {
        // 创建真实的条件构建器实例（按优先级顺序）
        conditionBuilders =
                Arrays.asList(
                        new KeywordComplexExpressionBuilder(), // Order(10) - 高优先级
                        new KeywordMatchAllConditionBuilder(), // Order(20) - 中等优先级
                        new KeywordMatchAnyConditionBuilder(), // Order(20) - 中等优先级
                        new WhereSqlConditionBuilder() // Order(30) - 低优先级
                        );

        searchConditionManager = new SearchConditionManager(conditionBuilders);
    }

    // ==================== 单个关键字测试 ====================

    @Test
    @DisplayName("单个简单关键字 - 应使用MATCH_ANY")
    public void testSingleSimpleKeyword() {
        /**
         * 测试表达式：["error"] 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message MATCH_ANY 'error'
         * 说明：简单的单个关键字应该被MATCH_ANY处理器处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("error"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ANY 'error'", result);
    }

    @Test
    @DisplayName("单个带引号的关键字 - 应使用MATCH_ANY")
    public void testSingleQuotedKeyword() {
        /**
         * 测试表达式：["'database error'"] 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message MATCH_ANY
         * 'database error' 说明：带引号的单个关键字也应该被MATCH_ANY处理器处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'database error'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ANY 'database error'", result);
    }

    @Test
    @DisplayName("多个简单关键字 - 应使用MATCH_ANY并用AND连接")
    public void testMultipleSimpleKeywords() {
        /**
         * 测试表达式：["error", "timeout", "critical"] 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message
         * MATCH_ANY 'error' AND message MATCH_ANY 'timeout' AND message MATCH_ANY 'critical'
         * 说明：多个简单关键字应该分别生成MATCH_ANY条件，然后用AND连接
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "timeout", "critical"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout'"));
        assertTrue(result.contains("message MATCH_ANY 'critical'"));
        assertEquals(2, countOccurrences(result, " AND "));
    }

    // ==================== OR表达式测试 ====================

    @Test
    @DisplayName("简单OR表达式 - 应使用MATCH_ANY")
    public void testSimpleOrExpression() {
        /**
         * 测试表达式：["'error' || 'warning'"] 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message
         * MATCH_ANY 'error warning' 说明：简单的OR表达式应该被MATCH_ANY处理器处理，关键字用空格连接
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'error' || 'warning'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ANY 'error warning'", result);
    }

    @Test
    @DisplayName("多个词的OR表达式 - 应使用MATCH_ANY")
    public void testMultipleWordsOrExpression() {
        /**
         * 测试表达式：["'database error' || 'connection timeout' || 'service failure'"]
         * 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message MATCH_ANY 'database error connection
         * timeout service failure' 说明：多个词组成的OR表达式应该被正确处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Collections.singletonList(
                        "'database error' || 'connection timeout' || 'service failure'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals(
                "message MATCH_ANY 'database error connection timeout service failure'", result);
    }

    @Test
    @DisplayName("无引号的OR表达式 - 应使用MATCH_ANY")
    public void testUnquotedOrExpression() {
        /**
         * 测试表达式：["error || warning || critical"] 预期处理器：KeywordMatchAnyConditionBuilder 预期结果：message
         * MATCH_ANY 'error warning critical' 说明：没有引号的OR表达式也应该被正确处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("error || warning || critical"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ANY 'error warning critical'", result);
    }

    // ==================== AND表达式测试 ====================

    @Test
    @DisplayName("简单AND表达式 - 应使用MATCH_ALL")
    public void testSimpleAndExpression() {
        /**
         * 测试表达式：["'error' && 'critical'"] 预期处理器：KeywordMatchAllConditionBuilder 预期结果：message
         * MATCH_ALL 'error critical' 说明：简单的AND表达式应该被MATCH_ALL处理器处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'error' && 'critical'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ALL 'error critical'", result);
    }

    @Test
    @DisplayName("多个词的AND表达式 - 应使用MATCH_ALL")
    public void testMultipleWordsAndExpression() {
        /**
         * 测试表达式：["'database' && 'connection' && 'timeout'"] 预期处理器：KeywordMatchAllConditionBuilder
         * 预期结果：message MATCH_ALL 'database connection timeout' 说明：多个词的AND表达式应该将所有词用空格连接
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'database' && 'connection' && 'timeout'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ALL 'database connection timeout'", result);
    }

    @Test
    @DisplayName("无引号的AND表达式 - 应使用MATCH_ALL")
    public void testUnquotedAndExpression() {
        /**
         * 测试表达式：["database && connection && failed"] 预期处理器：KeywordMatchAllConditionBuilder
         * 预期结果：message MATCH_ALL 'database connection failed' 说明：没有引号的AND表达式也应该被正确处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("database && connection && failed"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("message MATCH_ALL 'database connection failed'", result);
    }

    // ==================== 复杂表达式测试 ====================

    @Test
    @DisplayName("简单括号表达式 - 应使用复杂表达式处理器")
    public void testSimpleParenthesesExpression() {
        /**
         * 测试表达式：["('error' || 'warning') && 'critical'"] 预期处理器：KeywordComplexExpressionBuilder
         * 预期结果：根据KeywordExpressionParser的解析结果 说明：包含括号的表达式应该被复杂表达式处理器处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("('error' || 'warning') && 'critical'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 复杂表达式的具体结果取决于KeywordExpressionParser的实现
        assertTrue(result.contains("message MATCH_"));
    }

    @Test
    @DisplayName("复杂嵌套表达式 - 应使用复杂表达式处理器")
    public void testComplexNestedExpression() {
        /**
         * 测试表达式：["('database' || 'service') && ('error' || 'timeout')"]
         * 预期处理器：KeywordComplexExpressionBuilder 预期结果：根据KeywordExpressionParser的解析结果
         * 说明：复杂的嵌套表达式应该被正确解析
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Collections.singletonList("('database' || 'service') && ('error' || 'timeout')"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("message MATCH_"));
    }

    @Test
    @DisplayName("混合AND和OR操作符 - 应使用复杂表达式处理器")
    public void testMixedAndOrOperators() {
        /**
         * 测试表达式：["'error' || 'warning' && 'critical'"] 预期处理器：KeywordComplexExpressionBuilder
         * 预期结果：根据操作符优先级的解析结果 说明：同时包含&&和||的表达式应该被复杂表达式处理器处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'error' || 'warning' && 'critical'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("message MATCH_"));
    }

    // ==================== WHERE SQL条件测试 ====================

    @Test
    @DisplayName("单个WHERE SQL条件")
    public void testSingleWhereSqlCondition() {
        /**
         * 测试表达式：whereSqls = ["level = 'ERROR'"] 预期处理器：WhereSqlConditionBuilder 预期结果：(level =
         * 'ERROR') 说明：WHERE SQL条件应该被直接包装在括号中
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(Collections.singletonList("level = 'ERROR'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("(level = 'ERROR')", result);
    }

    @Test
    @DisplayName("多个WHERE SQL条件")
    public void testMultipleWhereSqlConditions() {
        /**
         * 测试表达式：whereSqls = ["level = 'ERROR'", "service_name = 'user-service'", "status =
         * 'failed'"] 预期处理器：WhereSqlConditionBuilder 预期结果：(level = 'ERROR') AND (service_name =
         * 'user-service') AND (status = 'failed') 说明：多个WHERE SQL条件应该用AND连接，每个都包装在括号中
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setWhereSqls(
                Arrays.asList(
                        "level = 'ERROR'", "service_name = 'user-service'", "status = 'failed'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertTrue(result.contains("(level = 'ERROR')"));
        assertTrue(result.contains("(service_name = 'user-service')"));
        assertTrue(result.contains("(status = 'failed')"));
        assertEquals(2, countOccurrences(result, " AND "));
    }

    // ==================== 混合条件测试 ====================

    @Test
    @DisplayName("关键字和WHERE SQL混合条件")
    public void testMixedKeywordAndWhereSqlConditions() {
        /**
         * 测试表达式：keywords = ["error", "timeout"], whereSqls = ["level = 'ERROR'", "service_name =
         * 'api'"] 预期处理器：KeywordMatchAnyConditionBuilder + WhereSqlConditionBuilder 预期结果：message
         * MATCH_ANY 'error' AND message MATCH_ANY 'timeout' AND (level = 'ERROR') AND (service_name
         * = 'api') 说明：关键字条件和WHERE SQL条件应该都被处理，并用AND连接
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "timeout"));
        dto.setWhereSqls(Arrays.asList("level = 'ERROR'", "service_name = 'api'"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout'"));
        assertTrue(result.contains("(level = 'ERROR')"));
        assertTrue(result.contains("(service_name = 'api')"));
        assertEquals(3, countOccurrences(result, " AND "));
    }

    @Test
    @DisplayName("复杂关键字和WHERE SQL混合条件")
    public void testComplexMixedConditions() {
        /**
         * 测试表达式： keywords = ["error", "'warning' || 'critical'", "'database' && 'timeout'",
         * "('service' || 'api') && 'failed'"] whereSqls = ["level IN ('ERROR', 'WARN')", "duration
         * > 5000"] 预期结果：包含所有条件的复杂SQL，各种处理器都被使用 说明：各种类型的关键字表达式和WHERE条件应该都能正确处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "error", // 简单关键字 -> MATCH_ANY
                        "'warning' || 'critical'", // OR表达式 -> MATCH_ANY
                        "'database' && 'timeout'", // AND表达式 -> MATCH_ALL
                        "('service' || 'api') && 'failed'" // 复杂表达式 -> 复杂处理器
                        ));
        dto.setWhereSqls(Arrays.asList("level IN ('ERROR', 'WARN')", "duration > 5000"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证包含各种类型的条件
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'warning critical'"));
        assertTrue(result.contains("message MATCH_ALL 'database timeout'"));
        assertTrue(
                result.contains(
                        "message MATCH_ANY 'service api' AND message MATCH_ANY"
                                + " 'failed'")); // 复杂表达式的结果
        assertTrue(result.contains("(level IN ('ERROR', 'WARN'))"));
        assertTrue(result.contains("(duration > 5000)"));

        // 验证AND连接符的数量（6个条件应该有5个AND）
        assertTrue(countOccurrences(result, " AND ") >= 4);
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("空关键字列表")
    public void testEmptyKeywordsList() {
        /** 测试表达式：keywords = [] 预期结果：空字符串 说明：空的关键字列表应该不产生任何条件 */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.emptyList());

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("", result);
    }

    @Test
    @DisplayName("空字符串关键字")
    public void testEmptyStringKeywords() {
        /** 测试表达式：keywords = ["", " ", null] 预期结果：空字符串 说明：空字符串、空白字符串和null关键字应该被忽略 */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("", "   ", null));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertEquals("", result);
    }

    @Test
    @DisplayName("混合有效和无效关键字")
    public void testMixedValidAndInvalidKeywords() {
        /**
         * 测试表达式：keywords = ["error", "", "timeout", null, " ", "critical"] 预期结果：只处理有效的关键字
         * 说明：无效的关键字应该被忽略，有效的关键字正常处理
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "", "timeout", null, "   ", "critical"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout'"));
        assertTrue(result.contains("message MATCH_ANY 'critical'"));
        assertEquals(2, countOccurrences(result, " AND "));
    }

    // ==================== 优先级测试 ====================

    @Test
    @DisplayName("条件构建器优先级测试 - 复杂表达式优先级最高")
    public void testBuilderPriorityComplexFirst() {
        /**
         * 测试表达式：keywords = ["('error' || 'warning')"] 预期行为：即使表达式可能被其他构建器支持，复杂表达式构建器应该先处理
         * 预期处理器：KeywordComplexExpressionBuilder 说明：验证@Order(10)的复杂表达式构建器确实有最高优先级
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("('error' || 'warning')"));

        String result = searchConditionManager.buildSearchConditions(dto);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 应该是复杂表达式处理器处理的结果，而不是简单的MATCH_ANY
    }

    @Test
    @DisplayName("条件构建器优先级测试 - MATCH_ALL vs MATCH_ANY")
    public void testBuilderPriorityMatchAllVsMatchAny() {
        /**
         * 测试目标：验证对于同时被MATCH_ALL和MATCH_ANY支持的表达式，哪个优先级更高
         * 注意：当前实现中，MATCH_ALL和MATCH_ANY都是@Order(20)，但支持条件不重叠 这个测试主要验证supports()方法的正确性
         */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("error && timeout"));
        String result1 = searchConditionManager.buildSearchConditions(dto1);
        assertTrue(result1.contains("MATCH_ALL"));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("error || timeout"));
        String result2 = searchConditionManager.buildSearchConditions(dto2);
        assertTrue(result2.contains("MATCH_ANY"));
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
}
