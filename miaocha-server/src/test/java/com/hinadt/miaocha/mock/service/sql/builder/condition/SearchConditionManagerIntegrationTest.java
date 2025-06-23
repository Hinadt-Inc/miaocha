package com.hinadt.miaocha.mock.service.sql.builder.condition;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.builder.condition.*;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SearchConditionManager集成测试 验证只有优先级最高的关键字Builder会被执行，避免多个Builder同时生成条件 */
public class SearchConditionManagerIntegrationTest {

    private SearchConditionManager searchConditionManager;

    @BeforeEach
    void setUp() {
        // 手动创建所有的Builder
        List<SearchConditionBuilder> builders =
                Arrays.asList(
                        new KeywordPhraseConditionBuilder(), // @Order(5) - 最高优先级
                        new KeywordMatchAnyConditionBuilder(), // @Order(25)
                        new KeywordMatchAllConditionBuilder(), // @Order(25)
                        new KeywordComplexExpressionBuilder() // @Order(20)
                        );

        // 创建SearchConditionManager
        searchConditionManager = new SearchConditionManager(builders);
    }

    @Test
    @DisplayName("单个简单关键字 - 应只使用KeywordPhraseConditionBuilder")
    public void testSingleSimpleKeywordUsesOnlyPhraseBuilder() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("45d76d0c-2e39-4150-b397-390ad55d14d9"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证只包含MATCH_PHRASE，不包含MATCH_ANY
        assertTrue(result.contains("MATCH_PHRASE"), "应包含MATCH_PHRASE");
        assertFalse(result.contains("MATCH_ANY"), "不应包含MATCH_ANY");
        assertFalse(result.contains("MATCH_ALL"), "不应包含MATCH_ALL");

        // 验证包含括号
        assertTrue(result.startsWith("("), "应以括号开始");
        assertTrue(result.endsWith(")"), "应以括号结束");

        // 验证只有一个条件
        assertEquals(1, countOccurrences(result, "message"), "应该只有一个message条件");
    }

    @Test
    @DisplayName("多个简单关键字 - 应只使用KeywordPhraseConditionBuilder")
    public void testMultipleSimpleKeywordsUsesOnlyPhraseBuilder() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Arrays.asList("error", "timeout", "critical"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证只包含MATCH_PHRASE，不包含MATCH_ANY
        assertTrue(result.contains("MATCH_PHRASE"), "应包含MATCH_PHRASE");
        assertFalse(result.contains("MATCH_ANY"), "不应包含MATCH_ANY");
        assertFalse(result.contains("MATCH_ALL"), "不应包含MATCH_ALL");

        // 验证包含所有关键字
        assertTrue(result.contains("'error'"), "应包含error");
        assertTrue(result.contains("'timeout'"), "应包含timeout");
        assertTrue(result.contains("'critical'"), "应包含critical");
    }

    @Test
    @DisplayName("OR表达式 - 应只使用KeywordPhraseConditionBuilder且包含括号")
    public void testOrExpressionUsesOnlyPhraseBuilder() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(
                Collections.singletonList(
                        "45d76d0c-2e39-4150-b397-390ad55d14d9 || H_js_sourcemap_version"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证只包含MATCH_PHRASE，不包含MATCH_ANY
        assertTrue(result.contains("MATCH_PHRASE"), "应包含MATCH_PHRASE");
        assertFalse(result.contains("MATCH_ANY"), "不应包含MATCH_ANY");
        assertTrue(result.contains(" OR "), "应包含OR操作符");

        // 验证包含外层括号
        assertTrue(result.startsWith("("), "应以括号开始");
        assertTrue(result.endsWith(")"), "应以括号结束");

        // 验证期望的结果格式
        String expected =
                "(message MATCH_PHRASE '45d76d0c-2e39-4150-b397-390ad55d14d9' OR message"
                        + " MATCH_PHRASE 'H_js_sourcemap_version')";
        assertEquals(expected, result, "应该生成正确的带括号的OR条件");
    }

    @Test
    @DisplayName("AND表达式 - 应只使用KeywordPhraseConditionBuilder")
    public void testAndExpressionUsesOnlyPhraseBuilder() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("'error' && 'critical'"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证只包含MATCH_PHRASE，不包含MATCH_ALL
        assertTrue(result.contains("MATCH_PHRASE"), "应包含MATCH_PHRASE");
        assertFalse(result.contains("MATCH_ALL"), "不应包含MATCH_ALL");
        assertTrue(result.contains(" AND "), "应包含AND操作符");
    }

    @Test
    @DisplayName("复杂表达式 - 应只使用KeywordPhraseConditionBuilder")
    public void testComplexExpressionUsesOnlyPhraseBuilder() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("('error' || 'warning') && 'critical'"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 验证只包含MATCH_PHRASE，不包含其他类型
        assertTrue(result.contains("MATCH_PHRASE"), "应包含MATCH_PHRASE");
        assertFalse(result.contains("MATCH_ANY"), "不应包含MATCH_ANY");
        assertFalse(result.contains("MATCH_ALL"), "不应包含MATCH_ALL");
        assertTrue(result.contains(" OR "), "应包含OR操作符");
        assertTrue(result.contains(" AND "), "应包含AND操作符");
    }

    @Test
    @DisplayName("真实场景测试 - UUID关键字搜索")
    public void testRealScenarioUUIDSearch() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setKeywords(Collections.singletonList("45d76d0c-2e39-4150-b397-390ad55d14d9"));

        String result = searchConditionManager.buildSearchConditions(dto);

        // 这就是用户遇到的问题：不应该同时包含MATCH_PHRASE和MATCH_ANY
        String expected = "(message MATCH_PHRASE '45d76d0c-2e39-4150-b397-390ad55d14d9')";
        assertEquals(expected, result, "应该只生成MATCH_PHRASE条件，并包含括号");
    }

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
