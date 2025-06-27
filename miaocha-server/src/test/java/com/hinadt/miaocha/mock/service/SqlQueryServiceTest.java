package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.export.FileExporter;
import com.hinadt.miaocha.application.service.export.FileExporterFactory;
import com.hinadt.miaocha.application.service.impl.QueryPermissionChecker;
import com.hinadt.miaocha.application.service.impl.SqlQueryServiceImpl;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.SqlQueryDTO;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.SqlQueryHistory;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.SqlQueryHistoryMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import io.qameta.allure.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@Epic("秒查日志管理系统")
@Feature("SQL查询引擎")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SQL查询服务测试")
@Owner("开发团队")
public class SqlQueryServiceTest {

    @Mock private DatasourceMapper datasourceMapper;

    @Mock private UserMapper userMapper;

    @Mock private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    @Mock private JdbcQueryExecutor jdbcQueryExecutor;

    @Mock private FileExporterFactory exporterFactory;

    @Mock private FileExporter fileExporter;

    @Mock private QueryPermissionChecker permissionChecker;

    @Mock private TableValidationService tableValidationService;

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
                .checkQueryPermission(any(User.class), anyLong(), anyString());
        lenient().when(datasourceMapper.selectById(anyLong())).thenReturn(testDatasourceInfo);
        lenient().when(userMapper.selectById(anyLong())).thenReturn(testUser);
        lenient()
                .when(jdbcQueryExecutor.executeQuery(any(), anyString()))
                .thenReturn(testResultDTO);
        lenient().when(sqlQueryHistoryMapper.insert(any())).thenReturn(1);

        // 设置TableValidationService的默认行为
        lenient()
                .when(tableValidationService.processSqlWithLimit(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 默认返回原SQL
    }

    @Test
    @Story("SQL查询执行")
    @Description("测试成功执行SQL查询的功能")
    @Severity(SeverityLevel.CRITICAL)
    void testExecuteQuery_Success() {
        Allure.step(
                "执行SQL查询",
                () -> {
                    Allure.parameter("用户ID", testUser.getId());
                    Allure.parameter("数据源ID", testQueryDTO.getDatasourceId());
                    Allure.parameter("SQL语句", testQueryDTO.getSql());
                });

        // 执行测试
        SqlQueryResultDTO result = sqlQueryService.executeQuery(testUser.getId(), testQueryDTO);

        Allure.step(
                "验证查询结果",
                () -> {
                    assertNotNull(result);
                    assertEquals(testResultDTO.getColumns(), result.getColumns());
                    assertEquals(testResultDTO.getRows(), result.getRows());
                    assertEquals(testResultDTO.getExecutionTimeMs(), result.getExecutionTimeMs());

                    Allure.attachment(
                            "查询结果",
                            "列数: "
                                    + result.getColumns().size()
                                    + ", 行数: "
                                    + result.getRows().size());
                });

        // 验证调用
        verify(datasourceMapper).selectById(testQueryDTO.getDatasourceId());
        verify(userMapper).selectById(testUser.getId());
        verify(jdbcQueryExecutor).executeQuery(testDatasourceInfo, testQueryDTO.getSql());
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker)
                .checkQueryPermission(
                        testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
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
        verify(jdbcQueryExecutor).executeQuery(testDatasourceInfo, testQueryDTO.getSql());
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(sqlQueryHistoryMapper).insert(any(SqlQueryHistory.class));
        verify(permissionChecker)
                .checkQueryPermission(
                        testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
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
        verify(permissionChecker, never()).checkQueryPermission(any(), any(), any());
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
        verify(permissionChecker, never()).checkQueryPermission(any(), any(), any());
    }

    @Test
    void testExecuteQuery_InvalidSql() {
        // 准备数据
        testQueryDTO.setSql("");

        // 模拟权限检查器抛出异常
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空"))
                .when(permissionChecker)
                .checkQueryPermission(any(), anyLong(), eq(""));

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
        verify(permissionChecker)
                .checkQueryPermission(testUser, testQueryDTO.getDatasourceId(), "");
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
        verify(jdbcQueryExecutor).executeQuery(testDatasourceInfo, testQueryDTO.getSql());
        verify(exporterFactory).getExporter("xlsx");
        verify(fileExporter).exportToFile(any(), anyString());
        verify(permissionChecker)
                .checkQueryPermission(
                        testUser, testQueryDTO.getDatasourceId(), testQueryDTO.getSql());
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
    @Description("测试SqlQueryService正确委托TableValidationService处理LIMIT逻辑")
    @Severity(SeverityLevel.CRITICAL)
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
        verify(jdbcQueryExecutor).executeQuery(testDatasourceInfo, processedSql);
    }
}
