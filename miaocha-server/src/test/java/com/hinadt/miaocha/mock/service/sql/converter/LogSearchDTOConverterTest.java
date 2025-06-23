package com.hinadt.miaocha.mock.service.sql.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTODecorator;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * LogSearchDTOConverter单元测试
 *
 * <p>测试秒查系统中日志搜索DTO转换器的功能 验证装饰器模式实现的性能优化和正确性
 *
 * <p>测试覆盖范围： 1. 基本转换功能 - 需要转换和不需要转换的场景 2. 装饰器模式验证 - 装饰器的委托和透明性 3. 性能优化验证 - 避免不必要的对象创建 4. 边界条件处理 -
 * null值、空值、异常输入 5. 复杂场景模拟 - 真实业务场景测试 6. 内存管理验证 - 大量转换的内存使用 7. 并发安全性测试 - 多线程环境下的安全性
 */
@Epic("秒查日志管理系统")
@Feature("SQL查询引擎")
@Story("查询DTO转换")
@DisplayName("日志搜索DTO转换器测试")
@Owner("开发团队")
class LogSearchDTOConverterTest {

    private LogSearchDTOConverter converter;

    @Mock private VariantFieldConverter variantFieldConverter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        converter = new LogSearchDTOConverter(variantFieldConverter);
    }

    // ==================== 基本转换测试 ====================

    @Nested
    @DisplayName("基本转换功能测试")
    class BasicConversionTests {

        @Test
        @DisplayName("无需转换的DTO - 期望：返回原始对象，不创建装饰器，不调用转换器")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证当DTO不包含variant字段时，转换器正确识别并跳过转换过程")
        void testNoConversionNeeded() {
            LogSearchDTO original =
                    Allure.step(
                            "创建不包含variant字段的DTO",
                            () -> {
                                LogSearchDTO dto = createBasicDTO();
                                dto.setFields(Arrays.asList("host", "log_time", "level"));
                                dto.setWhereSqls(
                                        Arrays.asList("level = 'ERROR'", "host = 'server1'"));

                                Allure.parameter("字段列表", dto.getFields());
                                Allure.parameter("WHERE条件", dto.getWhereSqls());
                                return dto;
                            });

            LogSearchDTO result =
                    Allure.step(
                            "执行DTO转换",
                            () -> {
                                return converter.convert(original);
                            });

            Allure.step(
                    "验证转换结果",
                    () -> {
                        // 应该返回原始对象，不创建装饰器
                        assertSame(original, result, "不需要转换时应返回原始对象");
                        verifyNoInteractions(variantFieldConverter);

                        Allure.parameter("是否为原始对象", original == result);
                        Allure.attachment("验证结果", "返回了原始对象，未进行不必要的转换");
                    });
        }

        @Test
        @DisplayName("仅需要字段转换的DTO - 期望：返回装饰器，只转换fields，whereSqls保持原样")
        void testFieldConversionOnly() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId", "host", "log_time"));
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "message['logId'] AS 'message.logId'", "host", "log_time"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            // 应该返回装饰器
            assertInstanceOf(LogSearchDTODecorator.class, result, "需要转换时应返回装饰器");
            assertEquals(
                    Arrays.asList("message['logId'] AS 'message.logId'", "host", "log_time"),
                    result.getFields(),
                    "字段应正确转换");
            assertEquals(Arrays.asList("level = 'ERROR'"), result.getWhereSqls(), "WHERE条件应保持不变");

            // 其他字段应该保持不变
            assertEquals(original.getDatasourceId(), result.getDatasourceId());
            assertEquals(original.getModule(), result.getModule());
        }

        @Test
        @DisplayName("仅需要WHERE条件转换的DTO - 期望：返回装饰器，只转换whereSqls，fields保持原样")
        void testWhereConversionOnly() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time"));
            original.setWhereSqls(Arrays.asList("message.logId = 'test'", "level = 'ERROR'"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(Arrays.asList("host", "log_time"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("message['logId'] = 'test'", "level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            // 应该返回装饰器
            assertInstanceOf(LogSearchDTODecorator.class, result, "需要转换时应返回装饰器");
            assertEquals(Arrays.asList("host", "log_time"), result.getFields(), "字段应保持不变");
            assertEquals(
                    Arrays.asList("message['logId'] = 'test'", "level = 'ERROR'"),
                    result.getWhereSqls(),
                    "WHERE条件应正确转换");
        }

        @Test
        @DisplayName("同时需要字段和WHERE转换的DTO - 期望：返回装饰器，同时转换fields和whereSqls")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证当DTO同时包含需要转换的字段和WHERE条件时，转换器能够正确处理")
        void testBothFieldAndWhereConversion() {
            LogSearchDTO original =
                    Allure.step(
                            "创建包含variant字段和WHERE条件的DTO",
                            () -> {
                                LogSearchDTO dto = createBasicDTO();
                                dto.setFields(Arrays.asList("message.level", "message.logId"));
                                dto.setWhereSqls(Arrays.asList("message.service = 'api'"));

                                Allure.parameter("原始字段", dto.getFields());
                                Allure.parameter("原始WHERE条件", dto.getWhereSqls());
                                return dto;
                            });

            Allure.step(
                    "配置Mock转换器行为",
                    () -> {
                        // Mock转换器行为
                        when(variantFieldConverter.convertSelectFields(any()))
                                .thenReturn(
                                        Arrays.asList(
                                                "message['level'] AS 'message.level'",
                                                "message['logId'] AS 'message.logId'"));
                        when(variantFieldConverter.convertWhereClauses(any()))
                                .thenReturn(Arrays.asList("message['service'] = 'api'"));

                        Allure.parameter(
                                "Mock字段转换", "message.level → message['level'] AS 'message.level'");
                        Allure.parameter(
                                "Mock WHERE转换",
                                "message.service = 'api' → message['service'] = 'api'");
                    });

            LogSearchDTO result =
                    Allure.step(
                            "执行DTO转换",
                            () -> {
                                return converter.convert(original);
                            });

            Allure.step(
                    "验证转换结果",
                    () -> {
                        assertInstanceOf(LogSearchDTODecorator.class, result, "需要转换时应返回装饰器");
                        assertEquals(
                                Arrays.asList(
                                        "message['level'] AS 'message.level'",
                                        "message['logId'] AS 'message.logId'"),
                                result.getFields(),
                                "字段应正确转换");
                        assertEquals(
                                Arrays.asList("message['service'] = 'api'"),
                                result.getWhereSqls(),
                                "WHERE条件应正确转换");

                        Allure.parameter("转换后字段", result.getFields());
                        Allure.parameter("转换后WHERE条件", result.getWhereSqls());
                        Allure.attachment("转换结果", "字段和WHERE条件都已正确转换为bracket语法，返回了装饰器对象");
                    });
        }

        @Test
        @DisplayName("复杂字段混合转换 - 期望：正确处理包含多种字段类型的复杂场景")
        void testComplexMixedFieldConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(
                    Arrays.asList(
                            "log_time",
                            "host",
                            "level", // 普通字段
                            "message.logId",
                            "message.service", // 简单variant字段
                            "message.marker.duration",
                            "message.trace.spanId" // 嵌套variant字段
                            ));
            original.setWhereSqls(
                    Arrays.asList(
                            "level = 'ERROR'", // 普通条件
                            "message.service = 'api'", // 简单variant条件
                            "message.marker.duration > 100" // 嵌套variant条件
                            ));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "log_time",
                                    "host",
                                    "level",
                                    "message['logId'] AS 'message.logId'",
                                    "message['service'] AS 'message.service'",
                                    "message['marker']['duration'] AS 'message.marker.duration'",
                                    "message['trace']['spanId'] AS 'message.trace.spanId'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "level = 'ERROR'",
                                    "message['service'] = 'api'",
                                    "message['marker']['duration'] > 100"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);
            List<String> resultFields = result.getFields();
            assertEquals(7, resultFields.size(), "字段数量应保持一致");
            assertEquals("log_time", resultFields.get(0), "普通字段应保持不变");
            assertEquals(
                    "message['logId'] AS 'message.logId'", resultFields.get(3), "简单variant字段应正确转换");
            assertEquals(
                    "message['marker']['duration'] AS 'message.marker.duration'",
                    resultFields.get(5),
                    "嵌套variant字段应正确转换");
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件处理测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("null DTO处理 - 期望：null输入返回null")
        void testNullDTO() {
            LogSearchDTO result = converter.convert(null);

            assertNull(result, "null输入应返回null");
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("空字段列表处理 - 期望：空列表不需要转换，返回原始对象")
        void testEmptyFields() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Collections.emptyList());
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            // 空列表不需要转换，应该返回原始对象
            assertSame(original, result, "空字段列表应返回原始对象");
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("null字段列表处理 - 期望：null字段列表不需要转换，返回原始对象")
        void testNullFields() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(null);
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            // null列表不需要转换，应该返回原始对象
            assertSame(original, result, "null字段列表应返回原始对象");
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("空WHERE条件列表处理 - 期望：空WHERE列表不需要转换，返回原始对象")
        void testEmptyWhereClauses() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time"));
            original.setWhereSqls(Collections.emptyList());

            LogSearchDTO result = converter.convert(original);

            // 空列表不需要转换，应该返回原始对象
            assertSame(original, result, "空WHERE条件列表应返回原始对象");
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("null WHERE条件列表处理 - 期望：null WHERE列表不需要转换，返回原始对象")
        void testNullWhereClauses() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time"));
            original.setWhereSqls(null);

            LogSearchDTO result = converter.convert(original);

            assertSame(original, result, "null WHERE条件列表应返回原始对象");
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("包含null元素的字段列表 - 期望：正确处理包含null元素的列表")
        void testFieldListWithNullElements() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.level", null, "host", null));
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "message['level'] AS 'message.level'", null, "host", null));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("level = 'ERROR'"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);
            List<String> resultFields = result.getFields();
            assertEquals(4, resultFields.size(), "包含null元素的列表大小应保持一致");
            assertEquals("message['level'] AS 'message.level'", resultFields.get(0));
            assertNull(resultFields.get(1), "null元素应保持为null");
            assertEquals("host", resultFields.get(2));
            assertNull(resultFields.get(3), "null元素应保持为null");
        }

        @Test
        @DisplayName("大量字段处理 - 期望：高效处理大量字段而不出现性能问题")
        void testLargeFieldList() {
            LogSearchDTO original = createBasicDTO();
            List<String> largeFieldList = new ArrayList<>();
            List<String> expectedConvertedList = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                largeFieldList.add("message.field" + i);
                expectedConvertedList.add("message['field" + i + "'] AS 'message.field" + i + "'");
            }

            original.setFields(largeFieldList);
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(expectedConvertedList);
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("level = 'ERROR'"));

            long startTime = System.currentTimeMillis();
            LogSearchDTO result = converter.convert(original);
            long endTime = System.currentTimeMillis();

            assertInstanceOf(LogSearchDTODecorator.class, result);
            assertEquals(1000, result.getFields().size(), "大量字段处理后数量应保持一致");
            assertTrue(
                    endTime - startTime < 100,
                    "1000个字段处理应在100ms内完成，实际用时：" + (endTime - startTime) + "ms");
        }
    }

    // ==================== TOPN字段转换测试 ====================

    @Nested
    @DisplayName("TOPN字段转换测试")
    class TopnFieldConversionTests {

        @Test
        @DisplayName("TOPN字段转换 - 期望：委托给VariantFieldConverter进行转换")
        void testConvertTopnField() {
            String input = "message.level";
            String expected = "message['level']";

            when(variantFieldConverter.convertTopnField(input)).thenReturn(expected);

            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "TOPN字段应正确转换");
            verify(variantFieldConverter).convertTopnField(input);
        }

        @Test
        @DisplayName("嵌套TOPN字段转换 - 期望：正确处理多层嵌套的TOPN字段")
        void testNestedTopnFieldConversion() {
            String input = "message.marker.duration";
            String expected = "message['marker']['duration']";

            when(variantFieldConverter.convertTopnField(input)).thenReturn(expected);

            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "嵌套TOPN字段应正确转换");
            verify(variantFieldConverter).convertTopnField(input);
        }

        @Test
        @DisplayName("普通TOPN字段转换 - 期望：普通字段保持不变")
        void testRegularTopnFieldConversion() {
            String input = "level";
            String expected = "level";

            when(variantFieldConverter.convertTopnField(input)).thenReturn(expected);

            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "普通TOPN字段应保持不变");
            verify(variantFieldConverter).convertTopnField(input);
        }

        @Test
        @DisplayName("null TOPN字段处理 - 期望：null输入正确处理")
        void testNullTopnField() {
            when(variantFieldConverter.convertTopnField(null)).thenReturn(null);

            String result = converter.convertTopnField(null);

            assertNull(result, "null TOPN字段应返回null");
            verify(variantFieldConverter).convertTopnField(null);
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
            original.setWhereSqls(Arrays.asList("message.level = 'ERROR'"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(Arrays.asList("message['logId'] AS 'message.logId'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("message['level'] = 'ERROR'"));

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 测试装饰器的委托行为 - 所有getter方法应正确委托
            assertEquals(original.getDatasourceId(), decorator.getDatasourceId(), "数据源ID应正确委托");
            assertEquals(original.getModule(), decorator.getModule(), "模块名应正确委托");
            assertEquals(original.getKeywords(), decorator.getKeywords(), "关键词应正确委托");
            assertEquals(original.getStartTime(), decorator.getStartTime(), "开始时间应正确委托");
            assertEquals(original.getEndTime(), decorator.getEndTime(), "结束时间应正确委托");
            assertEquals(original.getPageSize(), decorator.getPageSize(), "页面大小应正确委托");
            assertEquals(original.getOffset(), decorator.getOffset(), "偏移量应正确委托");
            assertEquals(original.getTimeRange(), decorator.getTimeRange(), "时间范围应正确委托");
            assertEquals(original.getTimeGrouping(), decorator.getTimeGrouping(), "时间分组应正确委托");

            // 测试能获取原始对象
            assertSame(original, decorator.getOriginal(), "应能获取原始对象的引用");
        }

        @Test
        @DisplayName("装饰器透明性验证 - 期望：通过装饰器修改原始对象应正确传递")
        void testDecoratorTransparency() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(Arrays.asList("message['logId'] AS 'message.logId'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(original.getWhereSqls());

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

            List<String> convertedFields =
                    Arrays.asList("message['logId'] AS 'message.logId'", "host");
            List<String> convertedWhere = Arrays.asList("message['level'] = 'ERROR'");

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any())).thenReturn(convertedFields);
            when(variantFieldConverter.convertWhereClauses(any())).thenReturn(convertedWhere);

            LogSearchDTODecorator decorator = (LogSearchDTODecorator) converter.convert(original);

            // 装饰器应返回转换后的值
            assertEquals(convertedFields, decorator.getFields(), "装饰器应返回转换后的字段列表");
            assertEquals(convertedWhere, decorator.getWhereSqls(), "装饰器应返回转换后的WHERE条件");

            // 原始对象的值不应改变
            assertEquals(
                    Arrays.asList("message.logId", "host"), original.getFields(), "原始对象的字段列表不应改变");
            assertEquals(
                    Arrays.asList("message.level = 'ERROR'"),
                    original.getWhereSqls(),
                    "原始对象的WHERE条件不应改变");
        }
    }

    // ==================== 性能优化测试 ====================

    @Nested
    @DisplayName("性能优化验证测试")
    class PerformanceOptimizationTests {

        @Test
        @DisplayName("性能优化：避免不必要的转换 - 期望：不需要转换时不创建装饰器")
        void testAvoidUnnecessaryConversion() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time", "level"));
            original.setWhereSqls(Arrays.asList("level = 'ERROR'", "host = 'server1'"));

            LogSearchDTO result = converter.convert(original);

            // 应该返回原始对象，避免创建装饰器
            assertSame(original, result, "不需要转换时应返回原始对象，避免创建装饰器");

            // 不应该调用转换器（因为不需要转换）
            verifyNoInteractions(variantFieldConverter);
        }

        @Test
        @DisplayName("性能优化：返回原始列表对象避免复制 - 期望：当转换结果与原始相同时返回原始对象")
        void testAvoidUnnecessaryCopy() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("host", "log_time"));
            original.setWhereSqls(Arrays.asList("level = 'ERROR'"));

            // Mock转换器返回相同的对象引用（表示不需要转换）
            when(variantFieldConverter.convertSelectFields(original.getFields()))
                    .thenReturn(original.getFields());
            when(variantFieldConverter.convertWhereClauses(original.getWhereSqls()))
                    .thenReturn(original.getWhereSqls());

            LogSearchDTO result = converter.convert(original);

            // 应该返回原始对象
            assertSame(original, result, "转换结果与原始相同时应返回原始对象");
        }

        @Test
        @DisplayName("批量转换性能 - 期望：批量转换多个DTO时保持高性能")
        void testBatchConversionPerformance() {
            List<LogSearchDTO> dtoList = new ArrayList<>();

            // 创建1000个DTO对象
            for (int i = 0; i < 1000; i++) {
                LogSearchDTO dto = createBasicDTO();
                dto.setFields(Arrays.asList("message.field" + i, "host"));
                dto.setWhereSqls(Arrays.asList("message.level" + i + " = 'ERROR'"));
                dtoList.add(dto);
            }

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenAnswer(
                            invocation -> {
                                List<String> input = invocation.getArgument(0);
                                List<String> result = new ArrayList<>();
                                for (String field : input) {
                                    if (field.contains(".")) {
                                        result.add(
                                                field.replace(".", "['").replace(".", "']['")
                                                        + "'] AS '"
                                                        + field
                                                        + "'");
                                    } else {
                                        result.add(field);
                                    }
                                }
                                return result;
                            });
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenAnswer(
                            invocation -> {
                                List<String> input = invocation.getArgument(0);
                                return input; // 简单返回原值用于测试
                            });

            long startTime = System.currentTimeMillis();
            List<LogSearchDTO> results = new ArrayList<>();
            for (LogSearchDTO dto : dtoList) {
                results.add(converter.convert(dto));
            }
            long endTime = System.currentTimeMillis();

            assertEquals(1000, results.size(), "批量转换结果数量应正确");
            assertTrue(
                    endTime - startTime < 1000,
                    "1000个DTO转换应在1秒内完成，实际用时：" + (endTime - startTime) + "ms");

            // 验证所有结果都是装饰器
            long decoratorCount =
                    results.stream()
                            .mapToLong(dto -> dto instanceof LogSearchDTODecorator ? 1L : 0L)
                            .sum();
            assertEquals(1000, decoratorCount, "需要转换的DTO都应返回装饰器");
        }

        @Test
        @DisplayName("内存使用优化 - 期望：大量转换不会导致内存泄漏")
        void testMemoryUsageOptimization() {
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // 执行大量转换操作
            for (int i = 0; i < 1000; i++) {
                LogSearchDTO dto = createBasicDTO();
                dto.setFields(Arrays.asList("message.field" + i, "host" + i));
                dto.setWhereSqls(Arrays.asList("message.condition" + i + " = 'value'"));

                // Mock转换器行为
                when(variantFieldConverter.convertSelectFields(any()))
                        .thenReturn(
                                Arrays.asList(
                                        "message['field" + i + "'] AS 'message.field" + i + "'",
                                        "host" + i));
                when(variantFieldConverter.convertWhereClauses(any()))
                        .thenReturn(Arrays.asList("message['condition" + i + "'] = 'value'"));

                LogSearchDTO result = converter.convert(dto);
                assertNotNull(result); // 确保转换成功
            }

            // 强制垃圾回收
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // 内存增长应该在合理范围内（小于20MB）
            assertTrue(
                    memoryIncrease < 20 * 1024 * 1024,
                    "大量转换操作后内存增长应在20MB以内，实际增长：" + (memoryIncrease / 1024 / 1024) + "MB");
        }
    }

    // ==================== 复杂场景测试 ====================

    @Nested
    @DisplayName("复杂业务场景测试")
    class ComplexScenarioTests {

        @Test
        @DisplayName("日志查询典型场景 - 期望：模拟真实的日志查询业务场景")
        void testTypicalLogQueryScenario() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(
                    Arrays.asList(
                            "log_time",
                            "host",
                            "level",
                            "message.service",
                            "message.logId",
                            "message.duration"));
            original.setWhereSqls(
                    Arrays.asList(
                            "log_time >= '2023-06-01 10:00:00'",
                            "message.level = 'ERROR'",
                            "message.service = 'order-service'"));
            original.setModule("order-service");
            original.setPageSize(50);
            original.setOffset(0);

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "log_time",
                                    "host",
                                    "level",
                                    "message['service'] AS 'message.service'",
                                    "message['logId'] AS 'message.logId'",
                                    "message['duration'] AS 'message.duration'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "log_time >= '2023-06-01 10:00:00'",
                                    "message['level'] = 'ERROR'",
                                    "message['service'] = 'order-service'"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);

            // 验证转换后的字段
            List<String> fields = result.getFields();
            assertEquals(6, fields.size());
            assertEquals("log_time", fields.get(0));
            assertEquals("message['service'] AS 'message.service'", fields.get(3));

            // 验证转换后的WHERE条件
            List<String> wheres = result.getWhereSqls();
            assertEquals(3, wheres.size());
            assertEquals("message['level'] = 'ERROR'", wheres.get(1));

            // 验证其他属性保持不变
            assertEquals("order-service", result.getModule());
            assertEquals(50, result.getPageSize());
            assertEquals(0, result.getOffset());
        }

        @Test
        @DisplayName("深层嵌套字段查询场景 - 期望：正确处理复杂的嵌套字段结构")
        void testDeepNestedFieldQueryScenario() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(
                    Arrays.asList(
                            "message.request.headers.authorization",
                            "message.response.body.data.user.id",
                            "message.trace.spans.operations.duration"));
            original.setWhereSqls(
                    Arrays.asList(
                            "message.request.method = 'POST'",
                            "message.response.status >= 400",
                            "message.trace.spans.duration > 1000"));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "message['request']['headers']['authorization'] AS"
                                            + " 'message.request.headers.authorization'",
                                    "message['response']['body']['data']['user']['id'] AS"
                                            + " 'message.response.body.data.user.id'",
                                    "message['trace']['spans']['operations']['duration'] AS"
                                            + " 'message.trace.spans.operations.duration'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "message['request']['method'] = 'POST'",
                                    "message['response']['status'] >= 400",
                                    "message['trace']['spans']['duration'] > 1000"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);

            List<String> fields = result.getFields();
            assertEquals(3, fields.size());
            assertTrue(
                    fields.get(0).contains("['request']['headers']['authorization']"),
                    "深层嵌套字段应正确转换");
            assertTrue(
                    fields.get(1).contains("['response']['body']['data']['user']['id']"),
                    "超深层嵌套字段应正确转换");

            List<String> wheres = result.getWhereSqls();
            assertEquals(3, wheres.size());
            assertTrue(wheres.get(0).contains("['request']['method']"), "深层嵌套WHERE条件应正确转换");
        }

        @Test
        @DisplayName("混合查询场景 - 期望：正确处理包含普通字段和variant字段的混合查询")
        void testMixedQueryScenario() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(
                    Arrays.asList(
                            "id",
                            "log_time",
                            "host",
                            "level", // 普通字段
                            "message",
                            "message.service",
                            "message.logId", // variant字段
                            "path",
                            "method" // 更多普通字段
                            ));
            original.setWhereSqls(
                    Arrays.asList(
                            "level IN ('ERROR', 'WARN')", // 普通条件
                            "message.service = 'user-service'", // variant条件
                            "host LIKE '%prod%'", // 普通条件
                            "message.duration > 1000" // variant条件
                            ));

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "id",
                                    "log_time",
                                    "host",
                                    "level",
                                    "message",
                                    "message['service'] AS 'message.service'",
                                    "message['logId'] AS 'message.logId'",
                                    "path",
                                    "method"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(
                            Arrays.asList(
                                    "level IN ('ERROR', 'WARN')",
                                    "message['service'] = 'user-service'",
                                    "host LIKE '%prod%'",
                                    "message['duration'] > 1000"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);

            List<String> fields = result.getFields();
            assertEquals(9, fields.size());
            assertEquals("id", fields.get(0), "普通字段应保持不变");
            assertEquals("message", fields.get(4), "variant根字段应保持不变");
            assertEquals(
                    "message['service'] AS 'message.service'", fields.get(5), "variant子字段应正确转换");

            List<String> wheres = result.getWhereSqls();
            assertEquals(4, wheres.size());
            assertEquals("level IN ('ERROR', 'WARN')", wheres.get(0), "普通WHERE条件应保持不变");
            assertEquals(
                    "message['service'] = 'user-service'", wheres.get(1), "variant WHERE条件应正确转换");
        }
    }

    // ==================== 参数化测试 ====================

    @Nested
    @DisplayName("参数化测试")
    class ParameterizedTests {

        @ParameterizedTest
        @DisplayName("不同variant字段模式测试 - 期望：正确处理各种variant字段命名模式")
        @ValueSource(
                strings = {
                    "message.level",
                    "data.user.name",
                    "event.trace.span",
                    "log.metadata.tags",
                    "request.headers.auth"
                })
        void testVariousVariantFieldPatterns(String variantField) {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList(variantField, "host"));
            original.setWhereSqls(Arrays.asList("level = 'INFO'"));

            // 正确构造期望的转换格式：message.level -> message['level'] AS 'message.level'
            String[] parts = variantField.split("\\.");
            StringBuilder convertedField = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                convertedField.append("['").append(parts[i]).append("']");
            }
            String expectedConvertedField =
                    convertedField.toString() + " AS '" + variantField + "'";

            // Mock转换器行为
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(Arrays.asList(expectedConvertedField, "host"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenReturn(Arrays.asList("level = 'INFO'"));

            LogSearchDTO result = converter.convert(original);

            assertInstanceOf(LogSearchDTODecorator.class, result);
            List<String> fields = result.getFields();
            assertTrue(
                    fields.get(0).startsWith(variantField.split("\\.")[0] + "['"),
                    "variant字段 " + variantField + " 应正确转换");
            assertTrue(
                    fields.get(0).endsWith("'] AS '" + variantField + "'"),
                    "variant字段 " + variantField + " 应有正确的别名");
        }
    }

    // ==================== needsVariantConversion 方法单元测试 ====================

    @Nested
    @DisplayName("needsVariantConversion方法边界测试")
    class NeedsVariantConversionTests {

        // 使用反射访问私有方法
        private boolean testNeedsVariantConversion(String text) throws Exception {
            var method =
                    LogSearchDTOConverter.class.getDeclaredMethod(
                            "needsVariantConversion", String.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(converter, text);
        }

        @Test
        @DisplayName("基本转换需求测试 - 期望：简单点语法字段应该需要转换")
        void testBasicConversionNeeds() throws Exception {
            assertTrue(testNeedsVariantConversion("message.level = 'ERROR'"), "简单点语法字段应该需要转换");
            assertTrue(testNeedsVariantConversion("data.user.name = 'John'"), "嵌套点语法字段应该需要转换");
            assertTrue(testNeedsVariantConversion("event.trace.span = '123'"), "多级嵌套字段应该需要转换");
        }

        @Test
        @DisplayName("值中包含点的情况 - 期望：值中的点不应触发转换")
        void testDotsInValues() throws Exception {
            assertFalse(
                    testNeedsVariantConversion("level = 'com.example.service.UserService'"),
                    "纯值中的点不应触发转换");
            assertTrue(
                    testNeedsVariantConversion(
                            "message.service = 'com.example.service.UserService'"),
                    "字段名中的点应该转换，值中的点应忽略");
            assertTrue(
                    testNeedsVariantConversion("data.config = 'server.port=8080'"),
                    "字段名有点时应转换，不管值的内容");
            assertFalse(testNeedsVariantConversion("host = '192.168.1.100'"), "IP地址等包含点的值不应触发转换");
        }

        @Test
        @DisplayName("值中包含方括号的情况 - 期望：值中的方括号不应影响判断")
        void testBracketsInValues() throws Exception {
            assertTrue(
                    testNeedsVariantConversion("message.data = 'array[0]'"),
                    "字段名有点时应转换，不管值中是否有方括号");
            assertTrue(
                    testNeedsVariantConversion("log.info = 'data[\"key\"]'"), "值中的复杂方括号结构不应影响转换判断");
            assertFalse(testNeedsVariantConversion("count = 'items[5]'"), "无点字段名且值中有方括号不应转换");
        }

        @Test
        @DisplayName("值中包含括号语法的情况 - 期望：值中的bracket语法不应影响判断")
        void testBracketSyntaxInValues() throws Exception {
            assertTrue(
                    testNeedsVariantConversion("message.field = \"path']['to']['file\""),
                    "字段名有点时应转换，不管值中包含什么");
            assertTrue(
                    testNeedsVariantConversion("data.config = 'message[\"field\"]'"),
                    "值中包含类似已转换语法的字符串不应影响判断");
            assertFalse(
                    testNeedsVariantConversion("query = 'SELECT * FROM table WHERE col=\"]\"'"),
                    "复杂SQL值不应触发转换");
        }

        @Test
        @DisplayName("已转换字段的识别 - 期望：已转换的字段不应再次转换")
        void testAlreadyConvertedFields() throws Exception {
            assertFalse(testNeedsVariantConversion("message['level'] = 'ERROR'"), "已转换的简单字段不应再次转换");
            assertFalse(
                    testNeedsVariantConversion("data['user']['name'] = 'John'"), "已转换的嵌套字段不应再次转换");
            assertFalse(
                    testNeedsVariantConversion("message['field'] = 'value.with.dots'"),
                    "已转换字段的值中有点也不应转换");
        }

        @Test
        @DisplayName("混合情况测试 - 期望：复杂SQL语句的正确判断")
        void testMixedScenarios() throws Exception {
            assertTrue(
                    testNeedsVariantConversion("message.level = 'ERROR' AND host = 'server1'"),
                    "包含点语法字段的复杂条件应需要转换");
            assertFalse(
                    testNeedsVariantConversion("host = 'server1' AND port = 8080"),
                    "不包含点语法的复杂条件不应转换");
            assertTrue(
                    testNeedsVariantConversion(
                            "(message.level = 'ERROR' OR message.level = 'WARN')"),
                    "括号内的点语法字段应需要转换");
        }

        @Test
        @DisplayName("特殊字符和转义字符 - 期望：正确处理各种引号和转义")
        void testSpecialCharacters() throws Exception {
            assertTrue(
                    testNeedsVariantConversion("message.data = 'it\\'s a test'"), "转义单引号不应影响字段名识别");
            assertTrue(
                    testNeedsVariantConversion("data.info = \"quote: 'hello'\""), "双引号内的单引号不应影响判断");
            assertFalse(
                    testNeedsVariantConversion("text = 'message.field looks like dot syntax'"),
                    "引号内的伪点语法不应触发转换");
        }

        @Test
        @DisplayName("边界条件测试 - 期望：正确处理空值、空白等边界情况")
        void testBoundaryConditions() throws Exception {
            assertFalse(testNeedsVariantConversion(null), "null输入不应需要转换");
            assertFalse(testNeedsVariantConversion(""), "空字符串不应需要转换");
            assertFalse(testNeedsVariantConversion("   "), "空白字符串不应需要转换");
            assertFalse(testNeedsVariantConversion("."), "单独的点不应触发转换");
            assertFalse(testNeedsVariantConversion(".."), "多个点不应触发转换");
        }

        @Test
        @DisplayName("无效字段名测试 - 期望：不符合标识符规则的不应转换")
        void testInvalidFieldNames() throws Exception {
            assertFalse(testNeedsVariantConversion("123.field = 'value'"), "数字开头的字段名不应转换");
            assertFalse(testNeedsVariantConversion(".field = 'value'"), "点开头的无效语法不应转换");
            assertFalse(testNeedsVariantConversion("field. = 'value'"), "点结尾的无效语法不应转换");
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("转换器抛出异常处理 - 期望：当VariantFieldConverter抛出异常时能正确处理")
        void testConverterException() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId"));

            // Mock转换器抛出异常
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenThrow(new RuntimeException("转换错误"));

            // 应该抛出异常（不吞掉异常）
            assertThrows(
                    RuntimeException.class,
                    () -> {
                        converter.convert(original);
                    },
                    "转换器异常应该被传播");
        }

        @Test
        @DisplayName("部分转换失败处理 - 期望：当部分转换失败时能正确处理")
        void testPartialConversionFailure() {
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId"));
            original.setWhereSqls(Arrays.asList("message.level = 'ERROR'"));

            // Mock字段转换成功，WHERE转换失败
            when(variantFieldConverter.convertSelectFields(any()))
                    .thenReturn(Arrays.asList("message['logId'] AS 'message.logId'"));
            when(variantFieldConverter.convertWhereClauses(any()))
                    .thenThrow(new RuntimeException("WHERE转换错误"));

            assertThrows(
                    RuntimeException.class,
                    () -> {
                        converter.convert(original);
                    },
                    "部分转换失败时应抛出异常");
        }
    }

    // ==================== 辅助方法 ====================

    /** 创建基础的LogSearchDTO对象用于测试 设置常用的默认值以便测试使用 */
    private LogSearchDTO createBasicDTO() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setDatasourceId(1L);
        dto.setModule("test-module");
        dto.setKeywords(Arrays.asList("error", "timeout"));
        dto.setStartTime("2023-06-01 10:00:00");
        dto.setEndTime("2023-06-01 11:00:00");
        dto.setPageSize(50);
        dto.setOffset(0);
        dto.setTimeRange("last_1h");
        return dto;
    }
}
