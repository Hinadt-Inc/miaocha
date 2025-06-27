package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.*;
import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LogSqlBuilder门面类测试
 *
 * <p>验证门面类的委托逻辑，确保正确调用各专门的构建器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogSqlBuilder门面类测试")
class LogSqlBuilderTest {

    @Mock private DistributionSqlBuilder distributionSqlBuilder;

    @Mock private DetailSqlBuilder detailSqlBuilder;

    @Mock private FieldDistributionSqlBuilder fieldDistributionSqlBuilder;

    @Mock private KeywordConditionBuilder keywordConditionBuilder;

    @Mock private QueryConfigValidationService queryConfigValidationService;

    @Mock private WhereConditionBuilder whereConditionBuilder;

    private LogSqlBuilder logSqlBuilder;

    private LogSearchDTO testDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 手动创建LogSqlBuilder而不是使用@InjectMocks
        logSqlBuilder =
                new LogSqlBuilder(
                        distributionSqlBuilder,
                        detailSqlBuilder,
                        fieldDistributionSqlBuilder,
                        keywordConditionBuilder,
                        queryConfigValidationService);

        testDto = new LogSearchDTO();
        testDto.setModule("test");
        testDto.setStartTime("2024-01-01 00:00:00.000");
        testDto.setEndTime("2024-01-01 01:00:00.000");
        testDto.setPageSize(100);
        testDto.setOffset(0);
    }

    @Test
    @DisplayName("分布统计SQL构建委托测试 - 验证正确调用DistributionSqlBuilder")
    void testBuildDistributionSqlWithInterval() {
        // Arrange
        String tableName = "test_table";
        String timeUnit = "minute";
        int intervalValue = 5;
        String expectedTimeField = "log_time";
        String mockSql = "SELECT * FROM test_table";

        when(queryConfigValidationService.getTimeField("test")).thenReturn(expectedTimeField);
        when(distributionSqlBuilder.buildCustomIntervalDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(expectedTimeField),
                        eq(timeUnit),
                        eq(intervalValue)))
                .thenReturn(mockSql);

        // Act
        String result =
                logSqlBuilder.buildDistributionSqlWithInterval(
                        testDto, tableName, timeUnit, intervalValue);

        // Assert
        assertEquals(mockSql, result);
        verify(queryConfigValidationService, times(1)).getTimeField("test");
        verify(distributionSqlBuilder, times(1))
                .buildCustomIntervalDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(expectedTimeField),
                        eq(timeUnit),
                        eq(intervalValue));
    }

    @Test
    @DisplayName("详细查询SQL构建委托测试 - 验证正确调用DetailSqlBuilder")
    void testBuildDetailQuery() {
        // Arrange
        String tableName = "test_table";
        String timeField = "log_time";
        String mockSql = "SELECT * FROM test_table";

        when(detailSqlBuilder.buildDetailQuery(
                        any(LogSearchDTO.class), eq(tableName), eq(timeField)))
                .thenReturn(mockSql);

        // Act
        String result = logSqlBuilder.buildDetailQuery(testDto, tableName, timeField);

        // Assert
        assertEquals(mockSql, result);
        verify(detailSqlBuilder, times(1))
                .buildDetailQuery(any(LogSearchDTO.class), eq(tableName), eq(timeField));
    }

    @Test
    @DisplayName("总数查询SQL构建委托测试 - 验证正确调用DetailSqlBuilder")
    void testBuildCountQuery() {
        // Arrange
        String tableName = "test_table";
        String timeField = "log_time";
        String mockSql = "SELECT COUNT(1) FROM test_table";

        when(detailSqlBuilder.buildCountQuery(
                        any(LogSearchDTO.class), eq(tableName), eq(timeField)))
                .thenReturn(mockSql);

        // Act
        String result = logSqlBuilder.buildCountQuery(testDto, tableName, timeField);

        // Assert
        assertEquals(mockSql, result);
        verify(detailSqlBuilder, times(1))
                .buildCountQuery(any(LogSearchDTO.class), eq(tableName), eq(timeField));
    }

    @Test
    @DisplayName("字段分布SQL构建委托测试 - 验证正确调用FieldDistributionSqlBuilder")
    void testBuildFieldDistributionSql() {
        // Arrange
        String tableName = "test_table";
        List<String> fields = Arrays.asList("level", "service");
        List<String> originalFields = Arrays.asList("level", "service");
        int topN = 10;
        String expectedTimeField = "log_time";
        String mockSql = "SELECT TOPN(level, 10) FROM test_table";

        when(queryConfigValidationService.getTimeField("test")).thenReturn(expectedTimeField);
        when(fieldDistributionSqlBuilder.buildFieldDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(expectedTimeField),
                        eq(fields),
                        eq(originalFields),
                        eq(topN)))
                .thenReturn(mockSql);

        // Act
        String result =
                logSqlBuilder.buildFieldDistributionSql(
                        testDto, tableName, fields, originalFields, topN);

        // Assert
        assertEquals(mockSql, result);
        verify(queryConfigValidationService, times(1)).getTimeField("test");
        verify(fieldDistributionSqlBuilder, times(1))
                .buildFieldDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(expectedTimeField),
                        eq(fields),
                        eq(originalFields),
                        eq(topN));
    }

    @Test
    @DisplayName("时间字段配置异常处理测试 - 验证默认值返回")
    void testGetTimeFieldWithException() {
        // Arrange
        String tableName = "test_table";
        String timeUnit = "minute";
        int intervalValue = 5;
        String defaultTimeField = "log_time";
        String mockSql = "SELECT * FROM test_table";

        when(queryConfigValidationService.getTimeField("test"))
                .thenThrow(new RuntimeException("配置未找到"));
        when(distributionSqlBuilder.buildCustomIntervalDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(defaultTimeField),
                        eq(timeUnit),
                        eq(intervalValue)))
                .thenReturn(mockSql);

        // Act
        String result =
                logSqlBuilder.buildDistributionSqlWithInterval(
                        testDto, tableName, timeUnit, intervalValue);

        // Assert
        assertEquals(mockSql, result);
        verify(queryConfigValidationService, times(1)).getTimeField("test");
        verify(distributionSqlBuilder, times(1))
                .buildCustomIntervalDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(defaultTimeField),
                        eq(timeUnit),
                        eq(intervalValue));
    }

    @Test
    @DisplayName("字段分布查询时间字段异常处理测试 - 验证默认值返回")
    void testFieldDistributionWithTimeFieldException() {
        // Arrange
        String tableName = "test_table";
        List<String> fields = List.of("level");
        List<String> originalFields = List.of("level");
        int topN = 5;
        String defaultTimeField = "log_time";
        String mockSql = "SELECT TOPN(level, 5) FROM test_table";

        when(queryConfigValidationService.getTimeField("test"))
                .thenThrow(new RuntimeException("配置未找到"));
        when(fieldDistributionSqlBuilder.buildFieldDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(defaultTimeField),
                        eq(fields),
                        eq(originalFields),
                        eq(topN)))
                .thenReturn(mockSql);

        // Act
        String result =
                logSqlBuilder.buildFieldDistributionSql(
                        testDto, tableName, fields, originalFields, topN);

        // Assert
        assertEquals(mockSql, result);
        verify(queryConfigValidationService, times(1)).getTimeField("test");
        verify(fieldDistributionSqlBuilder, times(1))
                .buildFieldDistribution(
                        any(LogSearchDTO.class),
                        eq(tableName),
                        eq(defaultTimeField),
                        eq(fields),
                        eq(originalFields),
                        eq(topN));
    }

    @Test
    @DisplayName("完整SQL生成测试 - 关键字搜索+WHERE条件+字段选择")
    void testCompleteDetailSqlGeneration() {
        // KeywordConditionBuilder需要验证服务，所以继续用mock的方式
        when(keywordConditionBuilder.buildKeywordConditions(any(LogSearchDTO.class)))
                .thenReturn("((message_text LIKE '%error%') AND (level MATCH_PHRASE 'ERROR'))");

        // WhereConditionBuilder可以用真实实例
        WhereConditionBuilder realWhereBuilder = new WhereConditionBuilder();
        DetailSqlBuilder realDetailBuilder =
                new DetailSqlBuilder(keywordConditionBuilder, realWhereBuilder);

        // 设置复杂的搜索条件
        testDto.setWhereSqls(Arrays.asList("service = 'user-service'", "host LIKE 'prod-%'"));
        testDto.setFields(Arrays.asList("log_time", "level", "message_text", "service", "host"));

        // 生成SQL
        String sql = realDetailBuilder.buildDetailQuery(testDto, "logs_daily_20240101", "log_time");

        // 验证完整SQL结构
        assertTrue(sql.contains("SELECT log_time, level, message_text, service, host"), "字段选择错误");
        assertTrue(sql.contains("FROM logs_daily_20240101"), "表名错误");
        assertTrue(
                sql.contains(
                        "WHERE log_time >= '2024-01-01 00:00:00.000' AND log_time < '2024-01-01"
                                + " 01:00:00.000'"),
                "时间条件错误");
        assertTrue(
                sql.contains("(message_text LIKE '%error%') AND (level MATCH_PHRASE 'ERROR')"),
                "关键字条件错误");
        assertTrue(sql.contains("(service = 'user-service' AND host LIKE 'prod-%')"), "WHERE条件错误");
        assertTrue(sql.contains("ORDER BY log_time DESC"), "排序错误");
        assertTrue(sql.contains("LIMIT 100 OFFSET 0"), "分页错误");
    }

    @Test
    @DisplayName("时间分布SQL生成测试 - 验证分组和聚合")
    void testTimeDistributionSqlGeneration() {
        // 继续使用mock方式
        when(keywordConditionBuilder.buildKeywordConditions(any(LogSearchDTO.class)))
                .thenReturn("(level MATCH_PHRASE 'ERROR')");

        WhereConditionBuilder realWhereBuilder = new WhereConditionBuilder();
        DistributionSqlBuilder realDistBuilder =
                new DistributionSqlBuilder(keywordConditionBuilder, realWhereBuilder);

        // 设置搜索条件
        testDto.setWhereSqls(List.of("service IN ('user', 'order')"));

        // 生成5分钟间隔的时间分布SQL
        String sql =
                realDistBuilder.buildCustomIntervalDistribution(
                        testDto, "logs_daily_20240101", "log_time", "minute", 5);

        // 验证SQL结构 - 基于真实的SqlFragment.customTimeBucket实现
        assertTrue(
                sql.contains(
                        "SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / 300) * 300) AS"
                                + " log_time_"),
                "时间分组错误");
        assertTrue(sql.contains("COUNT(1) AS count"), "计数聚合错误");
        assertTrue(sql.contains("FROM logs_daily_20240101"), "表名错误");
        assertTrue(sql.contains("(level MATCH_PHRASE 'ERROR')"), "关键字条件错误");
        assertTrue(sql.contains("service IN ('user', 'order')"), "WHERE条件错误");
        assertTrue(
                sql.contains("GROUP BY FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / 300) * 300)"),
                "分组错误");
        assertTrue(sql.contains("ORDER BY log_time_ ASC"), "排序错误");
    }

    @Test
    @DisplayName("字段分布SQL生成测试 - 验证TOPN函数和采样查询")
    void testFieldDistributionSqlGeneration() {
        // KeywordConditionBuilder需要验证服务，所以继续用mock的方式
        when(keywordConditionBuilder.buildKeywordConditions(any(LogSearchDTO.class)))
                .thenReturn("(message_text LIKE '%timeout%')");

        // WhereConditionBuilder可以用真实实例
        WhereConditionBuilder realWhereBuilder = new WhereConditionBuilder();
        FieldDistributionSqlBuilder realFieldBuilder =
                new FieldDistributionSqlBuilder(keywordConditionBuilder, realWhereBuilder);

        // 设置复杂条件
        testDto.setKeywordConditions(
                List.of(createKeywordCondition("message_text", "timeout", "LIKE")));
        testDto.setWhereSqls(Arrays.asList("response_time > 1000", "status_code >= 400"));

        // 生成字段分布SQL
        String sql =
                realFieldBuilder.buildFieldDistribution(
                        testDto,
                        "logs_daily_20240101",
                        "log_time",
                        Arrays.asList("service", "host"),
                        Arrays.asList("service", "host"),
                        10);

        // 验证外层SQL结构 - 使用TOPN函数
        assertTrue(sql.contains("SELECT TOPN(service, 10) AS 'service'"), "service字段TOPN错误");
        assertTrue(sql.contains("TOPN(host, 10) AS 'host'"), "host字段TOPN错误");
        assertTrue(sql.contains("FROM (") && sql.contains(") AS sub_query"), "子查询结构错误");

        // 验证内层SQL包含条件和采样
        assertTrue(sql.contains("message_text LIKE '%timeout%'"), "关键字条件错误");
        assertTrue(sql.contains("(response_time > 1000 AND status_code >= 400)"), "WHERE条件错误");
        assertTrue(sql.contains("ORDER BY log_time DESC"), "内层排序错误");
        assertTrue(sql.contains("LIMIT 5000 OFFSET 0"), "采样限制错误");
    }

    /** 创建关键字条件DTO的辅助方法 */
    private KeywordConditionDTO createKeywordCondition(
            String fieldName, String value, String searchMethod) {
        KeywordConditionDTO condition = new KeywordConditionDTO();
        condition.setFieldName(fieldName);
        condition.setSearchValue(value);
        condition.setSearchMethod(searchMethod);
        return condition;
    }
}
