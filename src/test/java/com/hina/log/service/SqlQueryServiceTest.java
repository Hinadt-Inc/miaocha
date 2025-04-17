package com.hina.log.service;

import com.hina.log.dto.SqlQueryDTO;
import com.hina.log.dto.SqlQueryResultDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.SqlQueryHistory;
import com.hina.log.entity.User;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.SqlQueryHistoryMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.database.DatabaseMetadataService;
import com.hina.log.service.database.DatabaseMetadataServiceFactory;
import com.hina.log.service.export.FileExporter;
import com.hina.log.service.export.FileExporterFactory;
import com.hina.log.service.impl.JdbcQueryExecutor;
import com.hina.log.service.impl.QueryPermissionChecker;
import com.hina.log.service.impl.SqlQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SqlQueryServiceTest {

    @Mock
    private DatasourceMapper datasourceMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    @Mock
    private JdbcQueryExecutor jdbcQueryExecutor;

    @Mock
    private FileExporterFactory exporterFactory;

    @Mock
    private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Mock
    private FileExporter fileExporter;

    @Mock
    private DatabaseMetadataService metadataService;

    @Mock
    private QueryPermissionChecker permissionChecker;

    @InjectMocks
    private SqlQueryServiceImpl sqlQueryService;

    private User testUser;
    private Datasource testDatasource;
    private SqlQueryDTO testQueryDTO;
    private SqlQueryResultDTO testResultDTO;
    private String testExportDir;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时导出目录
        testExportDir = Files.createTempDirectory("test-sql-exports").toString();
        ReflectionTestUtils.setField(sqlQueryService, "exportDir", testExportDir);

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUid("test_user");
        testUser.setIsAdmin(false);

        // 创建测试数据源
        testDatasource = new Datasource();
        testDatasource.setId(1L);
        testDatasource.setName("测试数据源");
        testDatasource.setType("MYSQL");
        testDatasource.setIp("localhost");
        testDatasource.setPort(3306);
        testDatasource.setUsername("test");
        testDatasource.setPassword("test");
        testDatasource.setDatabase("test_db");

        // 创建测试查询DTO
        testQueryDTO = new SqlQueryDTO();
        testQueryDTO.setDatasourceId(1L);
        testQueryDTO.setSql("SELECT * FROM test_table LIMIT 10");
        testQueryDTO.setExportResult(false);

        // 创建测试结果DTO
        testResultDTO = new SqlQueryResultDTO();
        testResultDTO.setColumns(List.of("id", "name"));
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", 1);
        row.put("name", "test");
        rows.add(row);
        testResultDTO.setRows(rows);
        testResultDTO.setExecutionTimeMs(100L);

        // 设置默认行为
        lenient().doNothing().when(permissionChecker).checkQueryPermission(any(User.class), anyLong(), anyString());
        lenient().when(datasourceMapper.selectById(anyLong())).thenReturn(testDatasource);
        lenient().when(userMapper.selectById(anyLong())).thenReturn(testUser);
        lenient().when(jdbcQueryExecutor.executeQuery(any(), anyString())).thenReturn(testResultDTO);
        lenient().when(sqlQueryHistoryMapper.insert(any())).thenReturn(1);
    }

    @Test
    void testExecuteQuery_Success() {
        // 执行测试
        SqlQueryResultDTO result = sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(testResultDTO.getColumns(), result.getColumns());
        assertEquals(testResultDTO.getRows(), result.getRows());
        assertEquals(testResultDTO.getExecutionTimeMs(), result.getExecutionTimeMs());

        // 验证调用
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(testDatasource, testQueryDTO.getSql());
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
    }

    @Test
    void testExecuteQuery_WithExport() throws IOException {
        // 准备数据
        testQueryDTO.setExportResult(true);
        testQueryDTO.setExportFormat("xlsx");
        when(exporterFactory.getExporter(anyString())).thenReturn(fileExporter);

        // 执行测试
        SqlQueryResultDTO result = sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getExcelDownloadUrl());
        assertTrue(result.getExcelDownloadUrl().startsWith("/api/sql/excel/"));

        // 验证调用
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(testDatasource, testQueryDTO.getSql());
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
    }

    @Test
    void testExecuteQuery_DatasourceNotFound() {
        // 重置并设置datasourceMapper返回null
        reset(datasourceMapper);
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
        });

        assertEquals(ErrorCode.DATASOURCE_NOT_FOUND, exception.getErrorCode());
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper, never()).selectById(anyLong());
        verify(permissionChecker, never()).checkQueryPermission(any(), any(), any());
    }

    @Test
    void testExecuteQuery_UserNotFound() {
        // 重置并设置userMapper返回null
        reset(userMapper);
        // 首先重置datasourceMapper来清除默认的行为
        reset(datasourceMapper);
        // 然后重新配置以确保先返回一个数据源，然后用户查询才会返回null
        when(datasourceMapper.selectById(anyLong())).thenReturn(testDatasource);
        when(userMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.executeQuery(999L, testQueryDTO);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        // 需要验证datasourceMapper被调用，因为在实现中先检查数据源再检查用户
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(999L);
        // 因为用户不存在，所以不会调用权限检查
        verify(permissionChecker, never()).checkQueryPermission(any(), any(), any());
    }

    @Test
    void testExecuteQuery_InvalidSql() {
        // 准备数据
        testQueryDTO.setSql("");

        // 模拟权限检查器抛出异常
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空"))
                .when(permissionChecker).checkQueryPermission(any(), anyLong(), eq(""));

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("SQL语句不能为空", exception.getMessage());
        verify(userMapper).selectById(testUser.getId());
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getDatasourceId(), "");
        verify(jdbcQueryExecutor, never()).executeQuery(any(), anyString());
    }

    @Test
    void testExecuteQuery_ExportFailed() throws IOException {
        // 准备数据
        testQueryDTO.setExportResult(true);
        testQueryDTO.setExportFormat("xlsx");
        when(exporterFactory.getExporter(anyString())).thenReturn(fileExporter);
        doThrow(new IOException("导出失败")).when(fileExporter).exportToFile(any(), anyString());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
        });

        assertEquals(ErrorCode.EXPORT_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("导出失败"));
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(testDatasource, testQueryDTO.getSql());
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
    }

    @Test
    void testGetQueryResult_Success() throws IOException {
        // 准备数据
        String testFilePath = testExportDir + "/test_result.xlsx";
        Files.createFile(Path.of(testFilePath));

        SqlQueryHistory history = new SqlQueryHistory();
        history.setId(1L);
        history.setResultFilePath(testFilePath);

        when(sqlQueryHistoryMapper.selectById(anyLong())).thenReturn(history);

        // 执行测试
        Resource resource = sqlQueryService.getQueryResult(1L);

        // 验证结果
        assertNotNull(resource);
        assertTrue(resource instanceof FileSystemResource);
        verify(sqlQueryHistoryMapper).selectById(1L);

        // 清理
        Files.deleteIfExists(Path.of(testFilePath));
    }

    @Test
    void testGetQueryResult_HistoryNotFound() {
        // 准备数据
        reset(sqlQueryHistoryMapper);
        when(sqlQueryHistoryMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.getQueryResult(999L);
        });

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        assertEquals("查询记录不存在", exception.getMessage());
    }

    @Test
    void testGetQueryResult_FileNotFound() {
        // 准备数据
        reset(sqlQueryHistoryMapper);

        SqlQueryHistory history = new SqlQueryHistory();
        history.setId(1L);
        history.setResultFilePath(null);

        when(sqlQueryHistoryMapper.selectById(anyLong())).thenReturn(history);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sqlQueryService.getQueryResult(1L);
        });

        assertEquals(ErrorCode.EXPORT_FAILED, exception.getErrorCode());
        assertEquals("查询结果文件不存在", exception.getMessage());
    }
}