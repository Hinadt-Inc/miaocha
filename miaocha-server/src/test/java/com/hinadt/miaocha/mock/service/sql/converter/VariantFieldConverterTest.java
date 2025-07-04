package com.hinadt.miaocha.mock.service.sql.converter;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
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
        @DisplayName("简单点语法WHERE条件转换 - 期望：request.method转换为request['method']")
        void testSimpleDotSyntaxWhereConversion() {
            String input = "request.method = 'POST'";
            List<String> result = converter.convertWhereClauses(Arrays.asList(input));

            assertEquals(1, result.size(), "转换结果应包含1个条件");
            assertEquals("request['method'] = 'POST'", result.get(0), "简单点语法应正确转换为bracket语法");
        }

        @Test
        @DisplayName("嵌套点语法WHERE条件转换 - 期望：business.order.status转换为business['order']['status']")
        void testNestedDotSyntaxWhereConversion() {
            String input = "business.order.status = 'paid' AND business.user.level = 'vip'";
            List<String> result = converter.convertWhereClauses(Arrays.asList(input));

            assertEquals(1, result.size());
            String converted = result.get(0);
            assertTrue(converted.contains("business['order']['status'] = 'paid'"), "嵌套字段应正确转换");
            assertTrue(converted.contains("business['user']['level'] = 'vip'"), "多个嵌套字段都应转换");
        }

        @Test
        @DisplayName("深层嵌套点语法WHERE条件转换 - 期望：复杂嵌套结构正确转换")
        void testDeepNestedDotSyntaxWhereConversion() {
            String input = "trace.spans.operations.duration > 1000";
            List<String> result = converter.convertWhereClauses(Arrays.asList(input));

            assertEquals(1, result.size());
            assertEquals(
                    "trace['spans']['operations']['duration'] > 1000",
                    result.get(0),
                    "深层嵌套字段应正确转换");
        }

        @Test
        @DisplayName("混合条件WHERE语句转换 - 期望：只转换点语法字段，保留普通字段")
        void testMixedWhereClauseConversion() {
            String input = "level = 'ERROR' AND request.user_id = '123' AND host LIKE '%prod%'";
            List<String> result = converter.convertWhereClauses(Arrays.asList(input));

            assertEquals(1, result.size());
            String converted = result.get(0);
            assertTrue(converted.contains("level = 'ERROR'"), "普通字段应保持不变");
            assertTrue(converted.contains("request['user_id'] = '123'"), "点语法字段应转换");
            assertTrue(converted.contains("host LIKE '%prod%'"), "普通字段应保持不变");
        }

        @Test
        @DisplayName("值中包含点号的WHERE条件 - 期望：值中的点号不被转换")
        void testWhereClauseWithDotsInValues() {
            String input = "request.path = '/api/v1.0/users' AND config.version = '2.1.0'";
            List<String> result = converter.convertWhereClauses(Arrays.asList(input));

            assertEquals(1, result.size());
            String converted = result.get(0);
            assertTrue(
                    converted.contains("request['path'] = '/api/v1.0/users'"),
                    "字段名应转换，但值中的点号应保持不变");
            assertTrue(converted.contains("config['version'] = '2.1.0'"), "字段名应转换，但值中的点号应保持不变");
        }
    }

    // ==================== SELECT字段转换测试 ====================

    @Nested
    @DisplayName("SELECT字段转换测试")
    class SelectFieldConversionTests {

        @Test
        @DisplayName("简单字段转换 - 期望：request.method转换为request['method']")
        void testSimpleFieldConversion() {
            List<String> input = Arrays.asList("request.method", "host", "log_time");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(3, result.size());
            assertEquals("request['method']", result.get(0), "点语法字段应转换为bracket语法");
            assertEquals("host", result.get(1), "普通字段应保持不变");
            assertEquals("log_time", result.get(2), "普通字段应保持不变");
        }

        @Test
        @DisplayName("嵌套字段转换 - 期望：多层嵌套字段正确转换")
        void testNestedFieldConversion() {
            List<String> input =
                    Arrays.asList(
                            "user.profile.name",
                            "business.order.payment.method",
                            "trace.spans.operations.duration.ms");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(3, result.size());
            assertEquals("user['profile']['name']", result.get(0), "2层嵌套字段应正确转换");
            assertEquals("business['order']['payment']['method']", result.get(1), "3层嵌套字段应正确转换");
            assertEquals(
                    "trace['spans']['operations']['duration']['ms']", result.get(2), "4层嵌套字段应正确转换");
        }

        @Test
        @DisplayName("混合字段列表转换 - 期望：只转换需要转换的字段")
        void testMixedFieldListConversion() {
            List<String> input =
                    Arrays.asList(
                            "log_time",
                            "service_name",
                            "request.method",
                            "response.status_code",
                            "host");
            List<String> result = converter.convertSelectFields(input);

            assertEquals(5, result.size());
            assertEquals("log_time", result.get(0), "普通字段应保持不变");
            assertEquals("service_name", result.get(1), "普通字段应保持不变");
            assertEquals("request['method']", result.get(2), "点语法字段应转换");
            assertEquals("response['status_code']", result.get(3), "点语法字段应转换");
            assertEquals("host", result.get(4), "普通字段应保持不变");
        }
    }

    // ==================== TOPN字段转换测试 ====================

    @Nested
    @DisplayName("TOPN字段转换测试")
    class TopnFieldConversionTests {

        @Test
        @DisplayName("TOPN字段转换 - 期望：点语法转换为bracket语法")
        void testTopnFieldConversion() {
            String input = "business.sales.region";
            String result = converter.convertTopnField(input);

            assertEquals("business['sales']['region']", result, "TOPN字段应正确转换");
        }

        @Test
        @DisplayName("普通TOPN字段 - 期望：普通字段保持不变")
        void testRegularTopnField() {
            String input = "level";
            String result = converter.convertTopnField(input);

            assertEquals("level", result, "普通TOPN字段应保持不变");
        }

        @Test
        @DisplayName("null TOPN字段 - 期望：null输入返回null")
        void testNullTopnField() {
            String result = converter.convertTopnField(null);
            assertNull(result, "null输入应返回null");
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
            assertEquals("message['level']", result.get(0));
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
        @DisplayName("以数字开头的根字段名处理 - 期望：以数字开头的根字段名不转换")
        void testNumericStartRootFieldNames() {
            String input = "123field.subfield = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(input, result, "以数字开头的根字段名不符合标识符规范，不应转换");
        }

        @Test
        @DisplayName("以数字开头的子字段名处理 - 期望：以数字开头的子字段名正常转换")
        void testNumericStartSubFieldNames() {
            String input = "message.123subfield = 'test'";
            String expected = "message['123subfield'] = 'test'";
            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "以数字开头的子字段名应正常转换，因为Doris Variant支持");
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
        @DisplayName("哈希字段名处理 - 期望：以哈希值作为子字段名的情况正常转换")
        void testHashFieldNames() {
            // 模拟实际bug场景中的字段名
            String input = "message.marker.data.176d23818d6bcdbb9700735a08418b63";
            String expected = "message['marker']['data']['176d23818d6bcdbb9700735a08418b63']";

            List<String> fields = Arrays.asList(input);
            List<String> result = converter.convertSelectFields(fields);

            assertEquals(1, result.size(), "应正确转换一个字段");
            assertEquals(expected, result.get(0), "哈希子字段名应正确转换为bracket语法");
        }

        @Test
        @DisplayName("数字字面量处理 - 期望：数字字面量不被当作字段名转换")
        void testNumericLiteralHandling() {
            String input = "temperature > 25.5 AND humidity < 60.0";
            String result = converter.convertWhereClause(input);

            assertEquals(input, result, "数字字面量不应被当作字段名转换");
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
        @DisplayName("真实微服务日志字段测试 - 期望：模拟微服务架构中实际的日志字段")
        void testRealMicroserviceLogFields() {
            List<String> fields =
                    Arrays.asList(
                            // 服务基础信息
                            "service_info.name",
                            "service_info.version",
                            "service_info.instance_id",
                            // 请求信息
                            "request.method",
                            "request.path",
                            "request.headers.user_agent",
                            "request.headers.x_request_id",
                            // 响应信息
                            "response.status_code",
                            "response.content_length",
                            "response.headers.content_type",
                            // 性能指标
                            "performance.response_time_ms",
                            "performance.db_query_time_ms",
                            "performance.cache_hit_rate",
                            // 追踪信息
                            "trace.trace_id",
                            "trace.span_id",
                            "trace.parent_span_id",
                            // 用户信息
                            "user.id",
                            "user.role",
                            "user.session.id",
                            // 错误信息
                            "error.code",
                            "error.message",
                            "error.stack_trace.class",
                            // 业务数据
                            "business_data.order_id",
                            "business_data.product.category",
                            "business_data.payment.method");

            List<String> result = converter.convertSelectFields(fields);

            // 验证所有字段都被正确转换
            assertEquals(fields.size(), result.size(), "转换后字段数量应保持一致");

            // 验证几个关键字段的转换
            assertTrue(result.contains("service_info['name']"), "服务名称字段应正确转换");
            assertTrue(result.contains("request['headers']['user_agent']"), "请求头字段应正确转换");
            assertTrue(result.contains("trace['trace_id']"), "追踪ID字段应正确转换");
            assertTrue(result.contains("business_data['product']['category']"), "业务数据嵌套字段应正确转换");
        }

        @Test
        @DisplayName("真实电商业务WHERE条件测试 - 期望：模拟电商系统实际的查询条件")
        void testRealEcommerceWhereConditions() {
            String input =
                    "order_info.status = 'paid' AND order_info.payment.method = 'credit_card' AND"
                        + " user_info.level = 'vip' AND product_info.category.main = 'electronics'"
                        + " AND logistics_info.delivery.city = 'Shanghai'";
            String expected =
                    "order_info['status'] = 'paid' AND order_info['payment']['method'] ="
                            + " 'credit_card' AND user_info['level'] = 'vip' AND"
                            + " product_info['category']['main'] = 'electronics' AND"
                            + " logistics_info['delivery']['city'] = 'Shanghai'";

            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "真实电商业务查询条件应正确转换所有variant字段");
        }

        @Test
        @DisplayName("真实物联网设备数据查询测试 - 期望：处理物联网设备复杂的嵌套数据结构")
        void testRealIoTDeviceDataQuery() {
            String input =
                    "device_info.type = 'sensor' AND sensor_data.temperature.value > 25.5 AND"
                        + " sensor_data.humidity.percentage < 60 AND"
                        + " device_status.network.signal_strength >= -70 AND"
                        + " location_info.building.floor = '3' AND alert_config.threshold.critical"
                        + " = true";
            String expected =
                    "device_info['type'] = 'sensor' AND sensor_data['temperature']['value'] > 25.5"
                            + " AND sensor_data['humidity']['percentage'] < 60 AND"
                            + " device_status['network']['signal_strength'] >= -70 AND"
                            + " location_info['building']['floor'] = '3' AND"
                            + " alert_config['threshold']['critical'] = true";

            String result = converter.convertWhereClause(input);

            assertEquals(expected, result, "物联网设备数据查询应正确处理复杂嵌套的variant字段");
        }

        @Test
        @DisplayName("真实金融交易日志场景 - 期望：处理金融系统的复杂字段结构")
        void testRealFinancialTransactionLogScenario() {
            List<String> fields =
                    Arrays.asList(
                            // 交易基础信息
                            "transaction.id",
                            "transaction.type",
                            "transaction.amount.value",
                            "transaction.amount.currency",
                            // 账户信息
                            "account.from.id",
                            "account.from.type",
                            "account.to.id",
                            "account.to.type",
                            // 风控信息
                            "risk_assessment.score",
                            "risk_assessment.level",
                            "risk_assessment.rules.triggered",
                            // 合规信息
                            "compliance.aml_check.status",
                            "compliance.kyc_status",
                            "compliance.regulatory.region",
                            // 系统信息
                            "system.processor.id",
                            "system.timestamp.created",
                            "system.audit.trail");

            List<String> result = converter.convertSelectFields(fields);

            // 验证字段数量
            assertEquals(fields.size(), result.size(), "金融交易字段转换后数量应保持一致");

            // 验证关键字段转换
            assertTrue(result.contains("transaction['amount']['value']"), "交易金额字段应正确转换");
            assertTrue(result.contains("risk_assessment['rules']['triggered']"), "风控规则字段应正确转换");
            assertTrue(result.contains("compliance['aml_check']['status']"), "合规检查字段应正确转换");
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
            assertEquals("message['field0']['subfield0']", result.get(0));
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
