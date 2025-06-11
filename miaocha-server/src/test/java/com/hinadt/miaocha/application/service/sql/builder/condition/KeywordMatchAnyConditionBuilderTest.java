package com.hinadt.miaocha.application.service.sql.builder.condition;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import io.qameta.allure.*;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KeywordMatchAnyConditionBuilder 单元测试类
 *
 * <p>测试秒查系统中关键字匹配任意条件构建器的功能 验证MATCH_ANY查询语句的正确生成和匹配逻辑
 *
 * <p>测试目标：验证MATCH_ANY条件构建器的行为
 *
 * <p>支持的表达式类型： 1. 单个关键字：error 2. 带引号的关键字：'database error' 3. OR表达式：'error' || 'warning' 4. 不支持：包含
 * && 、( 、) 的表达式
 */
@Epic("秒查日志管理系统")
@Feature("日志检索")
@Story("关键字匹配")
@DisplayName("MATCH_ANY条件构建器测试")
@Owner("开发团队")
public class KeywordMatchAnyConditionBuilderTest {

    private KeywordMatchAnyConditionBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new KeywordMatchAnyConditionBuilder();
    }

    // ==================== supports()方法测试 ====================

    @Test
    @DisplayName("supports() - 空关键字列表 - 应返回false")
    public void testSupportsEmptyKeywords() {
        /** 测试目标：空关键字列表不应该被支持 输入：null, [], [""], [null], [" "] 预期：false */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(null);
        assertFalse(builder.supports(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.emptyList());
        assertFalse(builder.supports(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList(""));
        assertFalse(builder.supports(dto3));

        LogSearchDTO dto4 = new LogSearchDTO();
        dto4.setKeywords(Collections.singletonList(null));
        assertFalse(builder.supports(dto4));

        LogSearchDTO dto5 = new LogSearchDTO();
        dto5.setKeywords(Collections.singletonList("   "));
        assertFalse(builder.supports(dto5));
    }

    @Test
    @DisplayName("supports() - 简单关键字 - 应返回true")
    public void testSupportsSimpleKeywords() {
        /** 测试目标：简单关键字应该被支持 输入：单词、带引号的词语、特殊字符等 预期：true */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("error"));
        assertTrue(builder.supports(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("'database error'"));
        assertTrue(builder.supports(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("timeout_123"));
        assertTrue(builder.supports(dto3));

        LogSearchDTO dto4 = new LogSearchDTO();
        dto4.setKeywords(Collections.singletonList("'用户服务异常'"));
        assertTrue(builder.supports(dto4));
    }

    @Test
    @DisplayName("supports() - OR表达式 - 应返回true")
    public void testSupportsOrExpressions() {
        /** 测试目标：OR表达式应该被支持 输入：各种格式的OR表达式 预期：true */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("'error' || 'warning'"));
        assertTrue(builder.supports(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("error || timeout || failure"));
        assertTrue(builder.supports(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("'database error' || 'connection timeout'"));
        assertTrue(builder.supports(dto3));
    }

    @Test
    @DisplayName("supports() - AND表达式 - 应返回false")
    public void testSupportsAndExpressions() {
        /** 测试目标：AND表达式不应该被支持（由KeywordMatchAllConditionBuilder处理） 输入：包含 && 的表达式 预期：false */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("'error' && 'critical'"));
        assertFalse(builder.supports(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("database && timeout"));
        assertFalse(builder.supports(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("'user' && 'service' && 'error'"));
        assertFalse(builder.supports(dto3));
    }

    @Test
    @DisplayName("supports() - 包含括号的表达式 - 应返回false")
    public void testSupportsParenthesesExpressions() {
        /** 测试目标：包含括号的表达式不应该被支持（由KeywordComplexExpressionBuilder处理） 输入：包含 ( 或 ) 的表达式 预期：false */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("('error' || 'warning')"));
        assertFalse(builder.supports(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("('error' || 'warning') && 'critical'"));
        assertFalse(builder.supports(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("error)"));
        assertFalse(builder.supports(dto3));
    }

    @Test
    @DisplayName("supports() - 混合关键字列表 - 应返回true如果有任何支持的")
    public void testSupportsMixedKeywordsList() {
        /** 测试目标：关键字列表中只要有任何一个被支持，就应该返回true 输入：混合支持和不支持的关键字 预期：true（因为有支持的关键字） */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "'error' && 'critical'", // 不支持 - 包含&&
                        "error", // 支持 - 简单关键字
                        "('warning')", // 不支持 - 包含括号
                        "'timeout' || 'failure'" // 支持 - OR表达式
                        ));
        assertTrue(builder.supports(dto));
    }

    // ==================== buildCondition()方法测试 ====================

    @Test
    @DisplayName("buildCondition() - 不支持的表达式 - 应返回空字符串")
    public void testBuildConditionUnsupported() {
        /** 测试目标：对于不支持的表达式，应该返回空字符串 输入：AND表达式、括号表达式、空列表等 预期：空字符串 */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("'error' && 'critical'"));
        assertEquals("", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("('error')"));
        assertEquals("", builder.buildCondition(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.emptyList());
        assertEquals("", builder.buildCondition(dto3));
    }

    @Test
    @DisplayName("buildCondition() - 单个简单关键字")
    public void testBuildConditionSingleKeyword() {
        /** 测试目标：单个简单关键字应该生成MATCH_ANY条件 输入：各种单个关键字 预期：message MATCH_ANY '关键字' */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("error"));
        assertEquals("message MATCH_ANY 'error'", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("'database error'"));
        assertEquals("message MATCH_ANY 'database error'", builder.buildCondition(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("timeout_123"));
        assertEquals("message MATCH_ANY 'timeout_123'", builder.buildCondition(dto3));
    }

    @Test
    @DisplayName("buildCondition() - 多个简单关键字")
    public void testBuildConditionMultipleKeywords() {
        /**
         * 测试目标：多个简单关键字应该分别生成MATCH_ANY条件，用AND连接 输入：多个简单关键字 预期：message MATCH_ANY '关键字1' AND message
         * MATCH_ANY '关键字2' ...
         */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "timeout", "critical"));

        String result = builder.buildCondition(dto);
        assertEquals(
                "message MATCH_ANY 'error' AND message MATCH_ANY 'timeout' AND message MATCH_ANY"
                        + " 'critical'",
                result);
    }

    @Test
    @DisplayName("buildCondition() - OR表达式")
    public void testBuildConditionOrExpression() {
        /** 测试目标：OR表达式应该将关键字提取并用空格连接 输入：各种OR表达式格式 预期：message MATCH_ANY '关键字1 关键字2 ...' */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("'error' || 'warning'"));
        assertEquals("message MATCH_ANY 'error warning'", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("error || timeout || failure"));
        assertEquals("message MATCH_ANY 'error timeout failure'", builder.buildCondition(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("'database error' || 'connection timeout'"));
        assertEquals(
                "message MATCH_ANY 'database error connection timeout'",
                builder.buildCondition(dto3));
    }

    @Test
    @DisplayName("buildCondition() - 混合关键字类型")
    public void testBuildConditionMixedKeywordTypes() {
        /** 测试目标：混合简单关键字和OR表达式应该都被正确处理 输入：简单关键字 + OR表达式的组合 预期：每种类型生成相应的MATCH_ANY条件，用AND连接 */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "error", // 简单关键字
                        "'warning' || 'critical'", // OR表达式
                        "timeout" // 简单关键字
                        ));

        String result = builder.buildCondition(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'warning critical'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout'"));
        assertEquals(2, countOccurrences(result, " AND "));
    }

    @Test
    @DisplayName("buildCondition() - 引号处理")
    public void testBuildConditionQuoteHandling() {
        /** 测试目标：各种引号格式应该被正确处理 输入：带引号和不带引号的关键字 预期：引号被正确移除，内容正确提取 */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("'error'"));
        assertEquals("message MATCH_ANY 'error'", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("error"));
        assertEquals("message MATCH_ANY 'error'", builder.buildCondition(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("'database connection error'"));
        assertEquals("message MATCH_ANY 'database connection error'", builder.buildCondition(dto3));
    }

    @Test
    @DisplayName("buildCondition() - 空白字符处理")
    public void testBuildConditionWhitespaceHandling() {
        /** 测试目标：各种空白字符应该被正确处理 输入：包含前后空格、多余空格的关键字 预期：空格被正确trim，结果正确 */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("  error  "));
        assertEquals("message MATCH_ANY 'error'", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("'error'  ||  'warning'"));
        assertEquals("message MATCH_ANY 'error warning'", builder.buildCondition(dto2));
    }

    @Test
    @DisplayName("buildCondition() - 混合支持和不支持的关键字")
    public void testBuildConditionMixedSupportedUnsupported() {
        /** 测试目标：混合支持和不支持的关键字列表中，只处理支持的 输入：包含AND表达式（不支持）和简单关键字（支持）的混合列表 预期：只生成支持的关键字的条件 */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "'error' && 'critical'", // 不支持 - 包含&&
                        "error", // 支持 - 简单关键字
                        "('warning')", // 不支持 - 包含括号
                        "'timeout' || 'failure'", // 支持 - OR表达式
                        "info" // 支持 - 简单关键字
                        ));

        String result = builder.buildCondition(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout failure'"));
        assertTrue(result.contains("message MATCH_ANY 'info'"));
        assertFalse(result.contains("critical"));
        assertFalse(result.contains("warning"));
        assertEquals(2, countOccurrences(result, " AND "));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("buildCondition() - 特殊字符处理")
    public void testBuildConditionSpecialCharacters() {
        /** 测试目标：特殊字符应该被正确处理 输入：包含数字、下划线、中文等特殊字符的关键字 预期：特殊字符被保留，正确生成条件 */
        LogSearchDTO dto1 = new LogSearchDTO();
        dto1.setKeywords(Collections.singletonList("error_404"));
        assertEquals("message MATCH_ANY 'error_404'", builder.buildCondition(dto1));

        LogSearchDTO dto2 = new LogSearchDTO();
        dto2.setKeywords(Collections.singletonList("'用户服务异常'"));
        assertEquals("message MATCH_ANY '用户服务异常'", builder.buildCondition(dto2));

        LogSearchDTO dto3 = new LogSearchDTO();
        dto3.setKeywords(Collections.singletonList("'error-500' || 'timeout-60s'"));
        assertEquals("message MATCH_ANY 'error-500 timeout-60s'", builder.buildCondition(dto3));
    }

    @Test
    @DisplayName("buildCondition() - 空内容关键字过滤")
    public void testBuildConditionEmptyContentFiltering() {
        /** 测试目标：空内容的关键字应该被过滤掉 输入：包含空字符串、null、纯空格的关键字列表 预期：只处理有实际内容的关键字 */
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Arrays.asList(
                        "error", // 有效
                        "", // 无效 - 空字符串
                        "timeout", // 有效
                        null, // 无效 - null
                        "   ", // 无效 - 纯空格
                        "critical" // 有效
                        ));

        String result = builder.buildCondition(dto);
        assertTrue(result.contains("message MATCH_ANY 'error'"));
        assertTrue(result.contains("message MATCH_ANY 'timeout'"));
        assertTrue(result.contains("message MATCH_ANY 'critical'"));
        assertEquals(2, countOccurrences(result, " AND "));
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
