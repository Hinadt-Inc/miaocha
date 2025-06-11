package com.hinadt.miaocha.application.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.constants.FieldConstants;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import io.qameta.allure.*;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TableValidationServiceImpl 单元测试类 重点测试message字段校验功能
 *
 * <p>测试范围： 1. validateTableStructure方法的各种场景 2. 数据源验证 3. 表结构验证 4. message字段检查 5. 异常处理 6. 边界情况
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("表验证服务测试")
public class TableValidationServiceImplTest {

    @Mock private DatasourceMapper datasourceMapper;

    @Mock private JdbcQueryExecutor jdbcQueryExecutor;

    @Mock private DatabaseMetadataServiceFactory metadataServiceFactory;

    @Mock private DatabaseMetadataService metadataService;

    @Mock private Connection connection;

    private TableValidationServiceImpl tableValidationService;

    @BeforeEach
    public void setUp() {
        tableValidationService =
                spy(
                        new TableValidationServiceImpl(
                                datasourceMapper, jdbcQueryExecutor, metadataServiceFactory));
    }

    @Test
    @DisplayName("validateTableStructure - 表包含message字段 - 验证成功")
    public void testValidateTableStructureWithMessageField() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn(FieldConstants.MESSAGE_FIELD, "text"),
                        createColumn("level", "varchar"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });

        // 验证调用
        verify(metadataService).getColumnInfo(connection, tableName);
    }

    @Test
    @DisplayName("validateTableStructure - 表缺少message字段 - 抛出异常")
    public void testValidateTableStructureWithoutMessageField() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn("level", "varchar"),
                        createColumn("timestamp", "datetime"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(datasourceId, tableName);
                        });

        assertEquals(ErrorCode.TABLE_MESSAGE_FIELD_MISSING, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("message"));
        assertTrue(exception.getMessage().contains(tableName));
    }

    @Test
    @DisplayName("validateTableStructure - message字段名大小写不敏感")
    public void testValidateTableStructureCaseInsensitive() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn("MESSAGE", "text"), // 大写的MESSAGE字段
                        createColumn("level", "varchar"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });
    }

    @Test
    @DisplayName("validateTableStructure - 数据源不存在 - 抛出异常")
    public void testValidateTableStructureDataSourceNotFound() {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        // Mock 行为
        when(datasourceMapper.selectById(datasourceId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(datasourceId, tableName);
                        });

        assertEquals(ErrorCode.DATASOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("validateTableStructure - 空列信息 - 抛出异常")
    public void testValidateTableStructureEmptyColumns() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);
        List<SchemaInfoDTO.ColumnInfoDTO> columns = Collections.emptyList();

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(datasourceId, tableName);
                        });

        assertEquals(ErrorCode.TABLE_MESSAGE_FIELD_MISSING, exception.getErrorCode());
    }

    @Test
    @DisplayName("validateTableStructure - 获取元数据异常 - 抛出异常")
    public void testValidateTableStructureMetadataException() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName))
                .thenThrow(new RuntimeException("Database error"));

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(datasourceId, tableName);
                        });

        assertEquals(ErrorCode.TABLE_FIELD_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Database error"));
    }

    @Test
    @DisplayName("validateTableStructure - 数据库连接异常 - 抛出异常")
    public void testValidateTableStructureConnectionException() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo))
                .thenThrow(new RuntimeException("Connection failed"));

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(datasourceId, tableName);
                        });

        assertEquals(ErrorCode.TABLE_FIELD_VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Connection failed"));
    }

    @Test
    @DisplayName("validateTableStructure - 多种字段类型包含message - 验证成功")
    public void testValidateTableStructureWithVariousFieldTypes() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.MYSQL);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn("timestamp", "datetime"),
                        createColumn("level", "varchar(50)"),
                        createColumn(FieldConstants.MESSAGE_FIELD, "longtext"),
                        createColumn("service_name", "varchar(100)"),
                        createColumn("trace_id", "varchar(64)"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.MYSQL.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });

        verify(metadataService).getColumnInfo(connection, tableName);
        verify(datasourceMapper).selectById(datasourceId);
        verify(jdbcQueryExecutor).getConnection(datasourceInfo);
        verify(metadataServiceFactory).getService(DatasourceType.MYSQL.name());
    }

    @Test
    @DisplayName("validateTableStructure - PostgreSQL数据源 - 验证成功")
    public void testValidateTableStructureWithPostgreSQL() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.POSTGRESQL);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigserial"),
                        createColumn("message", "text"),
                        createColumn("created_at", "timestamp"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.POSTGRESQL.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });
    }

    @Test
    @DisplayName("validateTableStructure - 表名包含特殊字符 - 验证成功")
    public void testValidateTableStructureWithSpecialTableName() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "log_table_2024_01_01"; // 包含下划线和数字的表名

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("log_id", "varchar(64)"),
                        createColumn(FieldConstants.MESSAGE_FIELD, "string"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });
    }

    @Test
    @DisplayName("validateTableStructure - 混合大小写message字段 - 验证成功")
    public void testValidateTableStructureWithMixedCaseMessage() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn("Message", "text"), // 首字母大写
                        createColumn("level", "varchar"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });
    }

    @Test
    @DisplayName("validateTableStructure - 只有message字段的表 - 验证成功")
    public void testValidateTableStructureWithOnlyMessageField() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "simple_log_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(createColumn(FieldConstants.MESSAGE_FIELD, "text"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });
    }

    @Test
    @DisplayName("validateTableStructure - null参数处理")
    public void testValidateTableStructureWithNullParameters() {
        // 测试null datasourceId - 表不存在的检查在isTableExists中
        doReturn(false).when(tableValidationService).isTableExists(null, "test_table");

        BusinessException exception1 =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(null, "test_table");
                        });
        assertEquals(ErrorCode.LOGSTASH_TARGET_TABLE_NOT_FOUND, exception1.getErrorCode());

        // 测试null tableName
        doReturn(false).when(tableValidationService).isTableExists(1L, null);

        BusinessException exception2 =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            tableValidationService.validateTableStructure(1L, null);
                        });
        assertEquals(ErrorCode.LOGSTASH_TARGET_TABLE_NOT_FOUND, exception2.getErrorCode());
    }

    @Test
    @DisplayName("validateTableStructure - 极大数据量表 - 性能测试")
    public void testValidateTableStructurePerformance() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "large_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        // 创建大量列（模拟包含很多字段的表）
        List<SchemaInfoDTO.ColumnInfoDTO> columns = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            columns.add(createColumn("field_" + i, "varchar(255)"));
        }
        // 在最后添加message字段
        columns.add(createColumn(FieldConstants.MESSAGE_FIELD, "text"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行性能测试
        long startTime = System.currentTimeMillis();

        assertDoesNotThrow(
                () -> {
                    tableValidationService.validateTableStructure(datasourceId, tableName);
                });

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        assertTrue(executionTime < 5000, "处理1000个字段的表应在5秒内完成，实际耗时: " + executionTime + "ms");
    }

    @Test
    @DisplayName("validateTableStructure - 验证调用链完整性")
    public void testValidateTableStructureCallChain() throws Exception {
        // 准备数据
        Long datasourceId = 1L;
        String tableName = "test_table";

        DatasourceInfo datasourceInfo =
                createMockDatasourceInfo(datasourceId, DatasourceType.DORIS);

        List<SchemaInfoDTO.ColumnInfoDTO> columns =
                Arrays.asList(
                        createColumn("id", "bigint"),
                        createColumn(FieldConstants.MESSAGE_FIELD, "text"));

        // Mock 行为
        doReturn(true).when(tableValidationService).isTableExists(datasourceId, tableName);
        when(datasourceMapper.selectById(datasourceId)).thenReturn(datasourceInfo);
        when(jdbcQueryExecutor.getConnection(datasourceInfo)).thenReturn(connection);
        when(metadataServiceFactory.getService(DatasourceType.DORIS.name()))
                .thenReturn(metadataService);
        when(metadataService.getColumnInfo(connection, tableName)).thenReturn(columns);

        // 执行测试
        tableValidationService.validateTableStructure(datasourceId, tableName);

        // 验证所有Mock方法都被正确调用，且调用顺序正确
        verify(datasourceMapper, times(1)).selectById(datasourceId);
        verify(jdbcQueryExecutor, times(1)).getConnection(datasourceInfo);
        verify(metadataServiceFactory, times(1)).getService(DatasourceType.DORIS.name());
        verify(metadataService, times(1)).getColumnInfo(connection, tableName);

        // 验证没有多余的调用（不包括spy对象）
        verifyNoMoreInteractions(
                datasourceMapper, jdbcQueryExecutor, metadataServiceFactory, metadataService);
    }

    // ==================== 辅助方法 ====================

    /** 创建Mock的DatasourceInfo对象 */
    private DatasourceInfo createMockDatasourceInfo(Long id, DatasourceType type) {
        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(id);
        datasourceInfo.setType(type.name());
        datasourceInfo.setName("Test " + type.name());
        datasourceInfo.setIp("192.168.1.100");
        datasourceInfo.setPort(9030);
        datasourceInfo.setUsername("admin");
        datasourceInfo.setPassword("password");
        datasourceInfo.setDatabase("test_db");
        return datasourceInfo;
    }

    /** 创建测试用的列信息对象 */
    private SchemaInfoDTO.ColumnInfoDTO createColumn(String name, String type) {
        SchemaInfoDTO.ColumnInfoDTO column = new SchemaInfoDTO.ColumnInfoDTO();
        column.setColumnName(name);
        column.setDataType(type);
        // 只设置实际存在的字段
        column.setIsNullable(true);
        column.setIsPrimaryKey(false);
        return column;
    }

    /** 创建带有额外属性的列信息对象 */
    private SchemaInfoDTO.ColumnInfoDTO createColumnWithDetails(
            String name, String type, boolean nullable, boolean isPrimaryKey) {
        SchemaInfoDTO.ColumnInfoDTO column = new SchemaInfoDTO.ColumnInfoDTO();
        column.setColumnName(name);
        column.setDataType(type);
        // 只设置实际存在的字段
        column.setIsNullable(nullable);
        column.setIsPrimaryKey(isPrimaryKey);
        return column;
    }
}
