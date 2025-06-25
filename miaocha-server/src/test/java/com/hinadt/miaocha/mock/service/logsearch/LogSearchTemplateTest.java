package com.hinadt.miaocha.mock.service.logsearch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate;
import com.hinadt.miaocha.application.service.impl.logsearch.template.SearchContext;
import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.processor.TimeRangeProcessor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.KeywordSyntaxException;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LogSearchTemplate单元测试
 *
 * <p>重点测试模板方法模式的流程控制，验证执行顺序、异常处理和资源管理
 *
 * <p>关键理解：所有非LogQueryException的异常都会被转换为BusinessException(INTERNAL_ERROR)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogSearchTemplate模板流程测试")
class LogSearchTemplateTest {

    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private TimeRangeProcessor timeRangeProcessor;
    @Mock private ModuleInfoService moduleInfoService;
    @Mock private LogSearchDTOConverter dtoConverter;
    @Mock private QueryConfigValidationService queryConfigValidationService;
    @Mock private Connection connection;
    @Mock private LogSearchTemplate.SearchExecutor<LogDetailResultDTO> mockExecutor;

    private LogSearchTemplate logSearchTemplate;
    private LogSearchDTO testDto;
    private LogSearchDTO convertedDto;
    private DatasourceInfo testDatasource;

    @BeforeEach
    void setUp() {
        logSearchTemplate =
                new LogSearchTemplate(
                        jdbcQueryExecutor,
                        timeRangeProcessor,
                        moduleInfoService,
                        dtoConverter,
                        queryConfigValidationService);

        testDto = new LogSearchDTO();
        testDto.setModule("test-module");

        convertedDto = new LogSearchDTO();
        convertedDto.setModule("test-module");

        testDatasource = new DatasourceInfo();
        testDatasource.setId(1L);
        testDatasource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
    }

    @Test
    @DisplayName("正常执行流程 - 验证完整的模板方法执行顺序")
    void testExecute_NormalFlow_CallsAllStepsInOrder() throws Exception {
        // Arrange
        LogDetailResultDTO expectedResult = new LogDetailResultDTO();
        expectedResult.setTotalCount(100);

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(mockExecutor.execute(any(SearchContext.class))).thenReturn(expectedResult);

        // Act
        LogDetailResultDTO result =
                logSearchTemplate.execute(testDatasource, testDto, mockExecutor);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalCount());
        assertTrue(result.getExecutionTimeMs() >= 0);

        // 验证执行顺序和SearchContext参数
        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verify(queryConfigValidationService).getTimeField("test-module");
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verify(mockExecutor)
                .execute(
                        argThat(
                                context ->
                                        context.getConnection() == connection
                                                && context.getDto() == convertedDto
                                                && "test_table".equals(context.getTableName())
                                                && "timestamp".equals(context.getTimeField())));
    }

    @Test
    @DisplayName("数据库连接失败 - 抛出BusinessException(INTERNAL_ERROR)")
    void testExecute_DatabaseConnectionFails() throws SQLException {
        // Arrange
        SQLException originalException = new SQLException("连接数据库失败");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenThrow(originalException);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertEquals(ErrorCode.INTERNAL_ERROR, thrownException.getErrorCode());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verify(queryConfigValidationService).getTimeField("test-module");
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verifyNoInteractions(mockExecutor);
    }

    @Test
    @DisplayName("时间范围处理失败 - 异常自然传播")
    void testExecute_TimeRangeProcessingFails() {
        // Arrange
        RuntimeException originalException = new RuntimeException("时间范围无效");
        doThrow(originalException).when(timeRangeProcessor).processTimeRange(testDto);

        // Act & Assert - 异常应该自然传播，不被LogSearchTemplate捕获
        RuntimeException thrownException =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);
        assertEquals("时间范围无效", thrownException.getMessage());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verifyNoInteractions(
                dtoConverter,
                moduleInfoService,
                queryConfigValidationService,
                jdbcQueryExecutor,
                mockExecutor);
    }

    @Test
    @DisplayName("DTO转换失败 - 异常自然传播")
    void testExecute_DTOConversionFails() {
        // Arrange
        RuntimeException originalException = new RuntimeException("DTO转换失败");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenThrow(originalException);

        // Act & Assert - 异常应该自然传播
        RuntimeException thrownException =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);
        assertEquals("DTO转换失败", thrownException.getMessage());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verifyNoInteractions(
                moduleInfoService, queryConfigValidationService, jdbcQueryExecutor, mockExecutor);
    }

    @Test
    @DisplayName("模块表名获取失败 - 异常自然传播")
    void testExecute_ModuleTableNameFails() {
        // Arrange
        RuntimeException originalException = new RuntimeException("模块不存在");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenThrow(originalException);

        // Act & Assert - 异常应该自然传播
        RuntimeException thrownException =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);
        assertEquals("模块不存在", thrownException.getMessage());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verifyNoInteractions(queryConfigValidationService, jdbcQueryExecutor, mockExecutor);
    }

    @Test
    @DisplayName("时间字段获取失败 - 异常自然传播")
    void testExecute_TimeFieldConfigNotFound() {
        // Arrange
        RuntimeException originalException = new RuntimeException("配置未找到");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenThrow(originalException);

        // Act & Assert - 异常应该自然传播
        RuntimeException thrownException =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);
        assertEquals("配置未找到", thrownException.getMessage());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verify(queryConfigValidationService).getTimeField("test-module");
        verifyNoInteractions(jdbcQueryExecutor, mockExecutor);
    }

    @Test
    @DisplayName("执行器抛出LogQueryException - 异常自然传播")
    void testExecute_ExecutorThrowsLogQueryException() throws Exception {
        // Arrange
        LogQueryException originalException =
                new LogQueryException(ErrorCode.INTERNAL_ERROR, "DetailQuery", "查询执行失败");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(mockExecutor.execute(any(SearchContext.class))).thenThrow(originalException);

        // Act & Assert - LogQueryException应该自然传播
        LogQueryException thrownException =
                assertThrows(
                        LogQueryException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verify(queryConfigValidationService).getTimeField("test-module");
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verify(mockExecutor).execute(any(SearchContext.class));
    }

    @Test
    @DisplayName("执行器抛出KeywordSyntaxException - 异常自然传播")
    void testExecute_ExecutorThrowsKeywordSyntaxException() throws Exception {
        // Arrange
        KeywordSyntaxException originalException =
                new KeywordSyntaxException("关键词语法错误", "invalid_expression");

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(mockExecutor.execute(any(SearchContext.class))).thenThrow(originalException);

        // Act & Assert - KeywordSyntaxException应该自然传播，让GlobalExceptionHandler处理
        KeywordSyntaxException thrownException =
                assertThrows(
                        KeywordSyntaxException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertSame(originalException, thrownException);
        assertEquals("关键词语法错误", thrownException.getMessage());
        assertEquals("invalid_expression", thrownException.getExpression());
    }

    @Test
    @DisplayName("连接关闭失败 - 抛出BusinessException(INTERNAL_ERROR)")
    void testExecute_ConnectionCloseFails() throws Exception {
        // Arrange
        LogDetailResultDTO expectedResult = new LogDetailResultDTO();
        expectedResult.setTotalCount(50);

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(mockExecutor.execute(any(SearchContext.class))).thenReturn(expectedResult);
        doThrow(new SQLException("关闭连接失败")).when(connection).close();

        // Act & Assert - 连接关闭异常会被转换为BusinessException
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
                        });

        assertEquals(ErrorCode.INTERNAL_ERROR, thrownException.getErrorCode());

        verify(timeRangeProcessor).processTimeRange(testDto);
        verify(dtoConverter).convert(testDto);
        verify(moduleInfoService).getTableNameByModule("test-module");
        verify(queryConfigValidationService).getTimeField("test-module");
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verify(mockExecutor).execute(any(SearchContext.class));
    }

    @Test
    @DisplayName("执行时间计算 - 验证执行时间被正确设置")
    void testExecute_ExecutionTimeCalculation() throws Exception {
        // Arrange
        LogDetailResultDTO expectedResult = new LogDetailResultDTO();
        expectedResult.setTotalCount(25);

        doNothing().when(timeRangeProcessor).processTimeRange(testDto);
        when(dtoConverter.convert(testDto)).thenReturn(convertedDto);
        when(moduleInfoService.getTableNameByModule("test-module")).thenReturn("test_table");
        when(queryConfigValidationService.getTimeField("test-module")).thenReturn("timestamp");
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(mockExecutor.execute(any(SearchContext.class))).thenReturn(expectedResult);

        // Act
        long startTime = System.currentTimeMillis();
        LogDetailResultDTO result =
                logSearchTemplate.execute(testDatasource, testDto, mockExecutor);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(25, result.getTotalCount());
        assertTrue(result.getExecutionTimeMs() >= 0);
        assertTrue(result.getExecutionTimeMs() <= (endTime - startTime) + 100); // 允许100ms误差
    }
}
