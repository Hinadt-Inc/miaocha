package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.KeywordConditionBuilder;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.Arrays;
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
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证生成的SQL符合Doris LIKE语法
            assertEquals("(message LIKE '%error%')", result);

            // 验证配置服务的正确调用
            verify(queryConfigValidationService)
                    .validateKeywordFieldPermissions("nginx", dto.getKeywordConditions());
            verify(queryConfigValidationService).getFieldSearchMethodMap("nginx");
        }

        @Test
        @DisplayName("MATCH_PHRASE搜索方法测试 - 验证精确短语匹配SQL生成符合Doris语法")
        void testMatchPhraseSearchMethod() {
            when(queryConfigValidationService.getFieldSearchMethodMap("app"))
                    .thenReturn(Map.of("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("app");
            KeywordConditionDTO condition = createKeywordCondition("content", "java exception");
            dto.setKeywordConditions(Arrays.asList(condition));

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
            KeywordConditionDTO condition = createKeywordCondition("tags", "urgent critical");
            dto.setKeywordConditions(Arrays.asList(condition));

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
            KeywordConditionDTO condition = createKeywordCondition("keywords", "payment success");
            dto.setKeywordConditions(Arrays.asList(condition));

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
            KeywordConditionDTO condition = createKeywordCondition(fieldName, searchValue);
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals(expectedResult, result);
        }
    }

    @Nested
    @DisplayName("多字段条件组合测试")
    class MultipleFieldConditionsTests {

        @Test
        @DisplayName("多字段不同搜索方法组合测试 - 验证AND连接和括号逻辑符合Doris语法")
        void testMultipleFieldsWithDifferentSearchMethods() {
            // Mock配置：不同字段使用不同搜索方法
            when(queryConfigValidationService.getFieldSearchMethodMap("mixed"))
                    .thenReturn(
                            Map.of(
                                    "message", "LIKE",
                                    "level", "MATCH_PHRASE",
                                    "service", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("mixed");

            KeywordConditionDTO condition1 = createKeywordCondition("message", "timeout");
            KeywordConditionDTO condition2 = createKeywordCondition("level", "ERROR");
            KeywordConditionDTO condition3 = createKeywordCondition("service", "order-service");

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
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            dto.setKeywordConditions(Arrays.asList(condition));

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
            KeywordConditionDTO condition1 = createKeywordCondition("message", "error");
            KeywordConditionDTO condition2 = createKeywordCondition("level", "ERROR");
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
                    createKeywordCondition("message", "'error' || 'warning'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证OR表达式解析结果符合Doris语法
            assertEquals("(message LIKE '%error%' OR message LIKE '%warning%')", result);
        }

        @Test
        @DisplayName("AND表达式解析测试 - 验证&&运算符处理符合Doris语法")
        void testAndExpressionParsing() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition("message", "'error' && 'critical'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证AND表达式解析结果符合Doris语法
            assertEquals("(message LIKE '%error%' AND message LIKE '%critical%')", result);
        }

        @Test
        @DisplayName("复杂嵌套表达式解析测试 - 验证括号和运算符优先级符合Doris语法")
        void testComplexNestedExpression() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition("message", "('error' || 'warning') && 'critical'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证复杂嵌套表达式解析结果符合Doris语法和运算符优先级
            // 根据 FieldExpressionParser 的实际逻辑，括号表达式会添加空格
            assertEquals(
                    "(( message LIKE '%error%' OR message LIKE '%warning%' ) AND message LIKE"
                            + " '%critical%')",
                    result);
        }

        @Test
        @DisplayName("MATCH_PHRASE复杂表达式测试 - 验证精确短语匹配的OR表达式")
        void testComplexExpressionWithMatchPhrase() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("content", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition("content", "'OutOfMemoryError' || 'StackOverflowError'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证MATCH_PHRASE方法的复杂表达式解析
            assertEquals(
                    "(content MATCH_PHRASE 'OutOfMemoryError' OR content MATCH_PHRASE"
                            + " 'StackOverflowError')",
                    result);
        }

        @Test
        @DisplayName("MATCH_ANY复杂表达式测试 - 验证任意匹配的AND表达式")
        void testComplexExpressionWithMatchAny() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("tags", "MATCH_ANY"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition("tags", "'production' && 'critical'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证MATCH_ANY方法的复杂表达式解析
            assertEquals("(tags MATCH_ANY 'production' AND tags MATCH_ANY 'critical')", result);
        }

        @Test
        @DisplayName("MATCH_ALL复杂表达式测试 - 验证全匹配的嵌套表达式")
        void testComplexExpressionWithMatchAll() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("keywords", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition =
                    createKeywordCondition("keywords", "('payment' || 'order') && 'success'");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证MATCH_ALL方法的复杂嵌套表达式解析
            assertEquals(
                    "(( keywords MATCH_ALL 'payment' OR keywords MATCH_ALL 'order' ) AND keywords"
                            + " MATCH_ALL 'success')",
                    result);
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

            // 验证不调用配置服务
            verifyNoInteractions(queryConfigValidationService);
        }

        @Test
        @DisplayName("空列表关键字条件测试 - 验证返回空字符串")
        void testEmptyListKeywordConditions() {
            LogSearchDTO dto = createLogSearchDTO("test");
            dto.setKeywordConditions(Arrays.asList());

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            assertEquals("", result);
            verifyNoInteractions(queryConfigValidationService);
        }

        @Test
        @DisplayName("字段名为空的条件测试 - 验证跳过处理")
        void testEmptyFieldNameCondition() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition1 = createKeywordCondition("", "error");
            KeywordConditionDTO condition2 = createKeywordCondition("message", "warning");
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
            KeywordConditionDTO condition1 = createKeywordCondition("message", "");
            KeywordConditionDTO condition2 = createKeywordCondition("message", "error");
            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证只处理有效的条件
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("字段权限验证失败测试 - 验证异常传播")
        void testFieldPermissionValidationFailure() {
            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("unauthorized_field", "test");
            dto.setKeywordConditions(Arrays.asList(condition));

            // Mock权限验证失败
            doThrow(new BusinessException(ErrorCode.KEYWORD_FIELD_NOT_ALLOWED, "字段不允许查询"))
                    .when(queryConfigValidationService)
                    .validateKeywordFieldPermissions("test", dto.getKeywordConditions());

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
        @DisplayName("不支持的搜索方法测试 - 验证异常处理")
        void testUnsupportedSearchMethod() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "INVALID_METHOD"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            dto.setKeywordConditions(Arrays.asList(condition));

            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> {
                                keywordConditionBuilder.buildKeywordConditions(dto);
                            });

            assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("搜索方法优先级测试")
    class SearchMethodPriorityTests {

        @Test
        @DisplayName("请求指定搜索方法优先级测试 - 验证覆盖配置中的默认方法")
        void testRequestSearchMethodOverridesConfig() {
            // Mock配置：message字段配置为LIKE
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            // 在请求中指定使用MATCH_PHRASE，应该覆盖配置中的LIKE
            condition.setSearchMethod("MATCH_PHRASE");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证使用了请求中指定的MATCH_PHRASE而不是配置中的LIKE
            assertEquals("(message MATCH_PHRASE 'error')", result);
        }

        @Test
        @DisplayName("使用配置默认方法测试 - 验证未指定时使用配置中的方法")
        void testUseConfigDefaultMethod() {
            // Mock配置：message字段配置为MATCH_ALL
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "MATCH_ALL"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            // 不指定searchMethod，应该使用配置中的默认方法
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证使用了配置中的MATCH_ALL方法
            assertEquals("(message MATCH_ALL 'error')", result);
        }

        @Test
        @DisplayName("字段未配置且请求未指定搜索方法测试 - 验证异常抛出")
        void testFieldNotConfiguredAndNoRequestMethod() {
            // Mock配置：不包含message字段的配置
            when(queryConfigValidationService.getFieldSearchMethodMap("test")).thenReturn(Map.of());

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            // 既没有配置也没有在请求中指定，应该抛出异常
            dto.setKeywordConditions(Arrays.asList(condition));

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
        @DisplayName("字段未配置但请求指定搜索方法测试 - 验证可以正常工作")
        void testFieldNotConfiguredButRequestSpecified() {
            // Mock配置：不包含message字段的配置
            when(queryConfigValidationService.getFieldSearchMethodMap("test")).thenReturn(Map.of());

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "error");
            // 在请求中指定搜索方法，即使配置中没有也应该可以工作
            condition.setSearchMethod("LIKE");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证使用了请求中指定的LIKE方法
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("混合优先级测试 - 验证多字段不同优先级处理")
        void testMixedPriorityHandling() {
            // Mock配置：只配置了message字段
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE", "level", "MATCH_PHRASE"));

            LogSearchDTO dto = createLogSearchDTO("test");

            // 第一个条件：使用配置中的默认方法
            KeywordConditionDTO condition1 = createKeywordCondition("message", "error");

            // 第二个条件：覆盖配置中的方法
            KeywordConditionDTO condition2 = createKeywordCondition("level", "ERROR");
            condition2.setSearchMethod("MATCH_ALL"); // 覆盖配置中的MATCH_PHRASE

            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证第一个使用配置默认，第二个使用请求指定
            assertEquals("((message LIKE '%error%') AND (level MATCH_ALL 'ERROR'))", result);
        }

        @Test
        @DisplayName("空白搜索方法处理测试 - 验证空格和null处理")
        void testBlankSearchMethodHandling() {
            // Mock配置
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");

            // 测试空字符串
            KeywordConditionDTO condition1 = createKeywordCondition("message", "error");
            condition1.setSearchMethod("");

            // 测试只有空格
            KeywordConditionDTO condition2 = createKeywordCondition("message", "warning");
            condition2.setSearchMethod("   ");

            dto.setKeywordConditions(Arrays.asList(condition1, condition2));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证都使用了配置中的默认方法LIKE
            assertEquals("((message LIKE '%error%') AND (message LIKE '%warning%'))", result);
        }
    }

    @Nested
    @DisplayName("字段名和搜索值去空格测试")
    class TrimWhitespaceTests {

        @Test
        @DisplayName("字段名前后空格处理测试 - 验证trim()功能")
        void testFieldNameTrimming() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("  message  ", "error");
            dto.setKeywordConditions(Arrays.asList(condition));

            String result = keywordConditionBuilder.buildKeywordConditions(dto);

            // 验证字段名的空格被正确处理
            assertEquals("(message LIKE '%error%')", result);
        }

        @Test
        @DisplayName("搜索值前后空格处理测试 - 验证trim()功能")
        void testSearchValueTrimming() {
            when(queryConfigValidationService.getFieldSearchMethodMap("test"))
                    .thenReturn(Map.of("message", "LIKE"));

            LogSearchDTO dto = createLogSearchDTO("test");
            KeywordConditionDTO condition = createKeywordCondition("message", "  error  ");
            dto.setKeywordConditions(Arrays.asList(condition));

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
    private KeywordConditionDTO createKeywordCondition(String fieldName, String searchValue) {
        KeywordConditionDTO condition = new KeywordConditionDTO();
        condition.setFieldName(fieldName);
        condition.setSearchValue(searchValue);
        return condition;
    }

    /** 创建带搜索方法的关键字条件DTO对象 */
    private KeywordConditionDTO createKeywordCondition(
            String fieldName, String searchValue, String searchMethod) {
        KeywordConditionDTO condition = new KeywordConditionDTO();
        condition.setFieldName(fieldName);
        condition.setSearchValue(searchValue);
        condition.setSearchMethod(searchMethod);
        return condition;
    }
}
