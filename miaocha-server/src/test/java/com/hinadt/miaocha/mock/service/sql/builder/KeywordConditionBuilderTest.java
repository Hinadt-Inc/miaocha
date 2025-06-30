package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.KeywordConditionBuilder;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 关键字条件构建器测试
 *
 * <p>测试配置驱动的关键字条件构建核心逻辑，验证生成的SQL符合Doris语法规范
 *
 * <p>支持多字段查询，字段间使用AND连接
 */
@DisplayName("关键字条件构建器测试")
class KeywordConditionBuilderTest {

    @InjectMocks private KeywordConditionBuilder keywordConditionBuilder;

    @Mock private QueryConfigValidationService queryConfigValidationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("配置驱动的搜索方法测试")
    class ConfigDrivenSearchMethodTests {

        @Test
        @DisplayName("LIKE搜索方法测试 - 验证LIKE搜索SQL生成符合Doris语法")
        void testLikeSearchMethod() {
            // Mock配置：message字段使用LIKE搜索
            when(queryConfigValidationService.getFieldSearchMethodMap("nginx"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("nginx");
            KeywordConditionDTO condition = createKeywordCondition(List.of("message"), "error");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证生成的SQL符合Doris LIKE语法
            assertEquals("(message LIKE '%error%')", result);

            // 验证配置服务的正确调用
            verify(queryConfigValidationService)
                    .validateKeywordFieldPermissions(dto, dto.getKeywordConditions());
            verify(queryConfigValidationService).getFieldSearchMethodMap("nginx");
        }

        @Test
        @DisplayName("MATCH_PHRASE搜索方法测试 - 验证精确短语匹配SQL生成符合Doris语法")
        void testMatchPhraseSearchMethod() {
            when(queryConfigValidationService.getFieldSearchMethodMap("app"))
                    .thenReturn(Map.of("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("app");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("content"), "java exception");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证生成的SQL符合Doris MATCH_PHRASE语法
            assertEquals("(content MATCH_PHRASE 'java exception')", result);
        }

        @Test
        @DisplayName("MATCH_ANY搜索方法测试 - 验证任意词匹配SQL生成符合Doris语法")
        void testMatchAnySearchMethod() {
            when(queryConfigValidationService.getFieldSearchMethodMap("system"))
                    .thenReturn(Map.of("tags", "MATCH_ANY"));

            LogSearchDTO dto = createLogSearchDTO("system");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("tags"), "urgent critical");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证生成的SQL符合Doris MATCH_ANY语法
            assertEquals("(tags MATCH_ANY 'urgent critical')", result);
        }

        @Test
        @DisplayName("MATCH_ALL搜索方法测试 - 验证全词匹配SQL生成符合Doris语法")
        void testMatchAllSearchMethod() {
            when(queryConfigValidationService.getFieldSearchMethodMap("business"))
                    .thenReturn(Map.of("keywords", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("business");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("keywords"), "payment success");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证生成的SQL符合Doris MATCH_ALL语法
            assertEquals("(keywords MATCH_ALL 'payment success')", result);
        }

        @ParameterizedTest
        @DisplayName("不同模块不同搜索方法配置测试 - 验证配置驱动的灵活性")
        @CsvSource({
            "nginx, message, LIKE, error, '(message LIKE ''%error%'')'",
            "app, content, MATCH_PHRASE, 'NullPointerException', '(content MATCH_PHRASE"
                    + " ''NullPointerException'')'",
            "system, tags, MATCH_ANY, urgent, '(tags MATCH_ANY ''urgent'')'",
            "business, keywords, MATCH_ALL, payment, '(keywords MATCH_ALL ''payment'')'"
        })
        void testDifferentModuleConfigurations(
                String module,
                String fieldName,
                String searchMethod,
                String searchValue,
                String expectedResult) {
            when(queryConfigValidationService.getFieldSearchMethodMap(module))
                    .thenReturn(Map.of(fieldName, searchMethod));

            LogSearchDTO dto = createLogSearchDTO(module);
            KeywordConditionDTO condition = createKeywordCondition(List.of(fieldName), searchValue);
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals(expectedResult, result);
        }
    }

    @Nested
    @DisplayName("多字段功能测试")
    class MultipleFieldsTests {

        @Test
        @DisplayName("单个条件多字段测试 - 验证字段间OR连接")
        void testSingleConditionWithMultipleFields() {
            // Mock配置：不同字段使用不同搜索方法
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "content", "MATCH_PHRASE",
                                    "tags", "MATCH_ANY"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(Arrays.asList("message", "content", "tags"), "error");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证多字段间用OR连接，每个字段使用各自配置的搜索方法
            String expected =
                    "((message LIKE '%error%') OR (content MATCH_PHRASE 'error') OR (tags"
                            + " MATCH_ANY 'error'))";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("多个条件多字段测试 - 验证条件间AND连接，字段间OR连接")
        void testMultipleConditionsWithMultipleFields() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "MATCH_PHRASE",
                                    "service", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("test");

            KeywordConditionDTO condition1 =
                    createKeywordCondition(Arrays.asList("message", "level"), "error");
            KeywordConditionDTO condition2 =
                    createKeywordCondition(List.of("service"), "user-service");

            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证多个条件间用AND连接，字段间用OR连接，外层需要括号
            String expected =
                    "(((message LIKE '%error%') OR (level MATCH_PHRASE 'error')) AND (service"
                            + " MATCH_ALL 'user-service'))";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("字段名去空格测试 - 验证trim()功能")
        void testFieldNameTrimming() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition(List.of("  message  "), "error");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证字段名的空格被正确处理
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("多字段复杂表达式测试 - 验证多字段的复杂表达式处理")
        void testMultipleFieldsComplexExpression() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(
                            Arrays.asList("message", "level"), "'error' || 'warning'");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证每个字段都应用相同的复杂表达式，字段间用OR连接
            String expected =
                    "((message LIKE '%error%' OR message LIKE '%warning%') OR (level MATCH_PHRASE"
                            + " 'error' OR level MATCH_PHRASE 'warning'))";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("多字段嵌套复杂表达式测试 - 验证多字段处理嵌套括号和多层运算符")
        void testMultipleFieldsNestedComplexExpression() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "content", "MATCH_PHRASE",
                                    "status", "MATCH_ANY"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(
                            Arrays.asList("message", "content", "status"),
                            "('critical' || 'error') && ('system' || 'application')");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证复杂嵌套表达式在多字段间的正确应用，字段间用OR连接
            String expected =
                    "((( message LIKE '%critical%' OR message LIKE '%error%' ) AND ( message LIKE"
                        + " '%system%' OR message LIKE '%application%' )) OR (( content"
                        + " MATCH_PHRASE 'critical' OR content MATCH_PHRASE 'error' ) AND ( content"
                        + " MATCH_PHRASE 'system' OR content MATCH_PHRASE 'application' )) OR (("
                        + " status MATCH_ANY 'critical' OR status MATCH_ANY 'error' ) AND ( status"
                        + " MATCH_ANY 'system' OR status MATCH_ANY 'application' )))";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("多字段混合运算符表达式测试 - 验证OR和AND混合运算符的优先级处理")
        void testMultipleFieldsMixedOperatorsExpression() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(
                            Arrays.asList("message", "level"),
                            "'error' && 'critical' || 'warning' && 'info'");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证混合运算符的优先级处理：AND优先级高于OR，字段间用OR连接
            String expected =
                    "((message LIKE '%error%' AND message LIKE '%critical%' OR message LIKE"
                            + " '%warning%' AND message LIKE '%info%') OR"
                            + " (level MATCH_PHRASE 'error' AND level MATCH_PHRASE 'critical'"
                            + " OR level MATCH_PHRASE 'warning' AND level MATCH_PHRASE 'info'))";
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("单字段条件组合测试")
    class SingleFieldConditionsTests {

        @Test
        @DisplayName("多个单字段条件测试 - 验证AND连接和括号逻辑符合Doris语法")
        void testMultipleSingleFieldConditions() {
            // Mock配置：不同字段使用不同搜索方法
            when(queryConfigValidationService.getFieldSearchMethodMap("mixed"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "MATCH_PHRASE",
                                    "service", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("mixed");

            KeywordConditionDTO condition1 = createKeywordCondition(List.of("message"), "timeout");
            KeywordConditionDTO condition2 = createKeywordCondition(List.of("level"), "ERROR");
            KeywordConditionDTO condition3 =
                    createKeywordCondition(List.of("service"), "order-service");

            dto.setKeywordConditions(Arrays.asList(condition1, condition2, condition3));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证多字段条件的AND组合和外层括号符合Doris语法
            String expected =
                    "((message LIKE '%timeout%') AND (level MATCH_PHRASE 'ERROR') AND (service"
                            + " MATCH_ALL 'order-service'))";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("单字段条件测试 - 验证单字段不加外层括号")
        void testSingleFieldCondition() {
            when(queryConfigValidationService.getFieldSearchMethodMap("single"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("single");
            KeywordConditionDTO condition = createKeywordCondition(List.of("message"), "error");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 根据业务逻辑，单字段条件不应该有外层括号
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("两个字段条件测试 - 验证两个字段需要外层括号")
        void testTwoFieldConditions() {
            when(queryConfigValidationService.getFieldSearchMethodMap("two"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("two");
            KeywordConditionDTO condition1 = createKeywordCondition(List.of("message"), "error");
            KeywordConditionDTO condition2 = createKeywordCondition(List.of("level"), "ERROR");
            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 根据业务逻辑，多字段条件需要外层括号
            String expected = "((message LIKE '%error%') AND (level LIKE '%ERROR%'))";
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("复杂表达式解析测试")
    class ComplexExpressionTests {

        @Test
        @DisplayName("OR表达式解析测试 - 验证||运算符处理符合Doris语法")
        void testOrExpressionParsing() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("message"), "'error' || 'warning'");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals("(message LIKE '%error%' OR message LIKE '%warning%')", result);
        }

        @Test
        @DisplayName("AND表达式解析测试 - 验证&&运算符处理符合Doris语法")
        void testAndExpressionParsing() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("message"), "'error' && 'critical'");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals("(message LIKE '%error%' AND message LIKE '%critical%')", result);
        }

        @Test
        @DisplayName("复杂嵌套表达式解析测试 - 验证括号和运算符优先级符合Doris语法")
        void testComplexNestedExpression() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(
                            List.of("message"), "('error' || 'warning') && 'critical'");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            String expected =
                    "(( message LIKE '%error%' OR message LIKE '%warning%' ) AND message LIKE"
                            + " '%critical%')";
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("边界条件和异常测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("空关键字条件列表测试 - 验证返回空字符串")
        void testEmptyKeywordConditions() {
            LogSearchDTO dto = createLogSearchDTO("test");
            dto.setKeywordConditions(null);

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals("", result);
        }

        @Test
        @DisplayName("空列表关键字条件测试 - 验证返回空字符串")
        void testEmptyListKeywordConditions() {
            LogSearchDTO dto = createLogSearchDTO("test");
            dto.setKeywordConditions(Collections.emptyList());

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals("", result);
        }

        @Test
        @DisplayName("字段名列表为空的条件测试 - 验证跳过处理")
        void testEmptyFieldNamesCondition() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition1 =
                    createKeywordCondition(Collections.emptyList(), "error");
            KeywordConditionDTO condition2 = createKeywordCondition(List.of("message"), "warning");
            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证只处理有效的条件
            assertEquals("(message LIKE '%warning%')", result);
        }

        @Test
        @DisplayName("搜索值为空的条件测试 - 验证跳过处理")
        void testEmptySearchValueCondition() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition1 = createKeywordCondition(List.of("message"), "");
            KeywordConditionDTO condition2 = createKeywordCondition(List.of("message"), "error");
            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证只处理有效的条件
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("字段权限验证失败测试 - 验证异常传播")
        void testFieldPermissionValidationFailure() {
            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(List.of("unauthorized_field"), "test");
            dto.setKeywordConditions(List.of(condition));

            // Mock权限验证失败
            doThrow(new BusinessException(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED, "字段不允许查询"))
                    .when(queryConfigValidationService)
                    .validateKeywordFieldPermissions(dto, dto.getKeywordConditions());

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                keywordConditionBuilder.buildKeywordConditions(dto);
                            });

            assertEquals(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED, exception.getErrorCode());
            assertEquals("字段不允许查询", exception.getMessage());
        }

        @Test
        @DisplayName("字段未配置搜索方法测试 - 验证异常抛出")
        void testFieldNotConfigured() {
            // Mock配置：不包含message字段的配置
            when(queryConfigValidationService.getFieldSearchMethodMap("test")).thenReturn(Map.of());

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition(List.of("message"), "error");
            dto.setKeywordConditions(List.of(condition));

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                keywordConditionBuilder.buildKeywordConditions(dto);
                            });

            assertEquals(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("未配置默认搜索方法"));
        }

        @Test
        @DisplayName("不支持的搜索方法测试 - 验证异常处理")
        void testUnsupportedSearchMethod() {
            // Mock配置：使用不支持的搜索方法
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "INVALID_METHOD"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition(List.of("message"), "error");
            dto.setKeywordConditions(List.of(condition));

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                keywordConditionBuilder.buildKeywordConditions(dto);
                            });

            assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        }

        @Test
        @DisplayName("混合字段配置测试 - 验证部分字段有配置部分字段无配置的情况")
        void testMixedFieldConfiguration() {
            // Mock配置：只配置部分字段
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition(Arrays.asList("message", "unconfigured_field"), "error");
            dto.setKeywordConditions(List.of(condition));

            // 应该抛出字段未配置异常
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                keywordConditionBuilder.buildKeywordConditions(dto);
                            });

            assertEquals(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("unconfigured_field"));
            assertTrue(exception.getMessage().contains("未配置默认搜索方法"));
        }
    }

    @Nested
    @DisplayName("搜索值去空格测试")
    class TrimWhitespaceTests {

        @Test
        @DisplayName("搜索值前后空格处理测试 - 验证trim()功能")
        void testSearchValueTrimming() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition(List.of("message"), "  error  ");
            dto.setKeywordConditions(List.of(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证搜索值的空格被正确处理
            assertEquals("(message LIKE '%error%')", result);
        }
    }

    // ==================== 辅助方法 ====================

    /** 创建基础的LogSearchDTO对象 */
    private LogSearchDTO createLogSearchDTO(String module) {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule(module);
        return dto;
    }

    /** 创建关键字条件DTO对象 */
    private KeywordConditionDTO createKeywordCondition(
            List<String> fieldNames, String searchValue) {
        KeywordConditionDTO condition = new KeywordConditionDTO();
        condition.setFieldNames(fieldNames);
        condition.setSearchValue(searchValue);
        return condition;
    }
}
