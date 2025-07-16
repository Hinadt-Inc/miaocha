package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.export.FileExporter;
import com.hinadt.miaocha.application.service.export.FileExporterFactory;
import com.hinadt.miaocha.application.service.impl.QueryPermissionChecker;
import com.hinadt.miaocha.application.service.impl.SqlQueryServiceImpl;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.SchemaConverter;
import com.hinadt.miaocha.domain.dto.DatabaseTableListDTO;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import com.hinadt.miaocha.domain.dto.TableSchemaDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.SqlQueryHistory;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.SqlQueryHistoryMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * SQL查询服务测试
 *
 * <p>测试秒查系统的SQL查询引擎，包括查询执行、结果导出、权限控制等功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SQL查询服务测试")
public class SqlQueryServiceTest {

    @Mock private DatasourceMapper datasourceMapper;

    @Mock private UserMapper userMapper;

    @Mock private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    @Mock private JdbcQueryExecutor jdbcQueryExecutor;

    @Mock private FileExporterFactory exporterFactory;

    @Mock private FileExporter fileExporter;

    @Mock private QueryPermissionChecker permissionChecker;

    @Mock private TableValidationService tableValidationService;

    @Mock private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Mock private DatabaseMetadataService metadataService;

    @Mock private SchemaConverter schemaConverter;

    @Mock private Connection connection;

    @InjectMocks private SqlQueryServiceImpl sqlQueryService;

    private User testUser;
    private DatasourceInfo testDatasourceInfo;
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
        testUser.setRole(UserRole.SUPER_ADMIN.name());
        testUser.setEmail("admin@hinadt.com");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateTime(LocalDateTime.now());

        // 创建测试数据源
        testDatasourceInfo = new DatasourceInfo();
        testDatasourceInfo.setId(1L);
        testDatasourceInfo.setName("测试数据源");
        testDatasourceInfo.setType("MYSQL");
        testDatasourceInfo.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");
        testDatasourceInfo.setUsername("test");
        testDatasourceInfo.setPassword("test");

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
        lenient()
                .doNothing()
                .when(permissionChecker)
                .checkQueryPermission(any(User.class), anyString());
        lenient().when(datasourceMapper.selectById(anyLong())).thenReturn(testDatasourceInfo);
        lenient().when(userMapper.selectById(anyLong())).thenReturn(testUser);
        lenient()
                .when(jdbcQueryExecutor.executeQuery(any(DatasourceInfo.class), anyString()))
                .thenReturn(testResultDTO);
        lenient().when(sqlQueryHistoryMapper.insert(any())).thenReturn(1);

        // 设置TableValidationService的默认行为
        lenient()
                .when(tableValidationService.processSqlWithLimit(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 默认返回原SQL

        // 设置DatabaseMetadataServiceFactory的默认行为
        lenient().when(metadataServiceFactory.getService(anyString())).thenReturn(metadataService);

        // 设置JdbcQueryExecutor的默认行为
        try {
            lenient()
                    .when(jdbcQueryExecutor.getConnection(any(DatasourceInfo.class)))
                    .thenReturn(connection);
        } catch (SQLException e) {
            // 这不应该发生，因为这是mock设置
            throw new RuntimeException("Mock setup failed", e);
        }
    }

    @Test
    void testExecuteQuery_Success() {

        // 执行测试
        SqlQueryResultDTO result = sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);

        // 验证调用
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(eq(testDatasourceInfo), eq(testQueryDTO.getSql()));
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getSql());
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
        assertNotNull(result.getDownloadUrl());
        assertTrue(result.getDownloadUrl().startsWith("/api/sql/result"));

        // 验证调用
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(eq(testDatasourceInfo), eq(testQueryDTO.getSql()));
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getSql());
    }

    @Test
    void testExecuteQuery_DatasourceNotFound() {
        // 重置并设置datasourceMapper返回null
        reset(datasourceMapper);
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
                        });

        assertEquals(ErrorCode.DATASOURCE_NOT_FOUND, exception.getErrorCode());
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper, never()).selectById(anyLong());
        verify(permissionChecker, never()).checkQueryPermission(any(), any());
    }

    @Test
    void testExecuteQuery_UserNotFound() {
        // 重置并设置userMapper返回null
        reset(userMapper);
        // 首先重置datasourceMapper来清除默认的行为
        reset(datasourceMapper);
        // 然后重新配置以确保先返回一个数据源，然后用户查询才会返回null
        when(datasourceMapper.selectById(anyLong())).thenReturn(testDatasourceInfo);
        when(userMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            sqlQueryService.executeQuery(999L, testQueryDTO);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        // 需要验证datasourceMapper被调用，因为在实现中先检查数据源再检查用户
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(999L);
        // 因为用户不存在，所以不会调用权限检查
        verify(permissionChecker, never()).checkQueryPermission(any(), any());
    }

    @Test
    void testExecuteQuery_InvalidSql() {
        // 准备数据
        testQueryDTO.setSql("");

        // 模拟权限检查器抛出异常
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空"))
                .when(permissionChecker)
                .checkQueryPermission(any(), eq(""));

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("SQL语句不能为空", exception.getMessage());
        verify(userMapper).selectById(testUser.getId());
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(permissionChecker).checkQueryPermission(testUser, "");
        verify(jdbcQueryExecutor, never()).executeQuery(any(DatasourceInfo.class), anyString());
    }

    @Test
    void testExecuteQuery_ExportFailed() throws IOException {
        // 准备数据
        testQueryDTO.setExportResult(true);
        testQueryDTO.setExportFormat("xlsx");
        when(exporterFactory.getExporter(anyString())).thenReturn(fileExporter);
        doThrow(new IOException("导出失败")).when(fileExporter).exportToFile(any(), anyString());

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);
                        });

        assertEquals(ErrorCode.EXPORT_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("导出失败"));
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(eq(testDatasourceInfo), eq(testQueryDTO.getSql()));
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(permissionChecker).checkQueryPermission(testUser, testQueryDTO.getSql());
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
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
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
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            sqlQueryService.getQueryResult(1L);
                        });

        assertEquals(ErrorCode.EXPORT_FAILED, exception.getErrorCode());
        assertEquals("查询结果文件不存在", exception.getMessage());
    }

    // ==================== 业务逻辑测试 ====================

    @Test
    @DisplayName("SQL查询使用TableValidationService处理LIMIT")
    void testExecuteQuery_UsesTableValidationServiceForLimit() {
        // 设置tableValidationService的行为
        String originalSql = "SELECT * FROM users";
        String processedSql = "SELECT * FROM users LIMIT 1000";
        when(tableValidationService.processSqlWithLimit(originalSql)).thenReturn(processedSql);

        testQueryDTO.setSql(originalSql);

        // 执行测试
        SqlQueryResultDTO result = sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);

        // 验证结果
        assertNotNull(result);

        // 验证TableValidationService被调用
        verify(tableValidationService).processSqlWithLimit(originalSql);

        // 验证SQL被正确处理
        assertEquals(processedSql, testQueryDTO.getSql());

        // 验证后续流程正常
        verify(jdbcQueryExecutor).executeQuery(eq(testDatasourceInfo), eq(processedSql));
    }

    // ==================== 数据库表列表获取测试 ====================

    @Test
    @DisplayName("获取数据库表列表 - 成功")
    void testGetDatabaseTableList_Success() throws SQLException {
        // 准备测试数据
        List<String> tableNames = Arrays.asList("users", "orders", "products");

        List<DatabaseTableListDTO.TableBasicInfoDTO> expectedTables = new ArrayList<>();
        for (String tableName : tableNames) {
            DatabaseTableListDTO.TableBasicInfoDTO table =
                    new DatabaseTableListDTO.TableBasicInfoDTO();
            table.setTableName(tableName);
            expectedTables.add(table);
        }

        DatabaseTableListDTO expectedResult = new DatabaseTableListDTO();
        expectedResult.setDatabaseName("test_db");
        expectedResult.setTables(expectedTables);

        // Mock 行为
        when(metadataService.getAllTables(connection)).thenReturn(tableNames);
        when(metadataService.getTableComment(connection, "users")).thenReturn("用户表");
        when(metadataService.getTableComment(connection, "orders")).thenReturn("订单表");
        when(metadataService.getTableComment(connection, "products")).thenReturn("产品表");
        when(schemaConverter.createTableBasicInfoList(tableNames)).thenReturn(expectedTables);

        // 执行测试
        DatabaseTableListDTO result =
                sqlQueryService.getDatabaseTableList(testUser.getId(), testDatasourceInfo.getId());

        // 验证结果
        assertNotNull(result);
        assertEquals("test_db", result.getDatabaseName());
        assertEquals(3, result.getTables().size());

        // 验证调用
        verify(datasourceMapper).selectById(testDatasourceInfo.getId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).getConnection(testDatasourceInfo);
        verify(metadataServiceFactory).getService(testDatasourceInfo.getType());
        verify(metadataService).getAllTables(connection);
        verify(schemaConverter).createTableBasicInfoList(eq(tableNames));
    }

    @Test
    @DisplayName("获取数据库表列表 - 数据源不存在")
    void testGetDatabaseTableList_DatasourceNotFound() {
        // Mock 行为
        when(datasourceMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> sqlQueryService.getDatabaseTableList(testUser.getId(), 999L));

        assertEquals(ErrorCode.DATASOURCE_NOT_FOUND, exception.getErrorCode());
        verify(datasourceMapper).selectById(999L);
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("获取数据库表列表 - 用户不存在")
    void testGetDatabaseTableList_UserNotFound() {
        // Mock 行为
        when(userMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                sqlQueryService.getDatabaseTableList(
                                        999L, testDatasourceInfo.getId()));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(datasourceMapper).selectById(testDatasourceInfo.getId());
        verify(userMapper).selectById(999L);
    }

    // ==================== 表字段信息获取测试 ====================

    @Test
    @DisplayName("获取表字段信息 - 成功")
    void testGetTableSchema_Success() throws SQLException {
        // 准备测试数据
        String tableName = "users";
        String tableComment = "用户表";
        List<String> permittedTables = Arrays.asList("users", "orders");

        List<SchemaInfoDTO.ColumnInfoDTO> columns = new ArrayList<>();
        SchemaInfoDTO.ColumnInfoDTO column1 = new SchemaInfoDTO.ColumnInfoDTO();
        column1.setColumnName("id");
        column1.setDataType("BIGINT");
        column1.setIsPrimaryKey(true);
        column1.setIsNullable(false);
        columns.add(column1);

        SchemaInfoDTO.ColumnInfoDTO column2 = new SchemaInfoDTO.ColumnInfoDTO();
        column2.setColumnName("name");
        column2.setDataType("VARCHAR");
        column2.setIsPrimaryKey(false);
        column2.setIsNullable(true);
        columns.add(column2);

        TableSchemaDTO expectedResult = new TableSchemaDTO();
        expectedResult.setDatabaseName("test_db");
        expectedResult.setTableName(tableName);
        expectedResult.setTableComment(tableComment);

        // Mock 行为
        when(metadataService.getAllTables(connection)).thenReturn(permittedTables);
        when(metadataService.getTableComment(connection, tableName)).thenReturn(tableComment);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);
        when(schemaConverter.createTableSchema("test_db", tableName, tableComment, columns))
                .thenReturn(expectedResult);

        // 执行测试
        TableSchemaDTO result =
                sqlQueryService.getTableSchema(
                        testUser.getId(), testDatasourceInfo.getId(), tableName);

        // 验证结果
        assertNotNull(result);
        assertEquals("test_db", result.getDatabaseName());
        assertEquals(tableName, result.getTableName());
        assertEquals(tableComment, result.getTableComment());

        // 验证调用
        verify(datasourceMapper).selectById(testDatasourceInfo.getId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).getConnection(testDatasourceInfo);
        verify(metadataServiceFactory).getService(testDatasourceInfo.getType());
        verify(metadataService).getAllTables(connection);
        verify(metadataService).getTableComment(connection, tableName);
        verify(metadataService).getColumnInfo(connection, tableName);
        verify(schemaConverter).createTableSchema("test_db", tableName, tableComment, columns);
    }

    @Test
    @DisplayName("获取表字段信息 - 无权限访问表")
    void testGetTableSchema_PermissionDenied() throws SQLException {
        // 准备测试数据
        String tableName = "restricted_table";
        List<String> permittedTables = Arrays.asList("users", "orders"); // 不包含 restricted_table

        // Mock 行为
        when(metadataService.getAllTables(connection)).thenReturn(permittedTables);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                sqlQueryService.getTableSchema(
                                        testUser.getId(), testDatasourceInfo.getId(), tableName));

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("无权限访问表"));
        verify(datasourceMapper).selectById(testDatasourceInfo.getId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).getConnection(testDatasourceInfo);
        verify(metadataServiceFactory).getService(testDatasourceInfo.getType());
        verify(metadataService).getAllTables(connection);
        verify(metadataService, never()).getTableComment(any(), any());
        verify(metadataService, never()).getColumnInfo(any(), any());
    }

    @Test
    @DisplayName("获取表字段信息 - 普通用户权限检查")
    void testGetTableSchema_NormalUserPermission() throws SQLException {
        // 准备普通用户
        User normalUser = new User();
        normalUser.setId(2L);
        normalUser.setRole(UserRole.USER.name());
        normalUser.setEmail("user@hinadt.com");

        String tableName = "users";
        List<String> permittedTables = Arrays.asList("users");

        // Mock 行为
        when(userMapper.selectById(2L)).thenReturn(normalUser);
        when(permissionChecker.getPermittedTables(2L, testDatasourceInfo.getId(), connection))
                .thenReturn(permittedTables);
        when(metadataService.getTableComment(connection, tableName)).thenReturn("用户表");
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(new ArrayList<>());
        when(schemaConverter.createTableSchema(anyString(), anyString(), anyString(), any()))
                .thenReturn(new TableSchemaDTO());

        // 执行测试
        TableSchemaDTO result =
                sqlQueryService.getTableSchema(2L, testDatasourceInfo.getId(), tableName);

        // 验证结果
        assertNotNull(result);

        // 验证调用了权限检查器而不是直接获取所有表
        verify(permissionChecker).getPermittedTables(2L, testDatasourceInfo.getId(), connection);
        verify(metadataService, never()).getAllTables(connection);
    }
}
