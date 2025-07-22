package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.KeywordConditionBuilder;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO.KeywordFieldConfigDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 关键字条件构建器测试
 *
 * <p>测试基于keywords字段的自动查询条件构建功能，验证： - 自动应用模块配置中的所有关键字字段 - 配置驱动的搜索方法 -
 * 多个关键字之间使用AND连接，每个关键字对所有配置字段使用OR连接 - Variant字段自动转换
 */
@DisplayName("关键字条件构建器测试")
class KeywordConditionBuilderTest {

    private KeywordConditionBuilder keywordConditionBuilder;

    @Mock private QueryConfigValidationService queryConfigValidationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 创建真实的VariantFieldConverter实例
        VariantFieldConverter variantFieldConverter = new VariantFieldConverter();

        // 手动创建KeywordConditionBuilder并注入依赖
        keywordConditionBuilder = new KeywordConditionBuilder();
        ReflectionTestUtils.setField(
                keywordConditionBuilder,
                "queryConfigValidationService",
                queryConfigValidationService);
        ReflectionTestUtils.setField(
                keywordConditionBuilder, "variantFieldConverter", variantFieldConverter);
    }

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionTests {

        @Test
        @DisplayName("单关键字单字段LIKE搜索")
        void testSingleKeywordSingleFieldLike() {
            // 模拟配置：message字段使用LIKE搜索
            mockModuleConfig("nginx", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("nginx", List.of("error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("单关键字多字段OR连接")
        void testSingleKeywordMultipleFieldsOr() {
            // 模拟配置：多个字段使用不同搜索方法
            mockModuleConfig(
                    "app",
                    createKeywordField("message", "LIKE"),
                    createKeywordField("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("app", List.of("error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("((message LIKE '%error%') OR (content MATCH_PHRASE 'error'))", result);
        }

        @Test
        @DisplayName("多关键字AND连接")
        void testMultipleKeywordsAnd() {
            // 模拟配置：单字段LIKE搜索
            mockModuleConfig("nginx", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("nginx", List.of("error", "timeout"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("((message LIKE '%error%') AND (message LIKE '%timeout%'))", result);
        }

        @Test
        @DisplayName("多关键字多字段复合查询")
        void testMultipleKeywordsMultipleFields() {
            // 模拟配置：多字段不同搜索方法
            mockModuleConfig(
                    "system",
                    createKeywordField("message", "LIKE"),
                    createKeywordField("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("system", List.of("error", "warning"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            String expected =
                    "(((message LIKE '%error%') OR (content MATCH_PHRASE 'error')) AND ((message"
                            + " LIKE '%warning%') OR (content MATCH_PHRASE 'warning')))";
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("搜索方法测试")
    class SearchMethodTests {

        @ParameterizedTest
        @DisplayName("不同搜索方法测试")
        @CsvSource({
            "LIKE, error, '(message LIKE ''%error%'')'",
        })
        void testDifferentSearchMethods(String searchMethod, String keyword, String expected) {
            mockModuleConfig("test", createKeywordField("message", searchMethod));

            LogSearchDTO dto = createLogSearchDTO("test", List.of(keyword));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("MATCH_PHRASE搜索方法测试 - 带空格的关键字")
        void testMatchPhraseSearchMethod() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("'java exception'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message MATCH_PHRASE 'java exception')", result);
        }

        @Test
        @DisplayName("MATCH_ANY搜索方法测试 - 带空格的关键字")
        void testMatchAnySearchMethod() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_ANY"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("'urgent critical'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message MATCH_ANY 'urgent critical')", result);
        }

        @Test
        @DisplayName("MATCH_ALL搜索方法测试 - 带空格的关键字")
        void testMatchAllSearchMethod() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("'payment success'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message MATCH_ALL 'payment success')", result);
        }

        @Test
        @DisplayName("复杂表达式解析")
        void testComplexExpressionParsing() {
            mockModuleConfig("app", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto =
                    createLogSearchDTO("app", List.of("('error' || 'exception') && 'processing'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证复杂表达式被正确解析：'error' || 'exception' 和 'processing' 分别处理
            assertEquals(
                    "(( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'exception' ) AND"
                            + " message MATCH_PHRASE 'processing')",
                    result);
        }

        @Test
        @DisplayName("多字段复杂嵌套表达式解析 - 最复杂场景")
        void testMultiFieldComplexNestedExpressionParsing() {
            // 配置多个字段使用不同搜索方法
            mockModuleConfig(
                    "system",
                    createKeywordField("message", "MATCH_PHRASE"),
                    createKeywordField("content", "LIKE"),
                    createKeywordField("tags", "MATCH_ANY"));

            // 复杂嵌套表达式：(('error' || 'exception') && 'processing') || 'critical'
            LogSearchDTO dto =
                    createLogSearchDTO(
                            "system",
                            List.of("(('error' || 'exception') && 'processing') || 'critical'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证多字段复杂表达式：每个字段都应用完整的复杂表达式，字段间用OR连接
            String expected =
                    "((( ( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'exception' ) AND"
                        + " message MATCH_PHRASE 'processing' ) OR message MATCH_PHRASE 'critical')"
                        + " OR (( ( content LIKE '%error%' OR content LIKE '%exception%' ) AND"
                        + " content LIKE '%processing%' ) OR content LIKE '%critical%') OR (( ("
                        + " tags MATCH_ANY 'error' OR tags MATCH_ANY 'exception' ) AND tags"
                        + " MATCH_ANY 'processing' ) OR tags MATCH_ANY 'critical'))";

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("多字段多关键字复杂组合 - 终极复杂场景")
        void testMultiFieldMultiKeywordComplexCombination() {
            // 配置多个字段
            mockModuleConfig(
                    "enterprise",
                    createKeywordField("message", "MATCH_PHRASE"),
                    createKeywordField("tags", "MATCH_ANY"));

            // 多个复杂关键字：AND连接
            LogSearchDTO dto =
                    createLogSearchDTO(
                            "enterprise",
                            List.of(
                                    "('error' || 'exception') && 'request'", // 第一个复杂关键字
                                    "'user' || 'admin'" // 第二个复杂关键字
                                    ));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证：多个关键字间AND连接，每个关键字对所有字段使用OR连接
            String expected =
                    "(((( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'exception' ) AND"
                        + " message MATCH_PHRASE 'request') OR (( tags MATCH_ANY 'error' OR tags"
                        + " MATCH_ANY 'exception' ) AND tags MATCH_ANY 'request')) AND ((message"
                        + " MATCH_PHRASE 'user' OR message MATCH_PHRASE 'admin') OR (tags MATCH_ANY"
                        + " 'user' OR tags MATCH_ANY 'admin')))";

            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Variant字段转换测试")
    class VariantFieldTests {

        @Test
        @DisplayName("点语法字段自动转换为bracket语法")
        void testDotSyntaxFieldConversion() {
            // 使用真实的点语法字段，VariantFieldConverter会自动处理转换
            mockModuleConfig("app", createKeywordField("message.level", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("app", List.of("ERROR"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证点语法字段被转换为bracket语法
            assertEquals("(message['level'] LIKE '%ERROR%')", result);
        }

        @Test
        @DisplayName("混合字段转换 - 点语法和普通字段")
        void testMixedFieldConversion() {
            // 配置包含点语法字段和普通字段
            mockModuleConfig(
                    "test",
                    createKeywordField("message.level", "LIKE"), // 点语法字段，会被转换
                    createKeywordField("host", "MATCH_PHRASE")); // 普通字段，不需要转换

            LogSearchDTO dto = createLogSearchDTO("test", List.of("server"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证点语法字段被转换，普通字段保持不变
            assertEquals(
                    "((message['level'] LIKE '%server%') OR (host MATCH_PHRASE 'server'))", result);
        }

        @Test
        @DisplayName("多层嵌套点语法字段转换")
        void testNestedDotSyntaxFieldConversion() {
            // 使用多层嵌套的点语法字段
            mockModuleConfig(
                    "system", createKeywordField("request.user.profile.name", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("system", List.of("admin"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证多层嵌套点语法被正确转换
            assertEquals("(request['user']['profile']['name'] MATCH_PHRASE 'admin')", result);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("空关键字列表")
        void testEmptyKeywords() {
            LogSearchDTO dto = createLogSearchDTO("nginx", Collections.emptyList());

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("", result);
        }

        @Test
        @DisplayName("null关键字列表")
        void testNullKeywords() {
            LogSearchDTO dto = createLogSearchDTO("nginx", null);

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("", result);
        }

        @Test
        @DisplayName("空白关键字过滤")
        void testBlankKeywordsFiltering() {
            mockModuleConfig("nginx", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("nginx", List.of("", "  ", "error", "\t"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("无关键字字段配置")
        void testNoKeywordFieldsConfig() {
            // 模拟没有关键字字段配置
            QueryConfigDTO config = new QueryConfigDTO();
            config.setKeywordFields(Collections.emptyList());
            when(queryConfigValidationService.validateAndGetQueryConfig("empty"))
                    .thenReturn(config);

            LogSearchDTO dto = createLogSearchDTO("empty", List.of("error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("", result);
        }

        @Test
        @DisplayName("关键字trim处理")
        void testKeywordTrimming() {
            mockModuleConfig("nginx", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("nginx", List.of("  error  "));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("NOT-边界-001: 减号后面跟空字符串")
        void testMinusFollowedByEmptyString() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("- "));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 减号后面没有内容，应该返回空条件
            assertEquals("", result);
        }

        @Test
        @DisplayName("NOT-边界-002: 减号在单词中间")
        void testMinusInMiddleOfWord() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("test-data"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 单词中间的减号应该被当作普通字符
            assertEquals("(message MATCH_PHRASE 'test-data')", result);
        }
    }

    @Nested
    @DisplayName("括号逻辑测试")
    class ParenthesesLogicTests {

        @Test
        @DisplayName("单关键字单字段不加外层括号")
        void testSingleKeywordSingleFieldNoOuterParentheses() {
            mockModuleConfig("test", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("单关键字多字段加外层括号")
        void testSingleKeywordMultipleFieldsWithOuterParentheses() {
            mockModuleConfig(
                    "test",
                    createKeywordField("message", "LIKE"),
                    createKeywordField("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("((message LIKE '%error%') OR (content MATCH_PHRASE 'error'))", result);
        }

        @Test
        @DisplayName("多关键字加最外层括号")
        void testMultipleKeywordsWithOutermostParentheses() {
            mockModuleConfig("test", createKeywordField("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("error", "warning"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            assertEquals("((message LIKE '%error%') AND (message LIKE '%warning%'))", result);
        }
    }

    @Nested
    @DisplayName("NOT操作符功能测试")
    class NotOperatorFunctionTests {

        @Test
        @DisplayName("NOT-001: 基础NOT操作符测试")
        void testBasicNotOperator() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("- error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 单字段NOT条件，保留表达式逻辑结构
            assertEquals("NOT (message MATCH_PHRASE 'error')", result);
        }

        @Test
        @DisplayName("NOT-002: NOT操作符多字段测试")
        void testNotOperatorMultiField() {
            mockModuleConfig(
                    "test",
                    createKeywordField("message", "MATCH_PHRASE"),
                    createKeywordField("source", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("- error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 全局排除逻辑：NOT (任何字段包含term)
            assertEquals("NOT (message MATCH_PHRASE 'error' OR source LIKE '%error%')", result);
        }

        @Test
        @DisplayName("用户需求示例：复杂混合逻辑")
        void testUserRequirementExample() {
            mockModuleConfig(
                    "test",
                    createKeywordField("message_text", "MATCH_PHRASE"),
                    createKeywordField("source", "MATCH_PHRASE"));

            LogSearchDTO dto =
                    createLogSearchDTO(
                            "test",
                            List.of(
                                    "( engine || service ) && hina-cloud && ( - module || - engine"
                                            + " )"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 期望：((正向条件) AND NOT (负向条件))
            // 正向：( engine || service ) && hina-cloud 应用到两个字段
            // 负向：( - module || - engine ) 全局排除
            assertEquals(
                    """
                    (((( message_text MATCH_PHRASE 'engine' OR message_text MATCH_PHRASE 'service' ) \
                    AND message_text MATCH_PHRASE 'hina-cloud') OR \
                    (( source MATCH_PHRASE 'engine' OR source MATCH_PHRASE 'service' ) \
                    AND source MATCH_PHRASE 'hina-cloud')) AND \
                    NOT (message_text MATCH_PHRASE 'module' OR source MATCH_PHRASE 'module' OR \
                    message_text MATCH_PHRASE 'engine' OR source MATCH_PHRASE 'engine'))
                    """
                            .trim(),
                    result);
        }

        @Test
        @DisplayName("NOT-004: NOT与正常关键字混合")
        void testNotOperatorMixedWithNormalKeywords() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("error", "- debug"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 多个关键字AND连接，每个关键字保留自己的逻辑结构
            assertEquals(
                    "((message MATCH_PHRASE 'error') AND NOT (message MATCH_PHRASE 'debug'))",
                    result);
        }

        @Test
        @DisplayName("NOT-005: 引号内的减号不被当作NOT操作符")
        void testMinusInQuotesNotTreatedAsNotOperator() {
            mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("'test-data' && - error"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 引号内的减号作为普通字符，表达式外的减号作为NOT操作符
            assertEquals(
                    "((message MATCH_PHRASE 'test-data') AND NOT (message MATCH_PHRASE 'error'))",
                    result);
        }

        @Test
        @DisplayName("NOT-006: NOT操作符的Variant字段转换")
        void testNotOperatorWithVariantFieldConversion() {
            mockModuleConfig("test", createKeywordField("message.level", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test", List.of("- ERROR"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 验证Variant字段转换：message.level -> message['level']
            assertEquals("NOT (message['level'] MATCH_PHRASE 'ERROR')", result);
        }

        @Test
        @DisplayName("NOT-007: 复杂混合表达式 - 引号包围的负向条件")
        void testComplexMixedExpressionWithQuotedNegativeTerms() {
            mockModuleConfig(
                    "test",
                    createKeywordField("message_text", "MATCH_PHRASE"),
                    createKeywordField("source", "MATCH_PHRASE"));

            LogSearchDTO dto =
                    createLogSearchDTO(
                            "test",
                            List.of(
                                    "( engine || service ) && hina-cloud && - 'module' && -"
                                            + " 'engine'"));

            String result = keywordConditionBuilder.buildKeywords(dto);

            // 期望：正向条件 AND 全局排除条件
            // 正向：( engine || service ) && hina-cloud 应用到两个字段
            // 负向：- 'module' && - 'engine' 全局排除 module 和 engine
            assertEquals(
                    """
                    (((( message_text MATCH_PHRASE 'engine' OR message_text MATCH_PHRASE 'service' ) \
                    AND message_text MATCH_PHRASE 'hina-cloud') OR \
                    (( source MATCH_PHRASE 'engine' OR source MATCH_PHRASE 'service' ) \
                    AND source MATCH_PHRASE 'hina-cloud')) AND \
                    NOT (message_text MATCH_PHRASE 'module' OR source MATCH_PHRASE 'module' OR \
                    message_text MATCH_PHRASE 'engine' OR source MATCH_PHRASE 'engine'))
                    """
                            .trim(),
                    result);
        }
    }

    // ==================== 辅助方法 ====================

    private LogSearchDTO createLogSearchDTO(String module, List<String> keywords) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule(module);
        dto.setKeywords(keywords);
        return dto;
    }

    private KeywordFieldConfigDTO createKeywordField(String fieldName, String searchMethod) {
        KeywordFieldConfigDTO config = new KeywordFieldConfigDTO();
        config.setFieldName(fieldName);
        config.setSearchMethod(searchMethod);
        return config;
    }

    private void mockModuleConfig(String module, KeywordFieldConfigDTO... keywordFields) {
        QueryConfigDTO config = new QueryConfigDTO();
        config.setKeywordFields(Arrays.asList(keywordFields));
        when(queryConfigValidationService.validateAndGetQueryConfig(module)).thenReturn(config);
    }
}
