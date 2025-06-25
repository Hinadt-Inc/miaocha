package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.*;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * LogSqlBuilder门面类测试
 *
 * <p>验证门面类的委托逻辑，确保正确调用各专门的构建器
 */
@DisplayName("LogSqlBuilder门面类测试")
class LogSqlBuilderTest {

    @Mock private DistributionSqlBuilder distributionSqlBuilder;

    @Mock private DetailSqlBuilder detailSqlBuilder;

    @Mock private FieldDistributionSqlBuilder fieldDistributionSqlBuilder;

    @Mock private KeywordConditionBuilder keywordConditionBuilder;

    @Mock private QueryConfigValidationService queryConfigValidationService;

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
        testDto.setDatasourceId(1L);
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
        List<String> fields = Arrays.asList("level");
        List<String> originalFields = Arrays.asList("level");
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
}
