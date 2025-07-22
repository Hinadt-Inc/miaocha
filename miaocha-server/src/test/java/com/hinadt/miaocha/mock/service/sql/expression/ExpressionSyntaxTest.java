package com.hinadt.miaocha.mock.service.sql.expression;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.KeywordConditionBuilder;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.application.service.sql.expression.ExpressionToken;
import com.hinadt.miaocha.application.service.sql.expression.ExpressionTokenizer;
import com.hinadt.miaocha.application.service.sql.expression.FieldExpressionParser;
import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO.KeywordFieldConfigDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 表达式语法测试
 *
 * <p>专门测试减号语法的三种不同形式：
 *
 * <ul>
 *   <li>`- a` - NOT操作符后跟普通词项
 *   <li>`- 'a'` - NOT操作符后跟引号词项
 *   <li>`'- a'` - 引号内包含减号的普通字符串
 * </ul>
 */
@DisplayName("表达式语法区别测试")
class ExpressionSyntaxTest {

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

    @Test
    @DisplayName("语法-001: `- a` - NOT操作符后跟普通词项")
    void testNotOperatorWithPlainTerm() {
        // 测试词法分析
        String input = "- error";
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(input);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        // 验证Token序列：NOT + TERM + EOF
        assertEquals(3, tokens.size());
        assertEquals(ExpressionToken.TokenType.NOT, tokens.get(0).type());
        assertEquals("-", tokens.get(0).value());
        assertEquals(ExpressionToken.TokenType.TERM, tokens.get(1).type());
        assertEquals("error", tokens.get(1).value());
        assertEquals(ExpressionToken.TokenType.EOF, tokens.get(2).type());

        // 测试语法分析
        FieldExpressionParser parser =
                new FieldExpressionParser("message", SearchMethod.MATCH_PHRASE);
        String result = parser.parseKeywordExpression(input);
        assertEquals("NOT message MATCH_PHRASE 'error'", result);

        // 测试完整流程
        mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));
        LogSearchDTO dto = createLogSearchDTO("test", List.of("- error"));
        String fullResult = keywordConditionBuilder.buildKeywords(dto);
        assertEquals("NOT (message MATCH_PHRASE 'error')", fullResult);

        System.out.println("✅ `- a` 解析正确: " + fullResult);
    }

    @Test
    @DisplayName("语法-002: `- 'a'` - NOT操作符后跟引号词项")
    void testNotOperatorWithQuotedTerm() {
        // 测试词法分析
        String input = "- 'java exception'";
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(input);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        // 验证Token序列：NOT + TERM + EOF
        assertEquals(3, tokens.size());
        assertEquals(ExpressionToken.TokenType.NOT, tokens.get(0).type());
        assertEquals("-", tokens.get(0).value());
        assertEquals(ExpressionToken.TokenType.TERM, tokens.get(1).type());
        assertEquals("java exception", tokens.get(1).value()); // 引号被去掉，内容保留
        assertEquals(ExpressionToken.TokenType.EOF, tokens.get(2).type());

        // 测试语法分析
        FieldExpressionParser parser =
                new FieldExpressionParser("message", SearchMethod.MATCH_PHRASE);
        String result = parser.parseKeywordExpression(input);
        assertEquals("NOT message MATCH_PHRASE 'java exception'", result);

        // 测试完整流程
        mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));
        LogSearchDTO dto = createLogSearchDTO("test", List.of("- 'java exception'"));
        String fullResult = keywordConditionBuilder.buildKeywords(dto);
        assertEquals("NOT (message MATCH_PHRASE 'java exception')", fullResult);

        System.out.println("✅ `- 'a'` 解析正确: " + fullResult);
    }

    @Test
    @DisplayName("语法-003: `'- a'` - 引号内包含减号的普通字符串")
    void testQuotedStringWithMinus() {
        // 测试词法分析
        String input = "'- error'";
        ExpressionTokenizer tokenizer = new ExpressionTokenizer(input);
        List<ExpressionToken> tokens = tokenizer.tokenize();

        // 验证Token序列：TERM + EOF（整个引号内容是一个词项）
        assertEquals(2, tokens.size());
        assertEquals(ExpressionToken.TokenType.TERM, tokens.get(0).type());
        assertEquals("- error", tokens.get(0).value()); // 引号内的减号是普通字符
        assertEquals(ExpressionToken.TokenType.EOF, tokens.get(1).type());

        // 测试语法分析
        FieldExpressionParser parser =
                new FieldExpressionParser("message", SearchMethod.MATCH_PHRASE);
        String result = parser.parseKeywordExpression(input);
        assertEquals("message MATCH_PHRASE '- error'", result); // 没有NOT，是普通搜索

        // 测试完整流程
        mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));
        LogSearchDTO dto = createLogSearchDTO("test", List.of("'- error'"));
        String fullResult = keywordConditionBuilder.buildKeywords(dto);
        assertEquals("(message MATCH_PHRASE '- error')", fullResult);

        System.out.println("✅ `'- a'` 解析正确: " + fullResult);
    }

    @Test
    @DisplayName("语法-004: 复杂组合 - 混合三种语法")
    void testMixedSyntaxCombination() {
        // 测试复杂表达式：'system-error' && - timeout && - 'connection failed'
        String input = "'system-error' && - timeout && - 'connection failed'";

        mockModuleConfig("test", createKeywordField("message", "MATCH_PHRASE"));
        LogSearchDTO dto = createLogSearchDTO("test", List.of(input));
        String result = keywordConditionBuilder.buildKeywords(dto);

        String expected =
                """
                ((message MATCH_PHRASE 'system-error') AND \
                NOT (message MATCH_PHRASE 'timeout' OR message MATCH_PHRASE 'connection failed'))
                """
                        .trim();

        assertEquals(expected, result);
        System.out.println("✅ 混合语法解析正确: " + result);
    }

    @Test
    @DisplayName("语法-005: 边界情况 - 连续减号和空格处理")
    void testEdgeCasesWithMinusAndSpaces() {
        // 测试各种边界情况

        // 1. 多个空格的NOT操作符
        testSingleCase("test", "-   error", "NOT (message MATCH_PHRASE 'error')", "多空格NOT操作符");

        // 2. 引号内的多个减号
        testSingleCase("test", "'--error'", "(message MATCH_PHRASE '--error')", "引号内多减号");

        // 3. 括号内的NOT操作符 - KeywordConditionBuilder会添加外层括号
        testSingleCase("test", "( - error )", "NOT (message MATCH_PHRASE 'error')", "括号内NOT操作符");

        // 4. 减号后直接跟引号（无空格）
        testSingleCase("test", "-'error'", "NOT (message MATCH_PHRASE 'error')", "减号直接跟引号");
    }

    private void testSingleCase(String module, String input, String expected, String description) {
        mockModuleConfig(module, createKeywordField("message", "MATCH_PHRASE"));
        LogSearchDTO dto = createLogSearchDTO(module, List.of(input));
        String result = keywordConditionBuilder.buildKeywords(dto);
        assertEquals(expected, result);
        System.out.println("✅ " + description + ": " + result);
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
