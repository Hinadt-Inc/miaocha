package com.hinadt.miaocha.mock.service.logsearch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.impl.LogSearchServiceImpl;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.DetailSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.FieldDistributionSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.executor.HistogramSearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate;
import com.hinadt.miaocha.application.service.impl.logsearch.validator.LogSearchValidator;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogHistogramResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LogSearchServiceImpl单元测试
 *
 * <p>重点测试业务编排逻辑，验证参数验证、依赖调用和异常处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogSearchServiceImpl业务编排测试")
class LogSearchServiceImplTest {

    @Mock private LogSearchValidator validator;
    @Mock private LogSearchTemplate searchTemplate;
    @Mock private DetailSearchExecutor detailExecutor;
    @Mock private HistogramSearchExecutor histogramExecutor;
    @Mock private FieldDistributionSearchExecutor fieldDistributionExecutor;
    @Mock private DatabaseMetadataServiceFactory metadataServiceFactory;
    @Mock private ModuleInfoService moduleInfoService;
    @Mock private JdbcQueryExecutor jdbcQueryExecutor;
    @Mock private DatabaseMetadataService metadataService;
    @Mock private Connection connection;

    private LogSearchServiceImpl logSearchService;

    private LogSearchDTO testDto;
    private DatasourceInfo testDatasource;

    @BeforeEach
    void setUp() {
        // 手动创建LogSearchServiceImpl实例，确保所有依赖正确注入
        logSearchService =
                new LogSearchServiceImpl(
                        validator,
                        searchTemplate,
                        detailExecutor,
                        histogramExecutor,
                        fieldDistributionExecutor,
                        metadataServiceFactory,
                        moduleInfoService,
                        jdbcQueryExecutor);

        testDto = new LogSearchDTO();
        testDto.setModule("test-module");
        testDto.setOffset(0);
        testDto.setPageSize(20);

        testDatasource = new DatasourceInfo();
        testDatasource.setId(1L);
        testDatasource.setType("doris");
        testDatasource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
    }

    // ==================== searchDetails 测试 ====================

    @Test
    @DisplayName("明细查询成功 - 验证完整的业务流程")
    void testSearchDetails_Success() {
        // Arrange
        LogDetailResultDTO expectedResult = new LogDetailResultDTO();
        expectedResult.setTotalCount(100);

        doNothing().when(validator).validatePaginationParams(testDto);
        doNothing().when(validator).validateSortFields(testDto);
        when(validator.validateAndGetDatasource("test-module")).thenReturn(testDatasource);
        when(searchTemplate.execute(eq(testDatasource), eq(testDto), eq(detailExecutor)))
                .thenReturn(expectedResult);

        // Act
        LogDetailResultDTO result = logSearchService.searchDetails(testDto);

        // Assert
        assertEquals(expectedResult, result);

        // 验证调用顺序和参数
        verify(validator).validatePaginationParams(testDto);
        verify(validator).validateSortFields(testDto);
        verify(validator).validateAndGetDatasource("test-module");
        verify(searchTemplate).execute(testDatasource, testDto, detailExecutor);
    }

    // 删除用户验证测试 - 已移除userId参数

    @Test
    @DisplayName("明细查询 - 分页参数验证失败")
    void testSearchDetails_PaginationValidationFails() {
        // Arrange
        BusinessException expectedException =
                new BusinessException(ErrorCode.VALIDATION_ERROR, "分页参数无效");

        doThrow(expectedException).when(validator).validatePaginationParams(testDto);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class, () -> logSearchService.searchDetails(testDto));

        assertEquals(expectedException, thrownException);

        verify(validator).validatePaginationParams(testDto);
        verify(validator, never()).validateSortFields(any());
        verify(validator, never()).validateAndGetDatasource(anyString());
        verifyNoInteractions(searchTemplate);
    }

    @Test
    @DisplayName("明细查询 - 数据源验证失败")
    void testSearchDetails_DatasourceValidationFails() {
        // Arrange
        BusinessException expectedException =
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "数据源不存在");

        doNothing().when(validator).validatePaginationParams(testDto);
        doNothing().when(validator).validateSortFields(testDto);
        when(validator.validateAndGetDatasource("test-module")).thenThrow(expectedException);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class, () -> logSearchService.searchDetails(testDto));

        assertEquals(expectedException, thrownException);

        verify(validator).validatePaginationParams(testDto);
        verify(validator).validateSortFields(testDto);
        verify(validator).validateAndGetDatasource("test-module");
        verifyNoInteractions(searchTemplate);
    }

    @Test
    @DisplayName("明细查询 - 搜索模板执行失败")
    void testSearchDetails_SearchTemplateFails() {
        // Arrange
        LogQueryException expectedException =
                new LogQueryException(ErrorCode.INTERNAL_ERROR, "DetailQuery", "查询执行失败");

        doNothing().when(validator).validatePaginationParams(testDto);
        doNothing().when(validator).validateSortFields(testDto);
        when(validator.validateAndGetDatasource("test-module")).thenReturn(testDatasource);
        when(searchTemplate.execute(testDatasource, testDto, detailExecutor))
                .thenThrow(expectedException);

        // Act & Assert
        LogQueryException thrownException =
                assertThrows(
                        LogQueryException.class, () -> logSearchService.searchDetails(testDto));

        assertEquals(expectedException, thrownException);

        // 验证所有验证步骤都执行了
        verify(validator).validatePaginationParams(testDto);
        verify(validator).validateSortFields(testDto);
        verify(validator).validateAndGetDatasource("test-module");
        verify(searchTemplate).execute(testDatasource, testDto, detailExecutor);
    }

    @Test
    @DisplayName("明细查询 - 排序字段验证失败")
    void testSearchDetails_SortFieldsValidationFails() {
        // Arrange
        BusinessException expectedException =
                new BusinessException(ErrorCode.VALIDATION_ERROR, "排序字段不能重复，重复字段: level");

        doNothing().when(validator).validatePaginationParams(testDto);
        doThrow(expectedException).when(validator).validateSortFields(testDto);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class, () -> logSearchService.searchDetails(testDto));

        assertEquals(expectedException, thrownException);

        verify(validator).validatePaginationParams(testDto);
        verify(validator).validateSortFields(testDto);
        verify(validator, never()).validateAndGetDatasource(anyString());
        verifyNoInteractions(searchTemplate);
    }

    // ==================== searchHistogram 测试 ====================

    @Test
    @DisplayName("柱状图查询成功 - 验证业务流程")
    void testSearchHistogram_Success() {
        // Arrange
        LogHistogramResultDTO expectedResult = new LogHistogramResultDTO();

        when(validator.validateAndGetDatasource("test-module")).thenReturn(testDatasource);
        when(searchTemplate.execute(eq(testDatasource), eq(testDto), eq(histogramExecutor)))
                .thenReturn(expectedResult);

        // Act
        LogHistogramResultDTO result = logSearchService.searchHistogram(testDto);

        // Assert
        assertEquals(expectedResult, result);

        // 验证调用顺序（注意柱状图查询不需要分页验证）

        verify(validator).validateAndGetDatasource("test-module");
        verify(validator, never()).validatePaginationParams(any()); // 不应该调用分页验证
        verify(searchTemplate).execute(testDatasource, testDto, histogramExecutor);
    }

    // 删除用户验证测试 - 已移除userId参数

    // ==================== searchFieldDistributions 测试 ====================

    @Test
    @DisplayName("字段分布查询成功 - 验证字段验证逻辑")
    void testSearchFieldDistributions_Success() {
        // Arrange
        LogFieldDistributionResultDTO expectedResult = new LogFieldDistributionResultDTO();

        doNothing().when(validator).validateFields(testDto);
        when(validator.validateAndGetDatasource("test-module")).thenReturn(testDatasource);
        when(searchTemplate.execute(eq(testDatasource), eq(testDto), eq(fieldDistributionExecutor)))
                .thenReturn(expectedResult);

        // Act
        LogFieldDistributionResultDTO result = logSearchService.searchFieldDistributions(testDto);

        // Assert
        assertEquals(expectedResult, result);

        // 验证调用顺序（包含字段验证）

        verify(validator).validateFields(testDto);
        verify(validator).validateAndGetDatasource("test-module");
        verify(searchTemplate).execute(testDatasource, testDto, fieldDistributionExecutor);
    }

    @Test
    @DisplayName("字段分布查询 - 字段验证失败")
    void testSearchFieldDistributions_FieldValidationFails() {
        // Arrange
        BusinessException expectedException =
                new BusinessException(ErrorCode.VALIDATION_ERROR, "查询字段为空");

        doThrow(expectedException).when(validator).validateFields(testDto);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class,
                        () -> logSearchService.searchFieldDistributions(testDto));

        assertEquals(expectedException, thrownException);

        verify(validator).validateFields(testDto);
        verify(validator, never()).validateAndGetDatasource(anyString());
        verifyNoInteractions(searchTemplate);
    }

    // ==================== getTableColumns 测试 ====================

    @Test
    @DisplayName("获取表字段信息成功 - 验证完整的数据库元数据查询流程")
    void testGetTableColumns_Success() throws SQLException {
        // Arrange
        String module = "test-module";
        String tableName = "test_table";

        SchemaInfoDTO.ColumnInfoDTO column1 = new SchemaInfoDTO.ColumnInfoDTO();
        column1.setColumnName("id");
        column1.setDataType("BIGINT");
        column1.setIsPrimaryKey(true);
        column1.setIsNullable(false);

        SchemaInfoDTO.ColumnInfoDTO column2 = new SchemaInfoDTO.ColumnInfoDTO();
        column2.setColumnName("message");
        column2.setDataType("TEXT");
        column2.setIsPrimaryKey(false);
        column2.setIsNullable(true);

        SchemaInfoDTO.ColumnInfoDTO column3 = new SchemaInfoDTO.ColumnInfoDTO();
        column3.setColumnName("timestamp");
        column3.setDataType("DATETIME");
        column3.setIsPrimaryKey(false);
        column3.setIsNullable(false);

        List<SchemaInfoDTO.ColumnInfoDTO> expectedColumns =
                Arrays.asList(column1, column2, column3);

        when(validator.validateAndGetDatasource(module)).thenReturn(testDatasource);
        when(moduleInfoService.getTableNameByModule(module)).thenReturn(tableName);
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(metadataServiceFactory.getService("doris")).thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(expectedColumns);

        // Act
        List<SchemaInfoDTO.ColumnInfoDTO> result = logSearchService.getTableColumns(module);

        // Assert
        assertEquals(expectedColumns, result);

        // 验证调用顺序
        verify(validator).validateAndGetDatasource(module);
        verify(moduleInfoService).getTableNameByModule(module);
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verify(metadataServiceFactory).getService("doris");
        verify(metadataService).getColumnInfo(connection, tableName);
        // 注意：连接现在由HikariCP管理，不需要手动关闭
    }

    @Test
    @DisplayName("获取表字段信息 - 数据库连接失败")
    void testGetTableColumns_DatabaseConnectionFails() throws SQLException {
        // Arrange
        String module = "test-module";
        String tableName = "test_table";
        SQLException sqlException = new SQLException("连接失败");

        when(validator.validateAndGetDatasource(module)).thenReturn(testDatasource);
        when(moduleInfoService.getTableNameByModule(module)).thenReturn(tableName);
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenThrow(sqlException);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class, () -> logSearchService.getTableColumns(module));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrownException.getErrorCode());

        // 验证前置步骤正常执行
        verify(validator).validateAndGetDatasource(module);
        verify(moduleInfoService).getTableNameByModule(module);
        verify(jdbcQueryExecutor).getConnection(testDatasource);

        // 验证元数据服务没有被调用
        verifyNoInteractions(metadataServiceFactory, metadataService);
    }

    @Test
    @DisplayName("获取表字段信息 - 元数据查询失败")
    void testGetTableColumns_MetadataQueryFails() throws SQLException {
        // Arrange
        String module = "test-module";
        String tableName = "test_table";
        SQLException queryException = new SQLException("查询元数据失败");

        when(validator.validateAndGetDatasource(module)).thenReturn(testDatasource);
        when(moduleInfoService.getTableNameByModule(module)).thenReturn(tableName);
        when(jdbcQueryExecutor.getConnection(testDatasource)).thenReturn(connection);
        when(metadataServiceFactory.getService("doris")).thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenThrow(queryException);

        // Act & Assert
        BusinessException thrownException =
                assertThrows(
                        BusinessException.class, () -> logSearchService.getTableColumns(module));

        assertEquals(ErrorCode.INTERNAL_ERROR, thrownException.getErrorCode());

        // 验证所有步骤都执行了
        verify(validator).validateAndGetDatasource(module);
        verify(moduleInfoService).getTableNameByModule(module);
        verify(jdbcQueryExecutor).getConnection(testDatasource);
        verify(metadataServiceFactory).getService("doris");
        verify(metadataService).getColumnInfo(connection, tableName);

        // 注意：连接现在由HikariCP管理，即使查询失败也无需手动关闭
    }

    @Test
    @DisplayName("获取表字段信息 - 模块对应的表名获取失败")
    void testGetTableColumns_TableNameNotFound() {
        // Arrange
        String module = "invalid-module";
        RuntimeException moduleException = new RuntimeException("模块配置未找到");

        when(validator.validateAndGetDatasource(module)).thenReturn(testDatasource);
        when(moduleInfoService.getTableNameByModule(module)).thenThrow(moduleException);

        // Act & Assert
        RuntimeException thrownException =
                assertThrows(
                        RuntimeException.class, () -> logSearchService.getTableColumns(module));

        assertEquals(moduleException, thrownException);

        // 验证前置验证步骤执行了
        verify(validator).validateAndGetDatasource(module);
        verify(moduleInfoService).getTableNameByModule(module);

        // 验证没有尝试连接数据库
        verifyNoInteractions(jdbcQueryExecutor, metadataServiceFactory, metadataService);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("验证器调用参数正确性")
    void testValidatorCallsWithCorrectParameters() {
        // Arrange
        LogDetailResultDTO expectedResult = new LogDetailResultDTO();

        doNothing().when(validator).validatePaginationParams(testDto);
        when(validator.validateAndGetDatasource("test-module")).thenReturn(testDatasource);
        when(searchTemplate.execute(testDatasource, testDto, detailExecutor))
                .thenReturn(expectedResult);

        // Act
        logSearchService.searchDetails(testDto);

        // Assert - 验证传递给验证器的参数是正确的
        ArgumentCaptor<LogSearchDTO> dtoCaptor = ArgumentCaptor.forClass(LogSearchDTO.class);
        ArgumentCaptor<String> moduleCaptor = ArgumentCaptor.forClass(String.class);

        verify(validator).validatePaginationParams(dtoCaptor.capture());
        verify(validator).validateAndGetDatasource(moduleCaptor.capture());

        assertEquals(testDto, dtoCaptor.getValue());
        assertEquals("test-module", moduleCaptor.getValue());
    }
}
