package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.builder.condition.SearchConditionManager;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import io.qameta.allure.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LogSqlBuilder单元测试
 *
 * <p>测试秒查系统中日志SQL语句构建器的核心功能 主要验证字段分布查询SQL的TOPN函数AS别名修复
 *
 * <p>测试覆盖范围： 1. 字段分布SQL构建测试 - 验证TOPN函数AS别名的正确性 2. 详细日志查询SQL构建测试 3. 时间分布SQL构建测试 4. 边界条件测试 -
 * 空字段列表、null值处理 5. 搜索条件集成测试
 */
@Epic("秒查日志管理系统")
@Feature("SQL查询引擎")
@Story("SQL语句构建")
@Owner("开发团队")
@ExtendWith(MockitoExtension.class)
class LogSqlBuilderTest {

    @Mock private SearchConditionManager searchConditionManager;

    private LogSqlBuilder logSqlBuilder;

    @BeforeEach
    void setUp() {
        logSqlBuilder = new LogSqlBuilder(searchConditionManager);
    }

    private LogSearchDTO createBasicDTO() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setStartTime("2023-01-01 00:00:00");
        dto.setEndTime("2023-01-02 00:00:00");
        dto.setPageSize(10);
        dto.setOffset(0);
        return dto;
    }

    // ==================== 字段分布SQL构建测试 ====================

    @Nested
    @DisplayName("字段分布SQL构建测试")
    class FieldDistributionSqlTests {

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("单字段TOPN SQL构建 - 验证AS别名修复")
        @Description("验证修复后的TOPN函数能够正确生成AS别名，解决语法错误问题")
        @Issue("TOPN-SQL-FIX")
        void testSingleFieldTopnSqlGeneration() {
            Allure.step(
                    "准备测试数据",
                    () -> {
                        Allure.parameter("字段列表", Arrays.asList("message['level']"));
                        Allure.parameter("表名", "log_table");
                        Allure.parameter("TOPN数量", "5");
                        Allure.parameter("期望AS别名格式", "TOPN(field, 5) AS 'field'");
                    });

            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Arrays.asList("message['level']"); // 括号语法用于TOPN函数
            List<String> originalFields = Arrays.asList("message.level"); // 点语法用于AS别名
            int topN = 5;

            // Mock搜索条件管理器
            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result =
                    Allure.step(
                            "执行字段分布SQL构建",
                            () -> {
                                return logSqlBuilder.buildFieldDistributionSql(
                                        dto, tableName, convertedFields, originalFields, topN);
                            });

            Allure.step(
                    "验证AS别名语法",
                    () -> {
                        System.out.println("实际生成的SQL: " + result);
                        assertTrue(
                                result.contains("TOPN(message['level'], 5) AS 'message.level'"),
                                "TOPN函数应包含正确AS别名，格式为：TOPN(括号语法, 5) AS '点语法'。实际SQL: " + result);

                        Allure.parameter("实际SQL", result);
                        Allure.attachment("SQL构建结果", result);
                    });

            Allure.step(
                    "验证完整SQL结构",
                    () -> {
                        assertTrue(
                                result.contains("TOPN(message['level'], 5) AS 'message.level'"),
                                "应包含正确的TOPN AS别名语法");
                        assertTrue(result.contains("FROM " + tableName), "应包含正确的表名");
                        assertTrue(result.contains("WHERE log_time >= '"), "应包含开始时间条件");
                        assertTrue(result.contains("AND log_time < '"), "应包含结束时间条件");
                    });
        }

        @Test
        @DisplayName("多字段TOPN SQL构建 - 验证每个字段都有AS别名")
        @Severity(SeverityLevel.CRITICAL)
        @Description("验证多个字段的TOPN查询都能正确生成AS别名")
        void testMultipleFieldsTopnSqlGeneration() {
            Allure.step(
                    "准备多字段测试数据",
                    () -> {
                        Allure.parameter("字段数量", "3");
                        Allure.parameter(
                                "字段列表",
                                Arrays.asList("message['level']", "message['service']", "host"));
                        Allure.parameter("期望结果", "每个字段都有独立的AS别名");
                    });

            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields =
                    Arrays.asList("message['level']", "message['service']", "host");
            List<String> originalFields = Arrays.asList("message.level", "message.service", "host");
            int topN = 5;

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result =
                    Allure.step(
                            "执行多字段SQL构建",
                            () -> {
                                return logSqlBuilder.buildFieldDistributionSql(
                                        dto, tableName, convertedFields, originalFields, topN);
                            });

            Allure.step(
                    "验证每个字段的AS别名",
                    () -> {
                        // 验证每个字段都有对应的TOPN AS语法
                        assertTrue(
                                result.contains("TOPN(message['level'], 5) AS 'message.level'"),
                                "第一个字段应有正确的AS别名");
                        assertTrue(
                                result.contains("TOPN(message['service'], 5) AS 'message.service'"),
                                "第二个字段应有正确的AS别名");
                        assertTrue(result.contains("TOPN(host, 5) AS 'host'"), "第三个字段应有正确的AS别名");

                        Allure.parameter("生成的SQL", result);
                    });

            Allure.step(
                    "验证字段分隔符",
                    () -> {
                        // 验证字段之间用逗号分隔
                        assertTrue(result.contains(", "), "多个TOPN字段之间应用逗号分隔");

                        // 计算逗号数量应该是字段数量-1
                        long commaCount = result.chars().filter(ch -> ch == ',').count();
                        assertTrue(commaCount >= 2, "三个字段应至少有2个逗号分隔");
                    });
        }

        @Test
        @DisplayName("带搜索条件的TOPN SQL - 验证搜索条件集成")
        @Severity(SeverityLevel.NORMAL)
        void testTopnSqlWithSearchConditions() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Arrays.asList("message['level']");
            List<String> originalFields = Arrays.asList("message.level");
            int topN = 5;

            String mockConditions = "message['level'] = 'ERROR'";
            when(searchConditionManager.buildSearchConditions(dto)).thenReturn(mockConditions);

            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            Allure.step(
                    "验证搜索条件集成",
                    () -> {
                        assertTrue(result.contains("AND " + mockConditions), "应正确集成搜索条件管理器生成的条件");
                        assertTrue(
                                result.contains("TOPN(message['level'], 5) AS 'message.level'"),
                                "搜索条件不应影响TOPN AS别名的生成");

                        Allure.parameter("集成的搜索条件", mockConditions);
                        Allure.parameter("完整SQL", result);
                    });
        }

        @Test
        @DisplayName("不同TOPN数量测试 - 验证自定义TOPN数量")
        void testDifferentTopnNumbers() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Arrays.asList("message['level']");
            List<String> originalFields = Arrays.asList("message.level");

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            // 测试不同的TOPN数量
            int[] topNValues = {1, 3, 5, 10, 20};

            for (int topN : topNValues) {
                String result =
                        logSqlBuilder.buildFieldDistributionSql(
                                dto, tableName, convertedFields, originalFields, topN);

                Allure.step(
                        "验证TOPN数量: " + topN,
                        () -> {
                            assertTrue(
                                    result.contains("TOPN(message['level'], " + topN + ")"),
                                    "应包含正确的TOPN数量参数: " + topN);
                            assertTrue(result.contains("AS 'message.level'"), "AS别名不应受TOPN数量影响");

                            Allure.parameter("TOPN数量", topN);
                            Allure.parameter(
                                    "生成SQL片段",
                                    "TOPN(message['level'], " + topN + ") AS 'message.level'");
                        });
            }
        }

        @Test
        @DisplayName("空字段列表处理 - 验证空SELECT列表")
        @Severity(SeverityLevel.MINOR)
        void testEmptyFieldsList() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Collections.emptyList();
            List<String> originalFields = Collections.emptyList();
            int topN = 5;

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            Allure.step(
                    "验证空字段列表处理",
                    () -> {
                        assertTrue(result.contains("SELECT  FROM"), "空字段列表应生成空的SELECT列表");
                        assertTrue(result.contains("FROM " + tableName), "表名部分应正常");

                        Allure.parameter("空字段列表SQL", result);
                    });
        }

        @Test
        @DisplayName("修复前后对比测试 - 验证语法错误修复")
        void testSyntaxErrorFix() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> fields = Arrays.asList("message['time']");
            int topN = 5;

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            List<String> convertedFields = Arrays.asList("message['time']");
            List<String> originalFields = Arrays.asList("message.time");

            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            // 验证修复后的正确语法：TOPN(括号语法, 5) AS '点语法'
            assertTrue(
                    result.contains("TOPN(message['time'], 5) AS 'message.time'"),
                    "应生成正确的TOPN AS语法");

            // 验证不包含错误的语法：TOPN(field AS 'alias', 5)
            assertFalse(
                    result.contains("TOPN(message['time'] AS 'message.time', 5)"), "不应包含错误的AS位置");

            // 验证解决了原始错误中的语法问题
            assertFalse(
                    result.matches(".*TOPN\\([^,]+\\s+AS\\s+[^,]+,\\s*\\d+\\).*"),
                    "TOPN函数内部不应包含AS语法");
        }
    }

    // ==================== 详细日志查询SQL构建测试 ====================

    @Nested
    @DisplayName("详细日志查询SQL构建测试")
    class DetailSqlTests {

        @Test
        @DisplayName("基本详细查询SQL - 验证分页和排序")
        void testBasicDetailSql() {
            LogSearchDTO dto = createBasicDTO();
            dto.setFields(Arrays.asList("log_time", "message", "level"));
            String tableName = "log_table";

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result = logSqlBuilder.buildDetailSql(dto, tableName);

            assertTrue(result.contains("SELECT log_time, message, level"), "应包含指定的字段列表");
            assertTrue(result.contains("FROM " + tableName), "应包含表名");
            assertTrue(result.contains("ORDER BY log_time DESC"), "应按时间倒序排序");
            assertTrue(result.contains("LIMIT 10 OFFSET 0"), "应包含分页参数");
        }

        @Test
        @DisplayName("无字段列表的详细查询 - 验证SELECT *")
        void testDetailSqlWithoutFields() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result = logSqlBuilder.buildDetailSql(dto, tableName);

            assertTrue(result.contains("SELECT *"), "无字段列表时应使用SELECT *");
        }
    }

    // ==================== 时间分布SQL构建测试 ====================

    @Nested
    @DisplayName("时间分布SQL构建测试")
    class DistributionSqlTests {

        @Test
        @DisplayName("基本时间分布SQL - 验证包含时间分组函数")
        void testBasicDistributionSql() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            String timeUnit = "minute";

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result = logSqlBuilder.buildDistributionSql(dto, tableName, timeUnit);

            assertTrue(result.contains("date_trunc(log_time, 'minute')"), "应包含时间截断函数");
            assertTrue(result.contains("COUNT(1) AS count"), "应包含计数聚合");
            assertTrue(result.contains("GROUP BY date_trunc(log_time, 'minute')"), "应包含时间分组");
            assertTrue(result.contains("ORDER BY log_time_ ASC"), "应按时间升序排序");
        }
    }

    // ==================== 搜索条件构建测试 ====================

    @Nested
    @DisplayName("搜索条件构建测试")
    class SearchConditionTests {

        @Test
        @DisplayName("仅构建搜索条件 - 验证返回纯搜索条件字符串")
        void testBuildSearchConditionsOnly() {
            LogSearchDTO dto = createBasicDTO();
            String expectedConditions = "level = 'ERROR'";

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn(expectedConditions);

            String result = logSqlBuilder.buildSearchConditionsOnly(dto);

            assertEquals(expectedConditions, result, "应返回搜索条件管理器生成的条件");
            verify(searchConditionManager).buildSearchConditions(dto);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("空搜索条件处理 - 验证不添加多余AND")
        void testEmptySearchConditions() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Arrays.asList("level");
            List<String> originalFields = Arrays.asList("level");

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn("");

            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, 5);

            // 验证没有多余的AND
            assertFalse(result.contains("AND  "), "空搜索条件不应产生多余的AND");
            assertFalse(result.contains("AND ''"), "空搜索条件不应产生空的AND条件");
        }

        @Test
        @DisplayName("null搜索条件处理 - 验证正常处理null")
        void testNullSearchConditions() {
            LogSearchDTO dto = createBasicDTO();
            String tableName = "log_table";
            List<String> convertedFields = Arrays.asList("level");
            List<String> originalFields = Arrays.asList("level");

            when(searchConditionManager.buildSearchConditions(dto)).thenReturn(null);

            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, 5);

            // 应该不包含额外的AND条件，只有时间条件
            assertTrue(result.contains("WHERE log_time >= "), "应包含开始时间条件");
            assertTrue(result.contains("AND log_time < "), "应包含结束时间条件");

            // 不应该有多余的AND
            String withoutTimeConditions =
                    result.replaceAll("WHERE log_time >= '[^']*' AND log_time < '[^']*'", "");
            assertFalse(withoutTimeConditions.contains(" AND "), "除时间条件外不应有其他AND条件");
        }
    }
}
