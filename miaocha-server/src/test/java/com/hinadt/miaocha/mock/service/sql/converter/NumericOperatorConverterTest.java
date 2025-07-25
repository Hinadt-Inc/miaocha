package com.hinadt.miaocha.mock.service.sql.converter;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.service.sql.converter.NumericOperatorConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NumericOperatorConverter单元测试 */
class NumericOperatorConverterTest {

    private NumericOperatorConverter converter;

    @BeforeEach
    void setUp() {
        converter = new NumericOperatorConverter();
    }

    @Test
    void testBasicConversion() {
        // 基本转换
        assertEquals(
                "message.data.count > 100*1",
                converter.convertNumericOperators("message.data.count > 100"));

        // 不同运算符
        assertEquals(
                "message.data.value >= 50*1",
                converter.convertNumericOperators("message.data.value >= 50"));
        assertEquals(
                "message.data.size < 1024*1",
                converter.convertNumericOperators("message.data.size < 1024"));
        assertEquals(
                "message.data.code = 404*1",
                converter.convertNumericOperators("message.data.code = 404"));
    }

    @Test
    void testNumberFormats() {
        // 小数
        assertEquals(
                "message.data.rate > 3.14*1",
                converter.convertNumericOperators("message.data.rate > 3.14"));

        // 负数
        assertEquals(
                "message.data.temp < -10*1",
                converter.convertNumericOperators("message.data.temp < -10"));

        // 负小数
        assertEquals(
                "message.data.offset <= -25.5*1",
                converter.convertNumericOperators("message.data.offset <= -25.5"));

        // 科学计数法（不应转换）
        assertEquals(
                "message.data.value > 1e5",
                converter.convertNumericOperators("message.data.value > 1e5"));
    }

    @Test
    void testNonVariantFields() {
        // 普通字段不转换
        assertEquals("count > 100", converter.convertNumericOperators("count > 100"));
        assertEquals("level = 5", converter.convertNumericOperators("level = 5"));
    }

    @Test
    void testQuotedContent() {
        // 引号内不转换
        assertEquals(
                "message.text = 'count > 100'",
                converter.convertNumericOperators("message.text = 'count > 100'"));
        assertEquals(
                "message.sql = \"value >= 50\"",
                converter.convertNumericOperators("message.sql = \"value >= 50\""));
    }

    @Test
    void testEdgeCases() {
        // 空值
        assertNull(converter.convertNumericOperators(null));
        assertEquals("", converter.convertNumericOperators(""));
        assertEquals("   ", converter.convertNumericOperators("   "));

        // 已有运算符
        assertEquals(
                "message.data.value > 100*2",
                converter.convertNumericOperators("message.data.value > 100*2"));
        assertEquals(
                "message.data.value > 100+1",
                converter.convertNumericOperators("message.data.value > 100+1"));

        // 多条件
        assertEquals(
                "message.data.count > 100*1 AND message.data.size < 200*1",
                converter.convertNumericOperators(
                        "message.data.count > 100 AND message.data.size < 200"));

        // LIKE语句中的引号内容（不应转换）
        assertEquals(
                "message.msg like 'a > 100'",
                converter.convertNumericOperators("message.msg like 'a > 100'"));

        // 混合场景：LIKE + 正常比较
        assertEquals(
                "message.data.count > 50*1 AND message.msg like 'value > 100'",
                converter.convertNumericOperators(
                        "message.data.count > 50 AND message.msg like 'value > 100'"));
    }

    @Test
    void testComplexScenarios() {
        // 复杂嵌套场景：引号内包含复杂表达式，引号外有正常比较
        assertEquals(
                "( message.msg like 'marker.duration >= 100 and marker.a < 100' and message.aaa <"
                        + " 200.0*1 ) or mess.a100.c100 < 20*1",
                converter.convertNumericOperators(
                        "( message.msg like 'marker.duration >= 100 and marker.a < 100' and"
                                + " message.aaa < 200.0 ) or mess.a100.c100 < 20"));

        // 多层嵌套括号和复杂逻辑
        assertEquals(
                "((message.data.value > 100*1 AND message.info.count <= 50*1) OR"
                        + " (message.status.code = 200*1)) AND message.msg like 'error >= 500'",
                converter.convertNumericOperators(
                        "((message.data.value > 100 AND message.info.count <= 50) OR"
                            + " (message.status.code = 200)) AND message.msg like 'error >= 500'"));

        // 混合引号类型（单引号和双引号）
        assertEquals(
                "message.data.score > 85*1 AND message.msg like \"value >= 100\" AND"
                        + " message.info.level < 3*1",
                converter.convertNumericOperators(
                        "message.data.score > 85 AND message.msg like \"value >= 100\" AND"
                                + " message.info.level < 3"));

        // 包含IN语句和复杂条件
        assertEquals(
                "message.data.type IN ('A', 'B', 'C') AND message.data.value > 100*1 AND"
                        + " message.msg like 'status >= 200'",
                converter.convertNumericOperators(
                        "message.data.type IN ('A', 'B', 'C') AND message.data.value > 100 AND"
                                + " message.msg like 'status >= 200'"));

        // 极端复杂场景：多种运算符、嵌套引号、科学计数法
        assertEquals(
                "(message.data.count >= 1000*1 OR message.info.size <= 2.5*1) AND message.log like"
                        + " 'value = 1e5 and count > 999' AND message.stats.ratio != 0.75*1",
                converter.convertNumericOperators(
                        "(message.data.count >= 1000 OR message.info.size <= 2.5) AND message.log"
                                + " like 'value = 1e5 and count > 999' AND message.stats.ratio !="
                                + " 0.75"));
    }

    @Test
    void testNestedAndEscapedQuotes() {
        // 测试转义引号处理
        assertEquals(
                "message.data.value > 100*1 AND message.text = 'He said \\\"Hello\\\"'",
                converter.convertNumericOperators(
                        "message.data.value > 100 AND message.text = 'He said \\\"Hello\\\"'"));

        // 测试单引号内的转义单引号
        assertEquals(
                "message.data.count >= 50*1 AND message.msg = 'It\\\'s working'",
                converter.convertNumericOperators(
                        "message.data.count >= 50 AND message.msg = 'It\\\'s working'"));

        // 测试双引号内的转义双引号
        assertEquals(
                "message.data.size < 200*1 AND message.json = \"{\\\"key\\\":\\\"value\\\"}\"",
                converter.convertNumericOperators(
                        "message.data.size < 200 AND message.json ="
                                + " \"{\\\"key\\\":\\\"value\\\"}\""));

        // 测试复杂的转义场景：引号内包含反斜杠和引号
        assertEquals(
                "message.data.level = 3*1 AND message.path = 'C:\\\\folder\\\\file.txt'",
                converter.convertNumericOperators(
                        "message.data.level = 3 AND message.path = 'C:\\\\folder\\\\file.txt'"));

        // 测试混合转义：单引号内有转义双引号，双引号内有转义单引号
        assertEquals(
                "message.data.score > 90*1 AND message.quote1 = 'Say \\\"Hi\\\"' AND message.quote2"
                        + " = \"Don\\\'t worry\"",
                converter.convertNumericOperators(
                        "message.data.score > 90 AND message.quote1 = 'Say \\\"Hi\\\"' AND"
                                + " message.quote2 = \"Don\\\'t worry\""));

        // 测试不完整的引号（未闭合的引号）
        assertEquals(
                "message.data.value > 100*1 AND message.text = 'unclosed quote",
                converter.convertNumericOperators(
                        "message.data.value > 100 AND message.text = 'unclosed quote"));

        // 测试连续的转义字符
        assertEquals(
                "message.data.count < 50*1 AND message.escape = 'test\\\\\\\\end'",
                converter.convertNumericOperators(
                        "message.data.count < 50 AND message.escape = 'test\\\\\\\\end'"));

        // 测试引号内包含数字比较表达式但不应被转换
        assertEquals(
                "message.data.actual > 100*1 AND message.condition = 'if (value >= 200) return"
                        + " true'",
                converter.convertNumericOperators(
                        "message.data.actual > 100 AND message.condition = 'if (value >= 200)"
                                + " return true'"));

        // 测试嵌套引号场景：外层单引号，内层双引号
        assertEquals(
                "message.data.priority = 1*1 AND message.nested = 'outer \\\"inner content\\\""
                        + " text'",
                converter.convertNumericOperators(
                        "message.data.priority = 1 AND message.nested = 'outer \\\"inner"
                                + " content\\\" text'"));

        // 测试嵌套引号场景：外层双引号，内层单引号
        assertEquals(
                "message.data.status != 0*1 AND message.nested = \"outer \\\'inner content\\\'"
                        + " text\"",
                converter.convertNumericOperators(
                        "message.data.status != 0 AND message.nested = \"outer \\\'inner"
                                + " content\\\' text\""));

        // 测试复杂的SQL场景：包含LIKE、IN和转义引号
        assertEquals(
                "message.data.code = 404*1 AND message.sql LIKE 'SELECT * FROM table WHERE name ="
                        + " \\\'John\\\'' AND message.data.type IN ('A', 'B')",
                converter.convertNumericOperators(
                        "message.data.code = 404 AND message.sql LIKE 'SELECT * FROM table WHERE"
                                + " name = \\\'John\\\'' AND message.data.type IN ('A', 'B')"));

        // 测试极端情况：多层转义和嵌套
        assertEquals(
                "message.data.depth >= 5*1 AND message.complex = 'level1 \\\"level2"
                        + " \\\\\\\'level3\\\\\\\' back2\\\" back1'",
                converter.convertNumericOperators(
                        "message.data.depth >= 5 AND message.complex = 'level1 \\\"level2"
                                + " \\\\\\\'level3\\\\\\\' back2\\\" back1'"));
    }

    @Test
    void testQuotePatternEdgeCases() {
        // 测试空引号
        assertEquals(
                "message.data.value > 100*1 AND message.empty = ''",
                converter.convertNumericOperators(
                        "message.data.value > 100 AND message.empty = ''"));

        // 测试只包含空格的引号
        assertEquals(
                "message.data.count < 50*1 AND message.spaces = '   '",
                converter.convertNumericOperators(
                        "message.data.count < 50 AND message.spaces = '   '"));

        // 测试引号紧邻数字比较
        assertEquals(
                "message.data.score>=90*1AND message.text='test'",
                converter.convertNumericOperators("message.data.score>=90AND message.text='test'"));

        // 测试多个连续的引号对
        assertEquals(
                "message.data.value > 100*1 AND message.a = 'first' AND message.b = 'second' AND"
                        + " message.data.count < 50*1",
                converter.convertNumericOperators(
                        "message.data.value > 100 AND message.a = 'first' AND message.b = 'second'"
                                + " AND message.data.count < 50"));

        // 测试引号内包含特殊字符
        assertEquals(
                "message.data.level = 1*1 AND message.special = 'test@#$%^&*()_+-=[]{}|;:,.<>?'",
                converter.convertNumericOperators(
                        "message.data.level = 1 AND message.special ="
                                + " 'test@#$%^&*()_+-=[]{}|;:,.<>?'"));

        // 测试引号内包含换行符和制表符
        assertEquals(
                "message.data.code = 200*1 AND message.multiline = 'line1\\nline2\\tindented'",
                converter.convertNumericOperators(
                        "message.data.code = 200 AND message.multiline = 'line1\\n"
                                + "line2\\tindented'"));

        // 测试Unicode字符在引号内
        assertEquals(
                "message.data.priority >= 5*1 AND message.unicode = '测试中文字符'",
                converter.convertNumericOperators(
                        "message.data.priority >= 5 AND message.unicode = '测试中文字符'"));

        // 测试引号内包含SQL关键字
        assertEquals(
                "message.data.status != 0*1 AND message.sql = 'SELECT COUNT(*) FROM table WHERE id"
                        + " > 100'",
                converter.convertNumericOperators(
                        "message.data.status != 0 AND message.sql = 'SELECT COUNT(*) FROM table"
                                + " WHERE id > 100'"));
    }
}
