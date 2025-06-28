package com.hinadt.miaocha.mock.service.logsearch;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import io.qameta.allure.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * LogSearchDTOConverter单元测试
 *
 * <p>测试秒查系统中日志搜索DTO转换器的功能 验证装饰器模式实现的性能优化和正确性
 *
 * <p>测试覆盖范围： 1. 基本转换功能 - 不同字段类型的转换逻辑 2. 装饰器模式验证 - 装饰器的委托和透明性 3. 边界条件处理 - null值、空值、异常输入 4. 字段转换逻辑 -
 * 真实的点语法到bracket语法转换
 */
@Epic("秒查日志管理系统")
@Feature("SQL查询引擎")
@Story("查询DTO转换")
@DisplayName("日志搜索DTO转换器测试")
@Owner("开发团队")
class LogSearchDTOConverterTest {

    private LogSearchDTOConverter converter;
    private VariantFieldConverter variantFieldConverter;

    @BeforeEach
    void setUp() {
        // 使用真实的VariantFieldConverter，不使用Mock
        variantFieldConverter = new VariantFieldConverter();
        converter = new LogSearchDTOConverter(variantFieldConverter);
    }

    // ==================== 基本转换测试 ====================

    @Nested
    @DisplayName("基本转换功能测试")
    class BasicConversionTests {

        @Test
        @DisplayName("普通字段转换 - 期望：始终创建装饰器，普通字段保持不变")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证当DTO只包含普通字段时，转换器创建装饰器但字段内容不变")
        void testRegularFieldsConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time", "level", "service_name"));
            original.setWhereSqls(Arrays.asList("level = 'ERROR'", "host = 'server1'"));

            LogSearchDTO result = converter.convert(original);

            // 验证始终创建装饰器
            assertInstanceOf(LogSearchDTODecorator.class, result, "应始终创建装饰器");

            // 验证普通字段保持不变
            assertEquals(
                    Arrays.asList("host", "log_time", "level", "service_name"),
                    result.getFields(),
                    "普通字段应保持不变");
            assertEquals(
                    Arrays.asList("level = 'ERROR'", "host = 'server1'"),
                    result.getWhereSqls(),
                    "普通WHERE条件应保持不变");
        }

        @Test
        @DisplayName("点语法字段转换 - 期望：正确转换为bracket语法")
        void testDotSyntaxFieldConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId", "message.level", "host"));
            original.setWhereSqls(Arrays.asList("message.level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result, "应创建装饰器");

            // 验证点语法字段正确转换
            List<String> expectedFields =
                    Arrays.asList("message['logId']", "message['level']", "host");
            assertEquals(expectedFields, result.getFields(), "点语法字段应转换为bracket语法");

            // 验证WHERE条件中的点语法也被转换
            assertEquals(
                    Arrays.asList("message['level'] = 'ERROR'"),
                    result.getWhereSqls(),
                    "WHERE条件中的点语法应被转换");
        }

        @Test
        @DisplayName("混合字段转换 - 期望：普通字段和点语法字段混合处理")
        void testMixedFieldConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "message.logId", "level", "request.method"));

            LogSearchDTO result = converter.convert(original);

            List<String> expectedFields =
                    Arrays.asList("host", "message['logId']", "level", "request['method']");
            assertEquals(expectedFields, result.getFields(), "混合字段应正确处理");
        }

        @Test
        @DisplayName("关键字条件转换 - 期望：正确转换关键字条件中的字段名")
        void testKeywordConditionConversion() {
            LogSearchDTO original = createBasicDTO();

            KeywordConditionDTO condition1 = new KeywordConditionDTO();
            condition1.setFieldName("message.content");
            condition1.setSearchValue("error");
            condition1.setSearchMethod("contains");

            KeywordConditionDTO condition2 = new KeywordConditionDTO();
            condition2.setFieldName("level");
            condition2.setSearchValue("ERROR");
            condition2.setSearchMethod("equals");

            original.setKeywordConditions(Arrays.asList(condition1, condition2));

            LogSearchDTO result = converter.convert(original);

            List<KeywordConditionDTO> resultConditions = result.getKeywordConditions();
            assertEquals(2, resultConditions.size(), "应有2个关键字条件");

            // 验证点语法字段名被转换
            assertEquals(
                    "message['content']", resultConditions.get(0).getFieldName(), "点语法字段名应被转换");
            assertEquals("error", resultConditions.get(0).getSearchValue(), "搜索值应保持不变");

            // 验证普通字段名保持不变
            assertEquals("level", resultConditions.get(1).getFieldName(), "普通字段名应保持不变");
        }
    }

    // ==================== 边界条件处理测试 ====================

    @Nested
    @DisplayName("边界条件处理测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("null DTO处理 - 期望：null输入返回null")
        void testNullDTO() {
            LogSearchDTO result = converter.convert(null);
            assertNull(result, "null输入应返回null");
        }

        @Test
        @DisplayName("空字段列表处理 - 期望：创建装饰器但字段列表为空")
        void testEmptyFields() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Collections.emptyList());

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result, "应创建装饰器");
            assertTrue(result.getFields().isEmpty(), "字段列表应为空");
        }

        @Test
        @DisplayName("null字段列表处理 - 期望：创建装饰器但字段列表为null")
        void testNullFields() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(null);

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result, "应创建装饰器");
            assertNull(result.getFields(), "字段列表应为null");
        }

        @Test
        @DisplayName("包含null元素的字段列表 - 期望：正确处理包含null元素的列表")
        void testFieldListWithNullElements() {
            LogSearchDTO original = createBasicDTO();
            List<String> fieldsWithNull = new ArrayList<>();
            fieldsWithNull.add("host");
            fieldsWithNull.add(null);
            fieldsWithNull.add("message.level");
            original.setFields(fieldsWithNull);

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result, "应创建装饰器");
            List<String> resultFields = result.getFields();
            assertEquals(3, resultFields.size(), "应保持原有元素数量");
            assertEquals("host", resultFields.get(0), "普通字段应保持不变");
            assertNull(resultFields.get(1), "null元素应保持为null");
            assertEquals("message['level']", resultFields.get(2), "点语法字段应正确转换");
        }
    }

    // ==================== 装饰器行为测试 ====================

    @Nested
    @DisplayName("装饰器模式验证测试")
    class DecoratorPatternTests {

        @Test
        @DisplayName("装饰器委托验证 - 期望：装饰器正确委托所有方法调用到原始对象")
        void testDecoratorDelegation() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId"));
            original.setModule("test-module");
            original.setStartTime("2023-06-01 10:00:00");
            original.setEndTime("2023-06-01 11:00:00");
            original.setPageSize(50);
            original.setOffset(10);

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 测试装饰器的委托行为 - 所有getter方法应正确委托
            assertEquals(original.getModule(), decorator.getModule(), "模块名应正确委托");
            assertEquals(original.getStartTime(), decorator.getStartTime(), "开始时间应正确委托");
            assertEquals(original.getEndTime(), decorator.getEndTime(), "结束时间应正确委托");
            assertEquals(original.getPageSize(), decorator.getPageSize(), "页面大小应正确委托");
            assertEquals(original.getOffset(), decorator.getOffset(), "偏移量应正确委托");

            // 测试能获取原始对象
            assertSame(original, decorator.getOriginal(), "应能获取原始对象的引用");
        }

        @Test
        @DisplayName("装饰器透明性验证 - 期望：通过装饰器修改原始对象应正确传递")
        void testDecoratorTransparency() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId"));

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 修改装饰器中的属性
            decorator.setModule("new-module");
            decorator.setPageSize(100);
            decorator.setStartTime("2023-06-01 10:00:00");
            decorator.setEndTime("2023-06-01 11:00:00");

            // 原始对象应该被修改（透明性）
            assertEquals("new-module", original.getModule(), "模块名修改应传递到原始对象");
            assertEquals(100, original.getPageSize(), "页面大小修改应传递到原始对象");
            assertEquals("2023-06-01 10:00:00", original.getStartTime(), "开始时间修改应传递到原始对象");
            assertEquals("2023-06-01 11:00:00", original.getEndTime(), "结束时间修改应传递到原始对象");
        }

        @Test
        @DisplayName("装饰器字段覆盖验证 - 期望：装饰器的fields和whereSqls方法返回转换后的值")
        void testDecoratorFieldOverride() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId", "host"));
            original.setWhereSqls(Arrays.asList("message.level = 'ERROR'"));

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 验证装饰器返回转换后的值
            List<String> expectedFields = Arrays.asList("message['logId']", "host");
            assertEquals(expectedFields, decorator.getFields(), "装饰器应返回转换后的字段列表");
            assertEquals(
                    Arrays.asList("message['level'] = 'ERROR'"),
                    decorator.getWhereSqls(),
                    "装饰器应返回转换后的WHERE条件列表");

            // 验证原始对象未被修改
            assertEquals(
                    Arrays.asList("message.logId", "host"), original.getFields(), "原始对象的字段列表不应被修改");
            assertEquals(
                    Arrays.asList("message.level = 'ERROR'"),
                    original.getWhereSqls(),
                    "原始对象的WHERE条件列表不应被修改");
        }

        @Test
        @DisplayName("装饰器原始字段获取验证 - 期望：getOriginalFields返回原始字段列表")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证装饰器能正确返回原始字段列表，用于字段分布查询等场景")
        void testDecoratorGetOriginalFields() {
            LogSearchDTO original = createBasicDTO();
            List<String> originalFields = Arrays.asList("message.logId", "message.level", "host");
            original.setFields(originalFields);

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 验证装饰器方法返回值
            List<String> expectedConvertedFields =
                    Arrays.asList("message['logId']", "message['level']", "host");
            assertEquals(expectedConvertedFields, decorator.getFields(), "getFields()应返回转换后的字段");
            assertEquals(
                    originalFields, decorator.getOriginalFields(), "getOriginalFields()应返回原始字段");

            // 验证引用关系
            assertSame(originalFields, decorator.getOriginalFields(), "应返回原始字段列表的引用");
            assertNotSame(originalFields, decorator.getFields(), "转换后字段应是新的列表对象");
        }
    }

    // ==================== 字段转换逻辑测试 ====================

    @Nested
    @DisplayName("字段转换逻辑测试")
    class FieldConversionLogicTests {

        @Test
        @DisplayName("复杂嵌套字段转换 - 期望：正确处理多层嵌套字段")
        void testComplexNestedFieldConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(
                    Arrays.asList(
                            "request.headers.authorization",
                            "response.data.user.profile",
                            "system.metrics.cpu.usage"));

            LogSearchDTO result = converter.convert(original);

            List<String> expectedFields =
                    Arrays.asList(
                            "request['headers']['authorization']",
                            "response['data']['user']['profile']",
                            "system['metrics']['cpu']['usage']");
            assertEquals(expectedFields, result.getFields(), "复杂嵌套字段应正确转换");
        }

        @Test
        @DisplayName("WHERE条件中的字段转换 - 期望：正确转换WHERE条件中的点语法字段")
        void testWhereClauseFieldConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setWhereSqls(
                    Arrays.asList(
                            "message.level = 'ERROR'",
                            "request.method = 'POST'",
                            "level = 'INFO'" // 普通字段
                            ));

            LogSearchDTO result = converter.convert(original);

            List<String> expectedWhereSqls =
                    Arrays.asList(
                            "message['level'] = 'ERROR'",
                            "request['method'] = 'POST'",
                            "level = 'INFO'");
            assertEquals(expectedWhereSqls, result.getWhereSqls(), "WHERE条件中的字段应正确转换");
        }

        @ParameterizedTest
        @DisplayName("不同点语法字段模式测试 - 期望：正确处理各种点语法字段命名模式")
        @ValueSource(
                strings = {
                    "request.method",
                    "business.order.status",
                    "system.metrics.cpu.usage",
                    "error.exception.type",
                    "user.profile.preferences"
                })
        void testVariousFieldPatterns(String dotSyntaxField) {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList(dotSyntaxField));

            LogSearchDTO result = converter.convert(original);

            List<String> resultFields = result.getFields();
            assertEquals(1, resultFields.size(), "应只有一个字段");

            String convertedField = resultFields.get(0);
            assertTrue(convertedField.contains("['"), "应包含bracket语法");
            assertFalse(convertedField.contains("AS"), "不应包含AS别名");
            // 验证字段名被正确转换（将点替换为bracket语法）
            String expectedConverted =
                    dotSyntaxField
                            .replaceAll("\\.", "']['")
                            .replaceFirst("^", "")
                            .replaceFirst("$", "");
            expectedConverted =
                    dotSyntaxField.substring(0, dotSyntaxField.indexOf('.'))
                            + "['"
                            + dotSyntaxField
                                    .substring(dotSyntaxField.indexOf('.') + 1)
                                    .replaceAll("\\.", "']['")
                            + "']";
            assertEquals(expectedConverted, convertedField, "字段应正确转换为bracket语法");
        }
    }

    private LogSearchDTO createBasicDTO() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule("test-module");
        dto.setStartTime("2023-06-01 00:00:00");
        dto.setEndTime("2023-06-01 23:59:59");
        dto.setPageSize(20);
        dto.setOffset(0);
        return dto;
    }
}
