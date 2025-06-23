package com.hinadt.miaocha.mock.service.sql.converter;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * VariantFieldConverter单元测试
 *
 * <p>测试秒查系统中variant字段的点语法到括号语法的转换功能 这是秒查系统处理动态JSON字段查询的核心组件
 *
 * <p>测试覆盖范围： 1. WHERE条件转换测试 - 各种复杂度的WHERE条件转换 2. SELECT字段转换测试 - 字段列表转换和别名生成 3. TOPN字段转换测试 - 单字段转换
 * 4. 边界条件测试 - 空值、null值、异常输入处理 5. 性能测试 - 大量数据处理性能验证 6. 复杂场景测试 - 真实业务场景模拟 7. 安全性测试 - 恶意输入和SQL注入防护
 */
@Epic("秒查日志管理系统")
@Feature("SQL查询引擎")
@Story("动态字段转换")
@Owner("开发团队")
class VariantFieldConverterTest {

    private VariantFieldConverter converter;

    @BeforeEach
    void setUp() {
        converter = new VariantFieldConverter();
    }

    // ==================== WHERE条件转换测试 ====================

    @Nested
    @DisplayName("WHERE条件转换测试")
    class WhereClauseConversionTests {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("简单点语法WHERE条件转换 - 期望：message.logId转换为message['logId']")
        @Description("验证秒查系统能够正确转换简单的JSON字段点语法查询条件")
        @Issue("MIAOCHA-301")
        void testSimpleDotSyntaxWhereConversion() {
            Allure.step(
                    "准备转换测试数据",
                    () -> {
                        String input = "message.logId = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d'";
                        Allure.parameter("输入SQL条件", input);
                        Allure.parameter(
                                "预期输出",
                                "message['logId'] = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d'");
                    });

            String input = "message.logId = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d'";
            String expected = "message['logId'] = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d'";

            String result =
                    Allure.step(
                            "执行WHERE条件转换",
                            () -> {
                                return converter.convertWhereClause(input);
                            });

            Allure.step(
                    "验证转换结果",
                    () -> {
                        assertEquals(expected, result, "简单点语法应转换为括号语法，值部分保持不变");
                        Allure.parameter("实际输出", result);

                        // 附加转换结果
                        Allure.attachment(
                                "转换结果对比",
                                String.format("输入: %s\n输出: %s\n预期: %s", input, result, expected));
                    });
        }

        @Test
        @DisplayName("多层嵌套点语法WHERE条件转换 - 期望：message.marker.data转换为message['marker']['data']")
        @Severity(SeverityLevel.NORMAL)
        @Description("验证秒查系统能够正确处理多层嵌套的JSON字段点语法转换")
        void testNestedDotSyntaxWhereConversion() {
            Allure.step(
                    "准备多层嵌套测试数据",
                    () -> {
                        Allure.parameter("输入条件", "message.marker.data = 'test'");
                        Allure.parameter("预期输出", "message['marker']['data'] = 'test'");
                        Allure.parameter("嵌套层级", "3层");
                    });

            String input = "message.marker.data = 'test'";
            String expected = "message['marker']['data'] = 'test'";

            String result =
                    Allure.step(
                            "执行多层嵌套转换",
                            () -> {
                                return converter.convertWhereClause(input);
                            });

            Allure.step(
                    "验证多层嵌套转换结果",
                    () -> {
                        assertEquals(expected, result, "多层嵌套点语法应逐级转换为括号语法");
                        Allure.parameter("实际输出", result);
                        Allure.attachment("转换详情", String.format("原始: %s → 转换: %s", input, result));
                    });
        }

        @Test
        @DisplayName("复杂嵌套点语法WHERE条件转换 - 期望：多个字段同时转换，逻辑运算符保持不变")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证包含多个variant字段和逻辑运算符的复杂WHERE条件能够正确转换")
        void testComplexNestedWhereConversion() {
            Allure.step(
                    "准备复杂WHERE条件测试数据",
                    () -> {
                        Allure.parameter(
                                "输入条件",
                                "message.marker.duration > 100 AND message.service ="
                                        + " 'user-service'");
                        Allure.parameter(
                                "包含字段",
                                Arrays.asList("message.marker.duration", "message.service"));
                        Allure.parameter("逻辑运算符", "AND");
                        Allure.parameter("比较运算符", Arrays.asList(">", "="));
                    });

            String input = "message.marker.duration > 100 AND message.service = 'user-service'";
            String expected =
                    "message['marker']['duration'] > 100 AND message['service'] = 'user-service'";

            String result =
                    Allure.step(
                            "执行复杂条件转换",
                            () -> {
                                return converter.convertWhereClause(input);
                            });

            Allure.step(
                    "验证复杂条件转换结果",
                    () -> {
                        assertEquals(expected, result, "复杂条件中的多个点语法字段应同时转换，逻辑运算符和比较运算符保持不变");

                        Allure.parameter("实际输出", result);
                        Allure.parameter("字段转换数量", "2个");
                        Allure.attachment(
                                "复杂转换详情",
                                String.format(
                                        "原始: %s\n转换: %s\n验证: 多字段同时转换，运算符保持不变", input, result));
                    });
        }

        @Test
        @DisplayName("值中包含点的WHERE条件 - 期望：只转换字段名，值中的点保持不变")
        void testWhereClauseWithDotsInValues() {
            String input = "message.logId = 'com.example.service.UserService'";
            String expected = "message['logId'] = 'com.example.service.UserService'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "字段名中的点应转换，但值中的点应保持不变");
        }

        @Test
        @DisplayName("已经是括号语法的WHERE条件 - 期望：不做任何转换")
        void testAlreadyBracketSyntaxWhere() {
            String input = "message['logId'] = 'test'";
            String expected = "message['logId'] = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "已经是括号语法的字段不应再次转换");
        }

        @Test
        @DisplayName("普通字段不应被转换 - 期望：不包含点的字段名保持不变")
        void testRegularFieldsNotConverted() {
            String input = "level = 'ERROR' AND host = 'server1'";
            String expected = "level = 'ERROR' AND host = 'server1'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "普通字段名（不包含点）不应被转换");
        }

        @Test
        @DisplayName("混合括号和点语法 - 期望：只转换点语法部分")
        void testMixedBracketAndDotSyntax() {
            String input = "message['level'] = 'ERROR' AND message.service = 'api'";
            String expected = "message['level'] = 'ERROR' AND message['service'] = 'api'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "混合语法中只有点语法部分应被转换");
        }

        @Test
        @DisplayName("复杂逻辑运算符组合 - 期望：支持OR、AND、NOT等复杂逻辑")
        void testComplexLogicalOperators() {
            String input =
                    "(message.level = 'ERROR' OR message.level = 'WARN') AND NOT message.service ="
                            + " 'test'";
            String expected =
                    "(message['level'] = 'ERROR' OR message['level'] = 'WARN') AND NOT"
                            + " message['service'] = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "复杂逻辑运算符组合中的点语法字段应正确转换");
        }

        @Test
        @DisplayName("数值比较操作 - 期望：支持各种比较运算符")
        void testNumericComparisons() {
            String input =
                    "message.marker.duration >= 100 AND message.marker.count <= 50 AND"
                            + " message.marker.rate != 0";
            String expected =
                    "message['marker']['duration'] >= 100 AND message['marker']['count'] <= 50 AND"
                            + " message['marker']['rate'] != 0";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "数值比较操作中的点语法字段应正确转换");
        }

        @Test
        @DisplayName("LIKE模糊匹配 - 期望：支持LIKE操作符")
        void testLikeOperator() {
            String input = "message.service LIKE '%user%' AND message.path NOT LIKE '/health%'";
            String expected =
                    "message['service'] LIKE '%user%' AND message['path'] NOT LIKE '/health%'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "LIKE操作符中的点语法字段应正确转换");
        }

        @Test
        @DisplayName("IN操作符 - 期望：支持IN和NOT IN操作")
        void testInOperator() {
            String input =
                    "message.level IN ('ERROR', 'WARN') AND message.service NOT IN ('test',"
                            + " 'mock')";
            String expected =
                    "message['level'] IN ('ERROR', 'WARN') AND message['service'] NOT IN ('test',"
                            + " 'mock')";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "IN操作符中的点语法字段应正确转换");
        }

        @Test
        @DisplayName("NULL值判断 - 期望：支持IS NULL和IS NOT NULL")
        void testNullChecks() {
            String input = "message.error IS NOT NULL AND message.stack IS NULL";
            String expected = "message['error'] IS NOT NULL AND message['stack'] IS NULL";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "NULL值判断中的点语法字段应正确转换");
        }

        @Test
        @DisplayName("函数调用 - 期望：支持字段作为函数参数")
        void testFunctionCalls() {
            String input = "LENGTH(message.content) > 100 AND UPPER(message.level) = 'ERROR'";
            String expected =
                    "LENGTH(message['content']) > 100 AND UPPER(message['level']) = 'ERROR'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "函数调用中的点语法字段参数应正确转换");
        }
    }

    // ==================== SELECT字段转换测试 ====================

    @Nested
    @DisplayName("SELECT字段转换测试")
    class SelectFieldConversionTests {

        @Test
        @DisplayName("简单点语法SELECT字段转换 - 期望：生成带AS别名的括号语法")
        void testSimpleDotSyntaxSelectConversion() {
            List<String> input = Arrays.asList("message.logId", "host", "log_time");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(3, result.size(), "结果列表大小应与输入一致");
            assertEquals(
                    "message['logId'] AS 'message.logId'", result.get(0), "点语法字段应转换为括号语法并添加AS别名");
            assertEquals("host", result.get(1), "普通字段应保持不变");
            assertEquals("log_time", result.get(2), "普通字段应保持不变");
        }

        @Test
        @DisplayName("多层嵌套SELECT字段转换 - 期望：多级嵌套正确转换")
        void testNestedSelectFieldConversion() {
            List<String> input =
                    Arrays.asList("message.marker.data", "message.marker.duration", "level");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(3, result.size());
            assertEquals(
                    "message['marker']['data'] AS 'message.marker.data'",
                    result.get(0),
                    "三级嵌套字段应正确转换并保持原始路径作为别名");
            assertEquals(
                    "message['marker']['duration'] AS 'message.marker.duration'",
                    result.get(1),
                    "三级嵌套字段应正确转换并保持原始路径作为别名");
            assertEquals("level", result.get(2), "普通字段应保持不变");
        }

        @Test
        @DisplayName("复杂嵌套SELECT字段转换 - 期望：处理大量混合字段")
        void testComplexNestedSelectConversion() {
            List<String> input =
                    Arrays.asList(
                            "message.level",
                            "message.line",
                            "message.marker.reqType",
                            "message.marker.duration",
                            "host",
                            "log_time");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(6, result.size());
            assertEquals("message['level'] AS 'message.level'", result.get(0));
            assertEquals("message['line'] AS 'message.line'", result.get(1));
            assertEquals("message['marker']['reqType'] AS 'message.marker.reqType'", result.get(2));
            assertEquals(
                    "message['marker']['duration'] AS 'message.marker.duration'", result.get(3));
            assertEquals("host", result.get(4));
            assertEquals("log_time", result.get(5));
        }

        @Test
        @DisplayName("深层嵌套字段 - 期望：支持四层以上嵌套")
        void testDeepNestedFields() {
            List<String> input =
                    Arrays.asList(
                            "message.request.headers.auth.token",
                            "message.response.body.data.user.id");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(2, result.size());
            assertEquals(
                    "message['request']['headers']['auth']['token'] AS"
                            + " 'message.request.headers.auth.token'",
                    result.get(0),
                    "五层嵌套字段应正确转换");
            assertEquals(
                    "message['response']['body']['data']['user']['id'] AS"
                            + " 'message.response.body.data.user.id'",
                    result.get(1),
                    "六层嵌套字段应正确转换");
        }

        @Test
        @DisplayName("单个字段列表 - 期望：正确处理只有一个字段的情况")
        void testSingleFieldList() {
            List<String> input = Arrays.asList("message.logId");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(1, result.size());
            assertEquals("message['logId'] AS 'message.logId'", result.get(0), "单个点语法字段应正确转换");
        }

        @Test
        @DisplayName("大量字段处理 - 期望：高效处理大量字段")
        void testLargeFieldList() {
            List<String> input = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                input.add("message.field" + i);
                input.add("regular_field" + i);
            }

            List<String> result = converter.convertSelectFields(input);

            assertEquals(200, result.size(), "结果数量应与输入一致");
            assertEquals("message['field0'] AS 'message.field0'", result.get(0));
            assertEquals("regular_field0", result.get(1));
            assertEquals("message['field99'] AS 'message.field99'", result.get(198));
            assertEquals("regular_field99", result.get(199));
        }
    }

    // ==================== TOPN字段转换测试 ====================

    @Nested
    @DisplayName("TOPN字段转换测试")
    class TopnFieldConversionTests {

        @Test
        @DisplayName("TOPN字段转换 - 期望：转换为括号语法但不添加别名")
        void testTopnFieldConversion() {
            String input = "message.level";
            String expected = "message['level']";
            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "TOPN字段应转换为括号语法但不添加AS别名");
        }

        @Test
        @DisplayName("嵌套TOPN字段转换 - 期望：多层嵌套正确转换")
        void testNestedTopnFieldConversion() {
            String input = "message.marker.duration";
            String expected = "message['marker']['duration']";
            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "嵌套TOPN字段应正确转换为多层括号语法");
        }

        @Test
        @DisplayName("普通TOPN字段不转换 - 期望：不包含点的字段保持不变")
        void testRegularTopnFieldNotConverted() {
            String input = "level";
            String expected = "level";
            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "普通字段不应被转换");
        }

        @Test
        @DisplayName("深层嵌套TOPN字段 - 期望：支持深层嵌套")
        void testDeepNestedTopnField() {
            String input = "message.request.body.data.user";
            String expected = "message['request']['body']['data']['user']";
            String result = converter.convertTopnField(input);

            assertEquals(expected, result, "深层嵌套TOPN字段应正确转换");
        }
    }

    // ==================== 批量转换测试 ====================

    @Nested
    @DisplayName("批量转换测试")
    class BatchConversionTests {

        @Test
        @DisplayName("批量WHERE条件转换 - 期望：列表中每个条件都正确转换")
        void testBatchWhereClausesConversion() {
            List<String> input =
                    Arrays.asList(
                            "message.logId = 'test'",
                            "message.marker.data = 'value'",
                            "level = 'ERROR'");
            List<String> result = converter.convertWhereClauses(input);

            assertEquals(3, result.size());
            assertEquals("message['logId'] = 'test'", result.get(0));
            assertEquals("message['marker']['data'] = 'value'", result.get(1));
            assertEquals("level = 'ERROR'", result.get(2), "普通字段应保持不变");
        }

        @Test
        @DisplayName("空批量处理 - 期望：正确处理空列表")
        void testEmptyBatchConversion() {
            List<String> emptyList = Collections.emptyList();
            List<String> result = converter.convertWhereClauses(emptyList);

            assertTrue(result.isEmpty(), "空列表应返回空列表");
        }

        @Test
        @DisplayName("大批量转换性能 - 期望：高效处理大量条件")
        void testLargeBatchPerformance() {
            List<String> input = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                input.add("message.field" + i + " = 'value" + i + "'");
            }

            long startTime = System.currentTimeMillis();
            List<String> result = converter.convertWhereClauses(input);
            long endTime = System.currentTimeMillis();

            assertEquals(1000, result.size());
            assertTrue(endTime - startTime < 1000, "1000个条件转换应在1秒内完成");
            assertEquals("message['field0'] = 'value0'", result.get(0));
            assertEquals("message['field999'] = 'value999'", result.get(999));
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("空字符串处理 - 期望：空字符串和空白字符串保持不变")
        void testEmptyStringHandling() {
            assertEquals("", converter.convertWhereClause(""), "空字符串应保持不变");
            assertEquals("   ", converter.convertWhereClause("   "), "空白字符串应保持不变");
            assertEquals("\t\n", converter.convertWhereClause("\t\n"), "制表符和换行符应保持不变");
        }

        @Test
        @DisplayName("null值处理 - 期望：null输入返回null")
        void testNullHandling() {
            assertNull(converter.convertWhereClause(null), "null WHERE条件应返回null");
            assertNull(converter.convertTopnField(null), "null TOPN字段应返回null");
            assertNull(converter.convertSelectFields(null), "null字段列表应返回null");
            assertNull(converter.convertWhereClauses(null), "null WHERE条件列表应返回null");
        }

        @Test
        @DisplayName("空字段列表处理 - 期望：空列表返回空列表")
        void testEmptyFieldsList() {
            List<String> input = Collections.emptyList();
            List<String> result = converter.convertSelectFields(input);

            assertTrue(result.isEmpty(), "空字段列表应返回空列表");
            assertNotSame(input, result, "应返回新的空列表对象");
        }

        @Test
        @DisplayName("包含null元素的列表 - 期望：跳过null元素")
        void testListWithNullElements() {
            List<String> input = Arrays.asList("message.level", null, "host", null);
            List<String> result = converter.convertSelectFields(input);

            assertEquals(4, result.size(), "结果大小应与输入一致");
            assertEquals("message['level'] AS 'message.level'", result.get(0));
            assertNull(result.get(1), "null元素应保持为null");
            assertEquals("host", result.get(2));
            assertNull(result.get(3), "null元素应保持为null");
        }

        @Test
        @DisplayName("极长字段名处理 - 期望：正确处理超长字段名")
        void testVeryLongFieldNames() {
            StringBuilder longField = new StringBuilder("message");
            for (int i = 0; i < 50; i++) {
                longField.append(".verylongfieldname").append(i);
            }
            String input = longField.toString();

            String result = converter.convertTopnField(input);

            assertTrue(result.startsWith("message['verylongfieldname0']"));
            assertTrue(result.endsWith("['verylongfieldname49']"));
            assertFalse(result.contains("."), "结果不应包含点");
        }
    }

    // ==================== 特殊字符和格式测试 ====================

    @Nested
    @DisplayName("特殊字符和格式测试")
    class SpecialCharacterTests {

        @Test
        @DisplayName("不规范字段名处理 - 期望：以数字开头的字段名不转换")
        void testInvalidFieldNames() {
            String input = "123field.subfield = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(input, result, "以数字开头的字段名不符合标识符规范，不应转换");
        }

        @Test
        @DisplayName("字段名包含下划线 - 期望：下划线字段名正常转换")
        void testUnderscoreInFieldNames() {
            String input = "message_field.sub_field = 'test'";
            String expected = "message_field['sub_field'] = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "包含下划线的字段名应正常转换");
        }

        @Test
        @DisplayName("字段名包含数字 - 期望：字段名中间的数字正常转换")
        void testNumbersInFieldNames() {
            String input = "message1.field2.value3 = 'test'";
            String expected = "message1['field2']['value3'] = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "字段名中间包含数字应正常转换");
        }

        @Test
        @DisplayName("多个点语法字段在同一条件中 - 期望：所有点语法字段都被转换")
        void testMultipleDotSyntaxInSameClause() {
            String input =
                    "message.level = 'ERROR' OR message.service = 'api' AND data.status = 'failed'";
            String expected =
                    "message['level'] = 'ERROR' OR message['service'] = 'api' AND data['status'] ="
                            + " 'failed'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "同一条件中的多个点语法字段应都被转换");
        }

        @Test
        @DisplayName("引号内的点语法 - 期望：引号内的内容不被转换")
        void testQuotedContent() {
            String input =
                    "message.content = 'This is message.level content' AND message.level = 'ERROR'";
            String expected =
                    "message['content'] = 'This is message.level content' AND message['level'] ="
                            + " 'ERROR'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "引号内的点语法不应被转换，只转换字段名");
        }

        @Test
        @DisplayName("转义字符处理 - 期望：正确处理转义字符")
        void testEscapeCharacters() {
            String input = "message.path = '/api\\\\test' AND message.query = 'key\\=value'";
            String expected =
                    "message['path'] = '/api\\\\test' AND message['query'] = 'key\\=value'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "转义字符应保持不变");
        }
    }

    // ==================== 参数化测试 ====================

    @Nested
    @DisplayName("参数化测试")
    class ParameterizedTests {

        @ParameterizedTest
        @DisplayName("各种比较运算符测试 - 期望：支持所有SQL比较运算符")
        @ValueSource(strings = {"=", "!=", "<>", "<", ">", "<=", ">="})
        void testVariousComparisonOperators(String operator) {
            String input = "message.value " + operator + " 100";
            String expected = "message['value'] " + operator + " 100";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "运算符 " + operator + " 应被支持");
        }

        @ParameterizedTest
        @DisplayName("各种逻辑运算符测试 - 期望：支持所有SQL逻辑运算符")
        @ValueSource(strings = {"AND", "OR", "and", "or", "And", "Or"})
        void testVariousLogicalOperators(String operator) {
            String input = "message.field1 = 'a' " + operator + " message.field2 = 'b'";
            String expected = "message['field1'] = 'a' " + operator + " message['field2'] = 'b'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "逻辑运算符 " + operator + " 应被支持");
        }

        @ParameterizedTest
        @DisplayName("不同嵌套层级测试 - 期望：支持1-5层嵌套")
        @CsvSource({
            "message.level, message['level']",
            "message.marker.data, message['marker']['data']",
            "message.request.body.data, message['request']['body']['data']",
            "message.trace.span.tag.key, message['trace']['span']['tag']['key']",
            "message.deep.very.deep.nest.field, message['deep']['very']['deep']['nest']['field']"
        })
        void testVariousNestingLevels(String input, String expected) {
            String result = converter.convertTopnField(input);
            assertEquals(expected, result, input + " 应正确转换为 " + expected);
        }
    }

    // ==================== 真实场景测试 ====================

    @Nested
    @DisplayName("真实场景测试")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("真实variant展开字段测试 - 期望：模拟Doris实际返回的字段列表")
        void testRealVariantExpandedFields() {
            // 模拟真实的Doris variant展开字段
            List<String> input =
                    Arrays.asList(
                            "log_time",
                            "host",
                            "path",
                            "message", // variant原始字段
                            "message.level", // variant展开字段
                            "message.line",
                            "message.logId",
                            "message.logger",
                            "message.marker.data",
                            "message.marker.duration",
                            "message.marker.reqType",
                            "message.method",
                            "message.msg",
                            "message.service",
                            "message.stacktrace",
                            "message.thread",
                            "message.time");

            List<String> result = converter.convertSelectFields(input);

            // 验证普通字段不变
            assertEquals("log_time", result.get(0), "普通时间字段应保持不变");
            assertEquals("host", result.get(1), "普通主机字段应保持不变");
            assertEquals("path", result.get(2), "普通路径字段应保持不变");
            assertEquals("message", result.get(3), "variant根字段应保持不变");

            // 验证variant展开字段被正确转换
            assertEquals(
                    "message['level'] AS 'message.level'",
                    result.get(4),
                    "variant子字段应转换为括号语法并添加别名");
            assertEquals("message['line'] AS 'message.line'", result.get(5));
            assertEquals("message['logId'] AS 'message.logId'", result.get(6));
            assertEquals("message['logger'] AS 'message.logger'", result.get(7));
            assertEquals(
                    "message['marker']['data'] AS 'message.marker.data'",
                    result.get(8),
                    "二级嵌套字段应正确转换");
            assertEquals(
                    "message['marker']['duration'] AS 'message.marker.duration'", result.get(9));
            assertEquals(
                    "message['marker']['reqType'] AS 'message.marker.reqType'", result.get(10));
        }

        @Test
        @DisplayName("真实WHERE条件测试 - 期望：模拟实际业务查询条件")
        void testRealWhereConditions() {
            String input =
                    "message.logId = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d' AND"
                            + " message.marker.duration > 100";
            String expected =
                    "message['logId'] = 'ae5a8205-4b5f-4ffe-bccd-d8ce49aaab9d' AND"
                            + " message['marker']['duration'] > 100";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "真实UUID查询和性能阈值条件应正确转换");
        }

        @Test
        @DisplayName("复杂业务查询场景 - 期望：处理包含多种条件的复杂查询")
        void testComplexBusinessQuery() {
            String input =
                    "(message.level IN ('ERROR', 'WARN') OR message.marker.duration > 1000) "
                            + "AND message.service LIKE '%user%' "
                            + "AND message.trace.spanId IS NOT NULL "
                            + "AND LENGTH(message.stacktrace) > 0";
            String expected =
                    "(message['level'] IN ('ERROR', 'WARN') OR message['marker']['duration'] >"
                            + " 1000) AND message['service'] LIKE '%user%' AND"
                            + " message['trace']['spanId'] IS NOT NULL AND"
                            + " LENGTH(message['stacktrace']) > 0";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "复杂业务查询中的所有点语法字段应正确转换");
        }

        @Test
        @DisplayName("日志搜索典型场景 - 期望：处理典型的日志搜索需求")
        void testTypicalLogSearchScenario() {
            List<String> selectFields =
                    Arrays.asList(
                            "log_time",
                            "host",
                            "message.level",
                            "message.service",
                            "message.logId",
                            "message.marker.duration");
            String whereClause =
                    "message.level = 'ERROR' AND message.service = 'order-service' AND log_time >="
                            + " '2023-06-01'";
            String topnField = "message.service";

            List<String> convertedSelect = converter.convertSelectFields(selectFields);
            String convertedWhere = converter.convertWhereClause(whereClause);
            String convertedTopn = converter.convertTopnField(topnField);

            // 验证SELECT字段转换
            assertEquals("log_time", convertedSelect.get(0));
            assertEquals("host", convertedSelect.get(1));
            assertEquals("message['level'] AS 'message.level'", convertedSelect.get(2));
            assertEquals("message['service'] AS 'message.service'", convertedSelect.get(3));
            assertEquals("message['logId'] AS 'message.logId'", convertedSelect.get(4));
            assertEquals(
                    "message['marker']['duration'] AS 'message.marker.duration'",
                    convertedSelect.get(5));

            // 验证WHERE条件转换
            assertEquals(
                    "message['level'] = 'ERROR' AND message['service'] = 'order-service' AND"
                            + " log_time >= '2023-06-01'",
                    convertedWhere);

            // 验证TOPN字段转换
            assertEquals("message['service']", convertedTopn);
        }
    }

    // ==================== 性能压力测试 ====================

    @Nested
    @DisplayName("性能压力测试")
    class PerformanceStressTests {

        @Test
        @DisplayName("大量字段转换性能 - 期望：处理10000个字段在合理时间内完成")
        void testLargeFieldListPerformance() {
            List<String> input = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                input.add("message.field" + i + ".subfield" + i);
            }

            long startTime = System.currentTimeMillis();
            List<String> result = converter.convertSelectFields(input);
            long endTime = System.currentTimeMillis();

            assertEquals(10000, result.size());
            assertTrue(
                    endTime - startTime < 5000,
                    "10000个字段转换应在5秒内完成，实际用时：" + (endTime - startTime) + "ms");
            assertEquals(
                    "message['field0']['subfield0'] AS 'message.field0.subfield0'", result.get(0));
        }

        @Test
        @DisplayName("复杂WHERE条件性能 - 期望：处理超长复杂条件在合理时间内完成")
        void testComplexWhereClausePerformance() {
            StringBuilder complexWhere = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                if (i > 0) {
                    complexWhere.append(" AND ");
                }
                complexWhere
                        .append("message.field")
                        .append(i)
                        .append(" = 'value")
                        .append(i)
                        .append("'");
            }

            long startTime = System.currentTimeMillis();
            String result = converter.convertWhereClause(complexWhere.toString());
            long endTime = System.currentTimeMillis();

            assertTrue(result.contains("message['field0'] = 'value0'"));
            assertTrue(result.contains("message['field999'] = 'value999'"));
            assertTrue(
                    endTime - startTime < 2000,
                    "复杂WHERE条件转换应在2秒内完成，实际用时：" + (endTime - startTime) + "ms");
        }

        @Test
        @DisplayName("内存使用优化 - 期望：大量转换不会导致内存泄漏")
        void testMemoryUsageOptimization() {
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // 执行大量转换操作
            for (int i = 0; i < 1000; i++) {
                List<String> fields =
                        Arrays.asList("message.field" + i, "message.data" + i + ".value");
                converter.convertSelectFields(fields);

                String where =
                        "message.level" + i + " = 'test' AND message.service" + i + " = 'api'";
                converter.convertWhereClause(where);
            }

            // 强制垃圾回收
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // 内存增长应该在合理范围内（小于50MB）
            assertTrue(
                    memoryIncrease < 50 * 1024 * 1024,
                    "大量转换操作后内存增长应在50MB以内，实际增长：" + (memoryIncrease / 1024 / 1024) + "MB");
        }
    }

    // ==================== 安全性测试 ====================

    @Nested
    @DisplayName("安全性测试")
    class SecurityTests {

        @Test
        @DisplayName("SQL注入防护 - 期望：不处理潜在的SQL注入内容")
        void testSqlInjectionProtection() {
            String maliciousInput = "message.field = 'value'; DROP TABLE logs; --";
            String result = converter.convertWhereClause(maliciousInput);

            // 只转换正常的字段名，不处理SQL注入内容
            assertEquals(
                    "message['field'] = 'value'; DROP TABLE logs; --",
                    result,
                    "应只转换字段名部分，其他SQL语句保持不变");
        }

        @Test
        @DisplayName("恶意字段名处理 - 期望：过滤或保护恶意字段名")
        void testMaliciousFieldNames() {
            String input = "message.'; DROP TABLE logs; -- = 'test'";
            String result = converter.convertWhereClause(input);

            // 不应转换包含特殊SQL字符的字段名
            assertEquals(input, result, "包含SQL特殊字符的字段名不应被转换");
        }

        @Test
        @DisplayName("超长输入处理 - 期望：防护超长输入攻击")
        void testExtremelyLongInput() {
            StringBuilder longInput = new StringBuilder("message");
            for (int i = 0; i < 10000; i++) {
                longInput.append(".field").append(i);
            }

            // 应该能处理超长输入而不崩溃
            assertDoesNotThrow(
                    () -> {
                        String result = converter.convertTopnField(longInput.toString());
                        assertNotNull(result, "超长输入应能正常处理并返回结果");
                    },
                    "处理超长输入不应抛出异常");
        }

        @Test
        @DisplayName("特殊Unicode字符 - 期望：正确处理Unicode字符")
        void testUnicodeCharacters() {
            String input = "message.中文字段 = '测试值' AND message.emoji😀 = '🚀'";
            String expected = "message['中文字段'] = '测试值' AND message['emoji😀'] = '🚀'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "应正确处理包含Unicode字符的字段名");
        }
    }
}
