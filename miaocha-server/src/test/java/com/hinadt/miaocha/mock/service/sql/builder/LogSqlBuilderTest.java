package com.hinadt.miaocha.mock.service.sql.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.builder.*;
import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.converter.VariantFieldConverter;
import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LogSqlBuilder门面类单元测试
 *
 * <p>测试门面类的协调逻辑和委托行为，使用真实的SQL构建器组件，只Mock外部系统交互
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogSqlBuilder门面类单元测试")
class LogSqlBuilderTest {

    // 只Mock外部系统交互
    @Mock private QueryConfigValidationService queryConfigValidationService;

    // 使用真实的SQL构建器组件
    private DistributionSqlBuilder distributionSqlBuilder;
    private DetailSqlBuilder detailSqlBuilder;
    private FieldDistributionSqlBuilder fieldDistributionSqlBuilder;
    private KeywordConditionBuilder keywordConditionBuilder;
    private WhereConditionBuilder whereConditionBuilder;

    private LogSqlBuilder logSqlBuilder;
    private LogSearchDTOConverter dtoConverter;

    @BeforeEach
    void setUp() {
        // 创建真实的SQL构建器组件
        keywordConditionBuilder = new KeywordConditionBuilder();
        // 手动注入QueryConfigValidationService到KeywordConditionBuilder
        try {
            java.lang.reflect.Field field =
                    KeywordConditionBuilder.class.getDeclaredField("queryConfigValidationService");
            field.setAccessible(true);
            field.set(keywordConditionBuilder, queryConfigValidationService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject QueryConfigValidationService", e);
        }

        whereConditionBuilder = new WhereConditionBuilder();
        distributionSqlBuilder =
                new DistributionSqlBuilder(keywordConditionBuilder, whereConditionBuilder);
        detailSqlBuilder = new DetailSqlBuilder(keywordConditionBuilder, whereConditionBuilder);
        fieldDistributionSqlBuilder =
                new FieldDistributionSqlBuilder(keywordConditionBuilder, whereConditionBuilder);

        // 创建门面类
        logSqlBuilder =
                new LogSqlBuilder(
                        distributionSqlBuilder,
                        detailSqlBuilder,
                        fieldDistributionSqlBuilder,
                        keywordConditionBuilder,
                        queryConfigValidationService);

        // 创建DTO转换器用于测试
        VariantFieldConverter variantFieldConverter = new VariantFieldConverter();
        dtoConverter = new LogSearchDTOConverter(variantFieldConverter);
    }

    @Nested
    @DisplayName("时间分布SQL构建测试")
    class DistributionSqlTests {

        @Test
        @DisplayName("自定义间隔时间分布SQL - 期望：正确委托并生成完整SQL")
        void testBuildDistributionSqlWithInterval() {
            // Arrange
            LogSearchDTO dto = createBasicDTO();
            String tableName = "test_logs";
            String timeUnit = "minute";
            int intervalValue = 5;
            String expectedTimeField = "log_time";

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenReturn(expectedTimeField);

            // Act
            String result =
                    logSqlBuilder.buildDistributionSqlWithInterval(
                            dto, tableName, timeUnit, intervalValue);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("SELECT"), "应包含SELECT子句");
            assertTrue(result.contains("FROM " + tableName), "应包含正确的表名");
            assertTrue(result.contains("GROUP BY"), "应包含GROUP BY子句");
            assertTrue(result.contains("ORDER BY"), "应包含ORDER BY子句");
            assertTrue(result.contains(expectedTimeField), "应包含时间字段");
            assertTrue(result.contains("COUNT(1)"), "应包含计数函数");

            // 验证调用了配置服务
            verify(queryConfigValidationService, times(1)).getTimeField("test-module");
        }

        @Test
        @DisplayName("配置服务异常处理 - 期望：使用默认时间字段")
        void testDistributionSqlWithConfigException() {
            // Arrange
            LogSearchDTO dto = createBasicDTO();
            String tableName = "test_logs";
            String timeUnit = "hour";
            int intervalValue = 1;

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenThrow(new RuntimeException("配置未找到"));

            // Act
            String result =
                    logSqlBuilder.buildDistributionSqlWithInterval(
                            dto, tableName, timeUnit, intervalValue);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("log_time"), "应使用默认时间字段log_time");
            verify(queryConfigValidationService, times(1)).getTimeField("test-module");
        }
    }

    @Nested
    @DisplayName("详情查询SQL构建测试")
    class DetailSqlTests {

        @Test
        @DisplayName("详情查询SQL - 期望：正确委托并生成完整SQL")
        void testBuildDetailQuery() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.logId", "level", "host"));
            original.setPageSize(50);
            original.setOffset(100);

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            String timeField = "log_time";

            // Act
            String result = logSqlBuilder.buildDetailQuery(dto, tableName, timeField);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("SELECT"), "应包含SELECT子句");
            assertTrue(result.contains("FROM " + tableName), "应包含正确的表名");
            assertTrue(result.contains("ORDER BY"), "应包含ORDER BY子句");
            assertTrue(result.contains("LIMIT 50"), "应包含正确的LIMIT");
            assertTrue(result.contains("OFFSET 100"), "应包含正确的OFFSET");
            assertTrue(result.contains("message['logId'] AS 'message.logId'"), "应包含转换后的字段");
            assertTrue(result.contains("level"), "应包含普通字段");
            assertTrue(result.contains("host"), "应包含普通字段");
        }

        @Test
        @DisplayName("总数查询SQL - 期望：正确委托并生成COUNT SQL")
        void testBuildCountQuery() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            String timeField = "log_time";

            // Act
            String result = logSqlBuilder.buildCountQuery(dto, tableName, timeField);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("SELECT COUNT(1)"), "应包含COUNT查询");
            assertTrue(result.contains("FROM " + tableName), "应包含正确的表名");
            assertFalse(result.contains("LIMIT"), "COUNT查询不应包含LIMIT");
            assertFalse(result.contains("ORDER BY"), "COUNT查询不应包含ORDER BY");
        }
    }

    @Nested
    @DisplayName("字段分布SQL构建测试")
    class FieldDistributionSqlTests {

        @Test
        @DisplayName("字段分布SQL - 期望：正确委托并生成TOPN SQL")
        void testBuildFieldDistributionSql() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.level", "host", "source"));

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            List<String> convertedFields = dto.getFields();
            List<String> originalFields = dto.getOriginalFields();
            int topN = 5;
            String expectedTimeField = "log_time";

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenReturn(expectedTimeField);

            // Act
            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("SELECT"), "应包含SELECT子句");
            assertTrue(result.contains("TOPN"), "应包含TOPN函数");
            assertTrue(result.contains("FROM"), "应包含FROM子句");
            assertTrue(result.contains("sub_query"), "应包含子查询");
            assertTrue(result.contains("LIMIT 5000"), "应包含采样LIMIT");

            // 验证TOPN函数格式 - 应该是纯字段名，不包含AS别名
            assertTrue(
                    result.contains("TOPN(message['level'], 5) AS 'message.level'"),
                    "应包含转换后字段的TOPN函数");
            assertTrue(result.contains("TOPN(host, 5) AS 'host'"), "应包含普通字段的TOPN函数");

            verify(queryConfigValidationService, times(1)).getTimeField("test-module");
        }

        @Test
        @DisplayName("字段分布SQL配置异常处理 - 期望：使用默认时间字段")
        void testFieldDistributionSqlWithConfigException() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("level"));

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            List<String> convertedFields = dto.getFields();
            List<String> originalFields = dto.getOriginalFields();
            int topN = 10;

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenThrow(new RuntimeException("配置未找到"));

            // Act
            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("log_time"), "应使用默认时间字段log_time");
            verify(queryConfigValidationService, times(1)).getTimeField("test-module");
        }
    }

    @Nested
    @DisplayName("复杂场景SQL构建测试")
    class ComplexScenarioTests {

        @Test
        @DisplayName("包含关键字条件的详情查询 - 期望：正确处理关键字搜索条件")
        void testDetailQueryWithKeywordConditions() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("message.content", "level"));

            KeywordConditionDTO keywordCondition = new KeywordConditionDTO();
            keywordCondition.setFieldName("message.content");
            keywordCondition.setSearchValue("error");
            keywordCondition.setSearchMethod("like");
            original.setKeywordConditions(Arrays.asList(keywordCondition));

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            String timeField = "log_time";

            // Mock配置服务调用
            doNothing()
                    .when(queryConfigValidationService)
                    .validateKeywordFieldPermissions(any(), any());
            when(queryConfigValidationService.getFieldSearchMethodMap("test-module"))
                    .thenReturn(java.util.Collections.emptyMap());

            // Act
            String result = logSqlBuilder.buildDetailQuery(dto, tableName, timeField);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("WHERE"), "应包含WHERE子句");
            assertTrue(result.contains("message['content']"), "应包含转换后的字段名");
            assertTrue(result.contains("LIKE"), "关键字搜索应使用LIKE操作符");
            assertTrue(result.contains("%error%"), "应包含关键字搜索值");
        }

        @Test
        @DisplayName("包含WHERE条件的字段分布查询 - 期望：正确处理自定义WHERE条件")
        void testFieldDistributionWithWhereConditions() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setFields(Arrays.asList("level"));
            original.setWhereSqls(Arrays.asList("message.level = 'ERROR'", "host = 'server1'"));

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            List<String> convertedFields = dto.getFields();
            List<String> originalFields = dto.getOriginalFields();
            int topN = 5;
            String expectedTimeField = "log_time";

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenReturn(expectedTimeField);

            // Act
            String result =
                    logSqlBuilder.buildFieldDistributionSql(
                            dto, tableName, convertedFields, originalFields, topN);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("WHERE"), "应包含WHERE子句");
            assertTrue(result.contains("message['level'] = 'ERROR'"), "应包含转换后的WHERE条件");
            assertTrue(result.contains("host = 'server1'"), "应包含普通WHERE条件");
        }

        @Test
        @DisplayName("复杂嵌套字段的时间分布查询 - 期望：正确处理多层嵌套字段")
        void testDistributionWithComplexFields() {
            // Arrange
            LogSearchDTO original = createBasicDTO();
            original.setWhereSqls(
                    Arrays.asList(
                            "request.headers.authorization IS NOT NULL",
                            "response.data.user.id = '12345'"));

            LogSearchDTODecorator dto = (LogSearchDTODecorator) dtoConverter.convert(original);
            String tableName = "test_logs";
            String timeUnit = "hour";
            int intervalValue = 2;
            String expectedTimeField = "log_time";

            when(queryConfigValidationService.getTimeField("test-module"))
                    .thenReturn(expectedTimeField);

            // Act
            String result =
                    logSqlBuilder.buildDistributionSqlWithInterval(
                            dto, tableName, timeUnit, intervalValue);

            // Assert
            assertNotNull(result, "SQL不应为null");
            assertTrue(result.contains("request['headers']['authorization']"), "应包含转换后的嵌套字段");
            assertTrue(result.contains("response['data']['user']['id']"), "应包含转换后的嵌套字段");
        }
    }

    private LogSearchDTO createBasicDTO() {
        LogSearchDTO dto = new LogSearchDTO();
        dto.setModule("test-module");
        dto.setStartTime("2024-01-01 00:00:00.000");
        dto.setEndTime("2024-01-01 01:00:00.000");
        dto.setPageSize(20);
        dto.setOffset(0);
        return dto;
    }
}
