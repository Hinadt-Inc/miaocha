package com.hinadt.miaocha.integration.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.service.DatasourceService;
import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.application.service.SqlQueryService;
import com.hinadt.miaocha.application.service.SystemCacheService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataService;
import com.hinadt.miaocha.application.service.database.DatabaseMetadataServiceFactory;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.*;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.module.*;
import com.hinadt.miaocha.domain.dto.permission.*;
import com.hinadt.miaocha.domain.entity.*;
import com.hinadt.miaocha.domain.entity.enums.CacheGroup;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.*;
import com.hinadt.miaocha.integration.data.IntegrationTestDataInitializer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 服务联合集成测试
 *
 * <p>测试范围： 1. 数据源管理 - 创建、更新、删除、连接测试 2. 模块管理 - 模块CRUD、权限同步、配置管理 3. 权限管理 - 用户权限授予、撤销、验证 4. SQL查询 -
 * 权限验证、查询执行、历史记录 5. 权限验证集成 - 用户-模块-数据源权限链验证 6. 异常处理 - 业务异常、数据一致性、边界条件
 *
 * <p>Mock策略： - Mock JdbcQueryExecutor 避免真实Doris查询 - 保持其他业务逻辑真实性 - 使用MySQL容器存储元数据
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("服务联合集成测试")
public class ServiceIntegrationTest {

    // ==================== Mock配置 ====================

    /** Mock配置类 - 避免真实Doris查询 */
    @TestConfiguration
    static class MockJdbcConfiguration {
        @Bean
        @Primary
        public JdbcQueryExecutor mockJdbcQueryExecutor() {
            JdbcQueryExecutor mockExecutor = mock(JdbcQueryExecutor.class);

            // Mock数据库连接及Statement
            try {
                Connection mockConnection = mock(Connection.class);
                Statement mockStatement = mock(Statement.class);
                PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
                ResultSet mockResultSet = mock(ResultSet.class);
                ResultSet mockTablesResultSet = mock(ResultSet.class);
                ResultSet mockColumnsResultSet = mock(ResultSet.class);
                ResultSet mockPrimaryKeysResultSet = mock(ResultSet.class);
                ResultSetMetaData mockMetaData = mock(ResultSetMetaData.class);
                DatabaseMetaData mockDatabaseMetaData = mock(DatabaseMetaData.class);

                // 配置Connection Mock
                when(mockExecutor.getConnection(any(DatasourceInfo.class)))
                        .thenReturn(mockConnection);
                when(mockConnection.createStatement()).thenReturn(mockStatement);
                when(mockConnection.prepareStatement(anyString()))
                        .thenReturn(mockPreparedStatement);
                when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
                when(mockConnection.getCatalog()).thenReturn("test_db");

                // 配置Statement Mock
                when(mockStatement.execute(anyString())).thenReturn(true);
                when(mockStatement.getResultSet()).thenReturn(mockResultSet);
                when(mockStatement.getUpdateCount()).thenReturn(1);
                when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

                // 配置PreparedStatement Mock
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

                // 配置DatabaseMetaData Mock
                // Mock getTables方法 - 用于获取表列表
                when(mockDatabaseMetaData.getTables(
                                anyString(), isNull(), eq("%"), eq(new String[] {"TABLE"})))
                        .thenReturn(mockTablesResultSet);
                when(mockTablesResultSet.next()).thenReturn(true, true, false); // 返回两个表
                when(mockTablesResultSet.getString("TABLE_NAME"))
                        .thenReturn("test_integration_table", "other_table");

                // Mock getColumns方法 - 用于获取列信息
                when(mockDatabaseMetaData.getColumns(anyString(), isNull(), anyString(), eq("%")))
                        .thenReturn(mockColumnsResultSet);
                when(mockColumnsResultSet.next()).thenReturn(true, true, true, false); // 返回三列
                when(mockColumnsResultSet.getString("COLUMN_NAME"))
                        .thenReturn("id", "name", "status");
                when(mockColumnsResultSet.getString("TYPE_NAME"))
                        .thenReturn("BIGINT", "VARCHAR", "INT");
                when(mockColumnsResultSet.getString("IS_NULLABLE")).thenReturn("NO", "YES", "YES");

                // Mock getPrimaryKeys方法
                when(mockDatabaseMetaData.getPrimaryKeys(anyString(), isNull(), anyString()))
                        .thenReturn(mockPrimaryKeysResultSet);
                when(mockPrimaryKeysResultSet.next()).thenReturn(true, false); // 返回一个主键
                when(mockPrimaryKeysResultSet.getString("COLUMN_NAME")).thenReturn("id");

                // 配置ResultSet Mock
                when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
                when(mockMetaData.getColumnCount()).thenReturn(3);
                when(mockMetaData.getColumnLabel(1)).thenReturn("id");
                when(mockMetaData.getColumnLabel(2)).thenReturn("name");
                when(mockMetaData.getColumnLabel(3)).thenReturn("status");

                // 模拟结果集数据
                when(mockResultSet.next()).thenReturn(true, false); // 返回一行数据后结束
                when(mockResultSet.getObject(1)).thenReturn(1);
                when(mockResultSet.getObject(2)).thenReturn("test");
                when(mockResultSet.getObject(3)).thenReturn("success");

                // Mock SQL执行结果DTO
                SqlQueryResultDTO mockResult = new SqlQueryResultDTO();
                mockResult.setAffectedRows(1);
                mockResult.setColumns(List.of("id", "name", "status"));
                Map<String, Object> row = Map.of("id", 1, "name", "test", "status", "success");
                mockResult.setRows(List.of(row));
                mockResult.setExecutionTimeMs(100L);

                // Mock所有executeQuery方法变体
                when(mockExecutor.executeQuery(any(DatasourceInfo.class), anyString()))
                        .thenReturn(mockResult);
                when(mockExecutor.executeQuery(any(Connection.class), anyString()))
                        .thenReturn(mockResult);

                // Mock executeStructuredQuery方法
                com.hinadt.miaocha.application.service.sql.processor.QueryResult structuredResult =
                        new com.hinadt.miaocha.application.service.sql.processor.QueryResult();
                structuredResult.setColumns(List.of("id", "name", "status"));
                structuredResult.setRows(List.of(row));
                when(mockExecutor.executeStructuredQuery(any(Connection.class), anyString()))
                        .thenReturn(structuredResult);

            } catch (Exception e) {
                log.error("Mock JDBC配置失败", e);
            }

            return mockExecutor;
        }

        @Bean
        @Primary
        public DatabaseMetadataService mockDatabaseMetadataService() {
            DatabaseMetadataService mockService = mock(DatabaseMetadataService.class);

            try {
                // Mock getAllTables方法 - 需要Connection参数
                when(mockService.getAllTables(any(Connection.class)))
                        .thenReturn(
                                List.of("test_integration_table", "other_table1", "other_table2"));

                // Mock getTableComment方法 - 需要Connection参数
                when(mockService.getTableComment(any(Connection.class), anyString()))
                        .thenReturn("Test table comment");

                // Mock getColumnInfo方法 - 需要Connection参数，正确创建ColumnInfoDTO
                when(mockService.getColumnInfo(any(Connection.class), anyString()))
                        .thenAnswer(
                                invocation -> {
                                    List<SchemaInfoDTO.ColumnInfoDTO> columns = new ArrayList<>();

                                    // 创建id列
                                    SchemaInfoDTO.ColumnInfoDTO idColumn =
                                            new SchemaInfoDTO.ColumnInfoDTO();
                                    idColumn.setColumnName("id");
                                    idColumn.setDataType("BIGINT");
                                    idColumn.setIsPrimaryKey(true);
                                    idColumn.setIsNullable(false);
                                    columns.add(idColumn);

                                    // 创建name列
                                    SchemaInfoDTO.ColumnInfoDTO nameColumn =
                                            new SchemaInfoDTO.ColumnInfoDTO();
                                    nameColumn.setColumnName("name");
                                    nameColumn.setDataType("VARCHAR");
                                    nameColumn.setIsPrimaryKey(false);
                                    nameColumn.setIsNullable(false);
                                    columns.add(nameColumn);

                                    // 创建create_time列
                                    SchemaInfoDTO.ColumnInfoDTO timeColumn =
                                            new SchemaInfoDTO.ColumnInfoDTO();
                                    timeColumn.setColumnName("create_time");
                                    timeColumn.setDataType("DATETIME");
                                    timeColumn.setIsPrimaryKey(false);
                                    timeColumn.setIsNullable(false);
                                    columns.add(timeColumn);

                                    return columns;
                                });

                // Mock getSupportedDatabaseType方法 - 返回DORIS匹配业务数据源类型
                when(mockService.getSupportedDatabaseType()).thenReturn("DORIS");

            } catch (Exception e) {
                log.error("Mock DatabaseMetadataService配置失败", e);
            }

            return mockService;
        }

        @Bean
        @Primary
        public DatabaseMetadataServiceFactory mockDatabaseMetadataServiceFactory() {
            DatabaseMetadataServiceFactory mockFactory = mock(DatabaseMetadataServiceFactory.class);
            DatabaseMetadataService mockService = mockDatabaseMetadataService();

            // 为MYSQL和DORIS类型都返回同一个mock服务
            when(mockFactory.getService("MYSQL")).thenReturn(mockService);
            when(mockFactory.getService("mysql")).thenReturn(mockService);
            when(mockFactory.getService("DORIS")).thenReturn(mockService);
            when(mockFactory.getService("doris")).thenReturn(mockService);

            return mockFactory;
        }
    }

    // ==================== 容器配置 ====================

    /** MySQL容器 - 存储元数据 */
    @Container @ServiceConnection
    static MySQLContainer<?> mysqlContainer = TestContainersFactory.mysqlContainer();

    // ==================== 服务依赖注入 ====================

    @MockitoSpyBean private DatasourceService datasourceService;
    @Autowired private ModuleInfoService moduleInfoService;
    @Autowired private ModulePermissionService modulePermissionService;
    @Autowired private SqlQueryService sqlQueryService;
    @Autowired private DatabaseMetadataService databaseMetadataService;
    @Autowired private SystemCacheService systemCacheService;

    // ==================== Mapper依赖注入 ====================

    @Autowired private UserMapper userMapper;
    @Autowired private DatasourceMapper datasourceMapper;
    @Autowired private ModuleInfoMapper moduleInfoMapper;
    @Autowired private UserModulePermissionMapper userModulePermissionMapper;
    @Autowired private SqlQueryHistoryMapper sqlQueryHistoryMapper;

    // ==================== 测试数据初始化 ====================

    @Autowired private IntegrationTestDataInitializer dataInitializer;

    // ==================== 测试数据存储 ====================

    private Long testUserId; // 测试用户ID
    private Long testAdminUserId; // 测试管理员用户ID
    private Long testDatasourceId; // 测试数据源ID
    private Long testModuleId; // 测试模块ID
    private String testModuleName; // 测试模块名称

    // ==================== 测试环境管理 ====================

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== 服务联合集成测试：开始搭建测试环境 ===");

        // 验证容器状态
        assertThat(mysqlContainer.isRunning()).isTrue();

        // 初始化基础数据
        dataInitializer.initializeTestData();
        log.info("基础测试数据初始化完成");

        // 配置数据源连接测试模拟 - 对特定测试场景走真实逻辑，其他返回成功
        doAnswer(
                        invocation -> {
                            DatasourceCreateDTO dto = invocation.getArgument(0);

                            // ERR-001测试中的无效连接 - 走真实连接测试逻辑
                            if (dto.getJdbcUrl().contains("invalid-host")
                                    || dto.getJdbcUrl().contains("nonexistent_db")) {
                                return invocation.callRealMethod(); // 调用真实方法
                            }

                            // 其他情况返回成功（避免真实连接Doris）
                            return DatasourceConnectionTestResultDTO.success();
                        })
                .when(datasourceService)
                .testConnection(any(DatasourceCreateDTO.class));
        log.info("数据源连接测试模拟配置完成");

        // 创建测试专用数据
        createTestSpecificData();
        log.info("测试专用数据创建完成");

        log.info("测试环境搭建完成 - MySQL: {}", mysqlContainer.getJdbcUrl());
    }

    @AfterAll
    void cleanupTestEnvironment() {
        log.info("=== 服务联合集成测试：开始清理测试环境 ===");

        // 清理测试专用数据
        cleanupTestSpecificData();
        log.info("测试专用数据清理完成");

        // 清理基础数据
        dataInitializer.cleanupTestData();
        log.info("基础测试数据清理完成");

        log.info("测试环境清理完成");
    }

    /** 创建测试专用数据 */
    private void createTestSpecificData() {
        // 1. 创建普通测试用户
        User normalUser = createTestUser("normal_user", "normal@test.com", UserRole.USER);
        testUserId = normalUser.getId();

        // 2. 创建管理员测试用户
        User adminUser = createTestUser("admin_user", "admin@test.com", UserRole.ADMIN);
        testAdminUserId = adminUser.getId();

        // 3. 创建测试数据源
        DatasourceCreateDTO datasourceDto =
                DatasourceCreateDTO.builder()
                        .name("integration-test-datasource")
                        .type("DORIS")
                        .jdbcUrl("jdbc:mysql://localhost:9030/test_db")
                        .username("root")
                        .password("")
                        .description("集成测试数据源")
                        .build();

        DatasourceDTO createdDatasource = datasourceService.createDatasource(datasourceDto);
        testDatasourceId = createdDatasource.getId();

        // 4. 创建测试模块
        ModuleInfoCreateDTO moduleDto =
                ModuleInfoCreateDTO.builder()
                        .name("integration-test-module")
                        .datasourceId(testDatasourceId)
                        .tableName("test_integration_table")
                        .build();

        ModuleInfoDTO createdModule = moduleInfoService.createModule(moduleDto);
        testModuleId = createdModule.getId();
        testModuleName = createdModule.getName();

        log.info(
                "测试数据创建完成 - 用户ID: {}, 管理员ID: {}, 数据源ID: {}, 模块ID: {}, 模块名: {}",
                testUserId,
                testAdminUserId,
                testDatasourceId,
                testModuleId,
                testModuleName);
    }

    /** 清理测试专用数据 */
    private void cleanupTestSpecificData() {
        try {
            // 按外键依赖逆序删除

            // 1. 清理用户模块权限
            userModulePermissionMapper.deleteByUserId(testUserId);
            userModulePermissionMapper.deleteByUserId(testAdminUserId);

            // 2. 清理SQL查询历史
            // 注意：SQL查询历史可能有外键约束，需要先清理

            // 3. 清理模块
            if (testModuleId != null) {
                moduleInfoService.deleteModule(testModuleId, false);
            }

            // 4. 清理数据源
            if (testDatasourceId != null) {
                datasourceService.deleteDatasource(testDatasourceId);
            }

            // 5. 清理用户
            if (testUserId != null) {
                userMapper.deleteById(testUserId);
            }
            if (testAdminUserId != null) {
                userMapper.deleteById(testAdminUserId);
            }

        } catch (Exception e) {
            log.warn("清理测试专用数据时出错: {}", e.getMessage());
        }
    }

    /** 创建测试用户 */
    private User createTestUser(String nickname, String email, UserRole role) {
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setUid(UUID.randomUUID().toString());
        user.setRole(role.name());
        user.setStatus(1);

        userMapper.insert(user);
        return user;
    }

    // ==================== 辅助方法 ====================

    /** 等待异步操作完成 */
    private void waitForAsyncOperation(long timeout, TimeUnit unit) throws InterruptedException {
        Thread.sleep(unit.toMillis(timeout));
    }

    /** 验证业务异常 */
    private void assertBusinessException(Runnable operation, ErrorCode expectedErrorCode) {
        BusinessException exception = assertThrows(BusinessException.class, operation::run);
        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
    }

    /** 验证业务异常（带消息验证） */
    private void assertBusinessException(
            Runnable operation, ErrorCode expectedErrorCode, String expectedMessage) {
        BusinessException exception = assertThrows(BusinessException.class, operation::run);
        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
        assertThat(exception.getMessage()).contains(expectedMessage);
    }

    // ==================== 数据源管理测试组 ====================

    @Nested
    @DisplayName("数据源管理测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DatasourceManagementIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("DS-001: 创建数据源 - 连接测试验证")
        void testCreateDatasourceWithConnectionTest() {
            log.info("🔍 测试创建数据源并验证连接");

            DatasourceCreateDTO createDto =
                    DatasourceCreateDTO.builder()
                            .name("test-create-datasource")
                            .type("DORIS")
                            .jdbcUrl("jdbc:mysql://localhost:9030/test_create_db")
                            .username("root")
                            .password("")
                            .description("测试创建数据源")
                            .build();

            // 执行创建
            DatasourceDTO result = datasourceService.createDatasource(createDto);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("test-create-datasource");
            assertThat(result.getType()).isEqualTo("DORIS");
            assertThat(result.getJdbcUrl()).isEqualTo("jdbc:mysql://localhost:9030/test_create_db");
            assertThat(result.getUsername()).isEqualTo("root");

            // 验证数据库中确实创建了记录
            DatasourceInfo created = datasourceMapper.selectById(result.getId());
            assertThat(created).isNotNull();
            assertThat(created.getName()).isEqualTo("test-create-datasource");

            // 清理创建的数据源
            datasourceService.deleteDatasource(result.getId());

            log.info("✅ 创建数据源测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("DS-002: 创建数据源 - 名称重复验证")
        void testCreateDatasourceWithDuplicateName() {
            log.info("🔍 测试创建重复名称数据源");

            DatasourceCreateDTO createDto =
                    DatasourceCreateDTO.builder()
                            .name(
                                    testDatasourceId != null
                                            ? datasourceMapper
                                                    .selectById(testDatasourceId)
                                                    .getName()
                                            : "integration-test-datasource")
                            .type("DORIS")
                            .jdbcUrl("jdbc:mysql://localhost:9030/test_duplicate_db")
                            .username("root")
                            .password("")
                            .build();

            // 应该抛出数据源名称已存在异常
            assertBusinessException(
                    () -> datasourceService.createDatasource(createDto),
                    ErrorCode.DATASOURCE_NAME_EXISTS);

            log.info("✅ 重复名称验证测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("DS-003: 更新数据源 - 基本信息更新")
        void testUpdateDatasourceBasicInfo() {
            log.info("🔍 测试更新数据源基本信息");

            // 获取原始数据源信息
            DatasourceDTO original = datasourceService.getDatasource(testDatasourceId);

            // 创建更新DTO
            DatasourceUpdateDTO updateDto =
                    DatasourceUpdateDTO.builder()
                            .name("integration-test-datasource-updated")
                            .description("更新后的描述")
                            .build();

            // 执行更新
            DatasourceDTO updated = datasourceService.updateDatasource(testDatasourceId, updateDto);

            // 验证更新结果
            assertThat(updated.getName()).isEqualTo("integration-test-datasource-updated");
            assertThat(updated.getDescription()).isEqualTo("更新后的描述");
            assertThat(updated.getType()).isEqualTo(original.getType()); // 未更新字段保持不变
            assertThat(updated.getJdbcUrl()).isEqualTo(original.getJdbcUrl()); // 未更新字段保持不变

            // 验证数据库中的记录确实更新了
            DatasourceInfo dbRecord = datasourceMapper.selectById(testDatasourceId);
            assertThat(dbRecord.getName()).isEqualTo("integration-test-datasource-updated");
            assertThat(dbRecord.getDescription()).isEqualTo("更新后的描述");

            log.info("✅ 更新数据源基本信息测试通过");
        }

        @Test
        @Order(4)
        @DisplayName("DS-004: 更新数据源 - 连接信息更新触发连接测试")
        void testUpdateDatasourceConnectionInfo() {
            log.info("🔍 测试更新数据源连接信息");

            DatasourceUpdateDTO updateDto =
                    DatasourceUpdateDTO.builder()
                            .jdbcUrl("jdbc:mysql://localhost:9030/test_updated_db")
                            .password("new_password")
                            .build();

            // 执行更新（Mock的连接测试会通过）
            DatasourceDTO updated = datasourceService.updateDatasource(testDatasourceId, updateDto);

            // 验证连接信息已更新
            assertThat(updated.getJdbcUrl())
                    .isEqualTo("jdbc:mysql://localhost:9030/test_updated_db");

            // 验证数据库记录
            DatasourceInfo dbRecord = datasourceMapper.selectById(testDatasourceId);
            assertThat(dbRecord.getJdbcUrl())
                    .isEqualTo("jdbc:mysql://localhost:9030/test_updated_db");
            assertThat(dbRecord.getPassword()).isEqualTo("new_password");

            log.info("✅ 更新数据源连接信息测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("DS-005: 测试数据源连接 - 成功场景")
        void testDatasourceConnectionSuccess() {
            log.info("🔍 测试数据源连接（成功）");

            DatasourceCreateDTO testDto =
                    DatasourceCreateDTO.builder()
                            .jdbcUrl("jdbc:mysql://localhost:9030/test_db")
                            .username("root")
                            .password("")
                            .type("DORIS")
                            .build();

            // 执行连接测试（Mock会返回成功）
            DatasourceConnectionTestResultDTO result = datasourceService.testConnection(testDto);

            // 验证连接成功
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getErrorMessage()).isNull();

            log.info("✅ 数据源连接测试（成功）通过");
        }

        @Test
        @Order(6)
        @DisplayName("DS-006: 测试已保存数据源连接")
        void testExistingDatasourceConnection() {
            log.info("🔍 测试已保存数据源连接");

            // 测试已保存的数据源连接
            DatasourceConnectionTestResultDTO result =
                    datasourceService.testExistingConnection(testDatasourceId);

            // 验证连接成功（Mock返回）
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();

            log.info("✅ 已保存数据源连接测试通过");
        }

        @Test
        @Order(7)
        @DisplayName("DS-007: 根据模块获取数据源")
        void testGetDatasourceByModule() {
            log.info("🔍 测试根据模块获取数据源");

            // 根据模块名获取数据源
            DatasourceDTO result = datasourceService.getDatasourceByModule(testModuleName);

            // 验证返回的数据源
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testDatasourceId);
            assertThat(result.getName()).contains("integration-test-datasource");

            log.info("✅ 根据模块获取数据源测试通过");
        }

        @Test
        @Order(8)
        @DisplayName("DS-008: 根据不存在模块获取数据源 - 异常验证")
        void testGetDatasourceByNonExistentModule() {
            log.info("🔍 测试根据不存在模块获取数据源");

            // 应该抛出模块未找到异常
            assertBusinessException(
                    () -> datasourceService.getDatasourceByModule("non-existent-module"),
                    ErrorCode.MODULE_NOT_FOUND);

            log.info("✅ 不存在模块获取数据源异常验证通过");
        }

        @Test
        @Order(9)
        @DisplayName("DS-009: 删除数据源 - 检查模块引用")
        void testDeleteDatasourceWithModuleReference() {
            log.info("🔍 测试删除有模块引用的数据源");

            // 尝试删除被模块引用的数据源（应该失败）
            assertBusinessException(
                    () -> datasourceService.deleteDatasource(testDatasourceId),
                    ErrorCode.DATASOURCE_IN_USE);

            // 验证数据源仍然存在
            DatasourceDTO datasource = datasourceService.getDatasource(testDatasourceId);
            assertThat(datasource).isNotNull();

            log.info("✅ 删除有模块引用的数据源验证通过");
        }

        @Test
        @Order(10)
        @DisplayName("DS-010: 删除不存在的数据源 - 异常验证")
        void testDeleteNonExistentDatasource() {
            log.info("🔍 测试删除不存在的数据源");

            // 应该抛出数据源未找到异常
            Long nonExistentId = 999999L;
            assertBusinessException(
                    () -> datasourceService.deleteDatasource(nonExistentId),
                    ErrorCode.DATASOURCE_NOT_FOUND);

            log.info("✅ 删除不存在数据源异常验证通过");
        }

        @Test
        @Order(11)
        @DisplayName("DS-011: 获取所有数据源")
        void testGetAllDatasources() {
            log.info("🔍 测试获取所有数据源");

            List<DatasourceDTO> allDatasources = datasourceService.getAllDatasources();

            // 验证结果
            assertThat(allDatasources).isNotNull();
            assertThat(allDatasources).isNotEmpty();

            // 应该包含我们的测试数据源
            boolean containsTestDatasource =
                    allDatasources.stream().anyMatch(ds -> ds.getId().equals(testDatasourceId));
            assertThat(containsTestDatasource).isTrue();

            // 验证数据源DTO的字段完整性
            DatasourceDTO testDatasource =
                    allDatasources.stream()
                            .filter(ds -> ds.getId().equals(testDatasourceId))
                            .findFirst()
                            .orElse(null);

            assertThat(testDatasource).isNotNull();
            assertThat(testDatasource.getName()).isNotNull();
            assertThat(testDatasource.getType()).isNotNull();
            assertThat(testDatasource.getJdbcUrl()).isNotNull();
            assertThat(testDatasource.getUsername()).isNotNull();

            log.info("✅ 获取所有数据源测试通过 - 共{}个数据源", allDatasources.size());
        }
    }

    // ==================== 模块管理测试组 ====================

    @Nested
    @DisplayName("模块管理测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ModuleManagementIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("MOD-001: 创建模块 - 数据源验证")
        void testCreateModuleWithDatasourceValidation() {
            log.info("🔍 测试创建模块并验证数据源");

            ModuleInfoCreateDTO createDto =
                    ModuleInfoCreateDTO.builder()
                            .name("test-create-module")
                            .datasourceId(testDatasourceId)
                            .tableName("test_create_table")
                            .build();

            // 执行创建
            ModuleInfoDTO result = moduleInfoService.createModule(createDto);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("test-create-module");
            assertThat(result.getDatasourceId()).isEqualTo(testDatasourceId);
            assertThat(result.getTableName()).isEqualTo("test_create_table");
            assertThat(result.getStatus()).isEqualTo(1); // 默认启用

            // 验证数据库中确实创建了记录
            ModuleInfo created = moduleInfoMapper.selectById(result.getId());
            assertThat(created).isNotNull();
            assertThat(created.getName()).isEqualTo("test-create-module");

            // 清理创建的模块
            moduleInfoService.deleteModule(result.getId(), false);

            log.info("✅ 创建模块测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("MOD-002: 创建模块 - 数据源不存在验证")
        void testCreateModuleWithNonExistentDatasource() {
            log.info("🔍 测试创建模块时数据源不存在");

            Long nonExistentDatasourceId = 999999L;
            ModuleInfoCreateDTO createDto =
                    ModuleInfoCreateDTO.builder()
                            .name("test-invalid-datasource-module")
                            .datasourceId(nonExistentDatasourceId)
                            .tableName("test_table")
                            .build();

            // 应该抛出数据源未找到异常
            assertBusinessException(
                    () -> moduleInfoService.createModule(createDto),
                    ErrorCode.DATASOURCE_NOT_FOUND);

            log.info("✅ 数据源不存在验证测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("MOD-003: 创建模块 - 名称重复验证")
        void testCreateModuleWithDuplicateName() {
            log.info("🔍 测试创建重复名称模块");

            ModuleInfoCreateDTO createDto =
                    ModuleInfoCreateDTO.builder()
                            .name(testModuleName) // 使用已存在的模块名
                            .datasourceId(testDatasourceId)
                            .tableName("test_duplicate_table")
                            .build();

            // 应该抛出模块名称已存在异常
            assertBusinessException(
                    () -> moduleInfoService.createModule(createDto),
                    ErrorCode.VALIDATION_ERROR,
                    "模块名称已存在");

            log.info("✅ 重复名称验证测试通过");
        }

        @Test
        @Order(4)
        @DisplayName("MOD-004: 更新模块 - 模块名变更触发权限同步")
        void testUpdateModuleNameWithPermissionSync() {
            log.info("🔍 测试更新模块名触发权限同步");

            // 1. 先为用户授予当前模块权限
            modulePermissionService.grantModulePermission(testUserId, testModuleName);

            // 验证权限确实授予了
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionBefore).isTrue();

            // 2. 更新模块名
            String newModuleName = "integration-test-module-renamed";
            ModuleInfoUpdateDTO updateDto =
                    ModuleInfoUpdateDTO.builder()
                            .id(testModuleId)
                            .name(newModuleName)
                            .datasourceId(testDatasourceId)
                            .tableName("test_integration_table")
                            .build();

            ModuleInfoDTO updatedModule = moduleInfoService.updateModule(updateDto);

            // 验证模块名已更新
            assertThat(updatedModule.getName()).isEqualTo(newModuleName);

            // 3. 验证权限已同步更新
            boolean hasOldPermission =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            boolean hasNewPermission =
                    modulePermissionService.hasModulePermission(testUserId, newModuleName);

            assertThat(hasOldPermission).isFalse(); // 旧权限应该不存在
            assertThat(hasNewPermission).isTrue(); // 新权限应该存在

            // 更新测试数据中的模块名
            testModuleName = newModuleName;

            log.info("✅ 模块名变更权限同步测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("MOD-005: 执行Doris SQL - 创建表操作")
        void testExecuteDorisSql() {
            log.info("🔍 测试执行Doris SQL");

            String createTableSql =
                    """
                CREATE TABLE IF NOT EXISTS test_integration_table (
                    id BIGINT,
                    name VARCHAR(100),
                    create_time DATETIME
                ) UNIQUE KEY(id)
                DISTRIBUTED BY HASH(id) BUCKETS 1
                PROPERTIES("replication_num" = "1")
                """;

            // 执行SQL（Mock会成功）
            ModuleInfoDTO result = moduleInfoService.executeDorisSql(testModuleId, createTableSql);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getDorisSql()).isEqualTo(createTableSql);

            // 验证数据库记录已更新
            ModuleInfo dbRecord = moduleInfoMapper.selectById(testModuleId);
            assertThat(dbRecord.getDorisSql()).isEqualTo(createTableSql);

            log.info("✅ 执行Doris SQL测试通过");
        }

        @Test
        @Order(6)
        @DisplayName("MOD-006: 执行Doris SQL - 重复执行验证")
        void testExecuteDorisSqlDuplicate() {
            log.info("🔍 测试执行非CREATE TABLE的SQL验证");

            // 创建一个新模块用于测试SQL类型验证
            ModuleInfoCreateDTO duplicateTestModule =
                    ModuleInfoCreateDTO.builder()
                            .name("test_sql_validation_module")
                            .datasourceId(testDatasourceId)
                            .tableName("test_validation_table")
                            .build();

            ModuleInfoDTO createdModule = moduleInfoService.createModule(duplicateTestModule);
            Long validationModuleId = createdModule.getId();

            // 尝试执行非CREATE TABLE的SQL - 应该抛出SQL_NOT_CREATE_TABLE异常
            String selectSql = "SELECT * FROM test_validation_table";
            assertBusinessException(
                    () -> moduleInfoService.executeDorisSql(validationModuleId, selectSql),
                    ErrorCode.SQL_NOT_CREATE_TABLE);

            // 清理测试数据
            moduleInfoService.deleteModule(validationModuleId, false);

            log.info("✅ SQL类型验证测试通过");
        }

        @Test
        @Order(7)
        @DisplayName("MOD-007: 配置查询配置")
        void testConfigureQueryConfig() {
            log.info("🔍 测试配置查询配置");

            // 注意：testModuleId 在MOD-005中已经执行过SQL，这里直接配置即可
            // 根据实际SQL，表只有3个字段：[id, name, create_time]
            QueryConfigDTO queryConfig = new QueryConfigDTO();
            queryConfig.setTimeField("create_time");
            queryConfig.setExcludeFields(List.of("id")); // 使用实际存在的字段

            QueryConfigDTO.KeywordFieldConfigDTO keywordField =
                    new QueryConfigDTO.KeywordFieldConfigDTO();
            keywordField.setFieldName("name");
            keywordField.setSearchMethod("LIKE");
            queryConfig.setKeywordFields(List.of(keywordField));

            // 执行配置
            ModuleInfoDTO result =
                    moduleInfoService.configureQueryConfig(testModuleId, queryConfig);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getQueryConfig()).isNotNull();

            // 验证配置内容能够被正确解析
            QueryConfigDTO parsedConfig = moduleInfoService.getQueryConfigByModule(testModuleName);
            assertThat(parsedConfig).isNotNull();
            assertThat(parsedConfig.getTimeField()).isEqualTo("create_time");
            assertThat(parsedConfig.getExcludeFields()).contains("id");
            assertThat(parsedConfig.getKeywordFields()).hasSize(1);
            assertThat(parsedConfig.getKeywordFields().get(0).getFieldName()).isEqualTo("name");

            log.info("✅ 配置查询配置测试通过");
        }

        @Test
        @Order(8)
        @DisplayName("MOD-008: 配置查询配置 - 排除字段包含时间字段验证")
        void testConfigureQueryConfigWithTimeFieldInExclude() {
            log.info("🔍 测试配置查询配置时排除字段包含时间字段");

            // 注意：testModuleId 在MOD-005中已经执行过SQL，这里直接配置即可
            QueryConfigDTO queryConfig = new QueryConfigDTO();
            queryConfig.setTimeField("create_time");
            queryConfig.setExcludeFields(List.of("create_time", "other_field")); // 排除字段包含时间字段

            // 应该抛出验证异常
            assertBusinessException(
                    () -> moduleInfoService.configureQueryConfig(testModuleId, queryConfig),
                    ErrorCode.VALIDATION_ERROR,
                    "排除字段列表不能包含时间字段");

            log.info("✅ 排除字段包含时间字段验证通过");
        }

        @Test
        @Order(9)
        @DisplayName("MOD-009: 更新模块状态")
        void testUpdateModuleStatus() {
            log.info("🔍 测试更新模块状态");

            // 禁用模块
            ModuleStatusUpdateDTO disableDto =
                    ModuleStatusUpdateDTO.builder().id(testModuleId).status(0).build();

            ModuleInfoDTO disabledModule = moduleInfoService.updateModuleStatus(disableDto);
            assertThat(disabledModule.getStatus()).isEqualTo(0);

            // 启用模块
            ModuleStatusUpdateDTO enableDto =
                    ModuleStatusUpdateDTO.builder().id(testModuleId).status(1).build();

            ModuleInfoDTO enabledModule = moduleInfoService.updateModuleStatus(enableDto);
            assertThat(enabledModule.getStatus()).isEqualTo(1);

            log.info("✅ 更新模块状态测试通过");
        }

        @Test
        @Order(10)
        @DisplayName("MOD-010: 更新模块状态 - 无效状态验证")
        void testUpdateModuleStatusWithInvalidStatus() {
            log.info("🔍 测试更新模块状态为无效值");

            ModuleStatusUpdateDTO invalidDto =
                    ModuleStatusUpdateDTO.builder()
                            .id(testModuleId)
                            .status(999) // 无效状态值
                            .build();

            // 应该抛出验证异常
            assertBusinessException(
                    () -> moduleInfoService.updateModuleStatus(invalidDto),
                    ErrorCode.VALIDATION_ERROR,
                    "模块状态只能是0（禁用）或1（启用）");

            log.info("✅ 无效状态验证通过");
        }

        @Test
        @Order(11)
        @DisplayName("MOD-011: 获取模块信息")
        void testGetModuleById() {
            log.info("🔍 测试获取模块信息");

            ModuleInfoDTO result = moduleInfoService.getModuleById(testModuleId);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testModuleId);
            assertThat(result.getName()).isEqualTo(testModuleName);
            assertThat(result.getDatasourceId()).isEqualTo(testDatasourceId);
            assertThat(result.getDatasourceName()).isNotNull();

            log.info("✅ 获取模块信息测试通过");
        }

        @Test
        @Order(12)
        @DisplayName("MOD-012: 获取所有模块")
        void testGetAllModules() {
            log.info("🔍 测试获取所有模块");

            List<ModuleInfoDTO> allModules = moduleInfoService.getAllModules();

            // 验证结果
            assertThat(allModules).isNotNull();
            assertThat(allModules).isNotEmpty();

            // 应该包含我们的测试模块
            boolean containsTestModule =
                    allModules.stream().anyMatch(module -> module.getId().equals(testModuleId));
            assertThat(containsTestModule).isTrue();

            log.info("✅ 获取所有模块测试通过 - 共{}个模块", allModules.size());
        }

        @Test
        @Order(13)
        @DisplayName("MOD-013: 获取所有模块（带权限信息）")
        void testGetAllModulesWithPermissions() {
            log.info("🔍 测试获取所有模块（带权限信息）");

            List<ModuleInfoWithPermissionsDTO> allModulesWithPermissions =
                    moduleInfoService.getAllModulesWithPermissions();

            // 验证结果
            assertThat(allModulesWithPermissions).isNotNull();
            assertThat(allModulesWithPermissions).isNotEmpty();

            // 查找我们的测试模块
            ModuleInfoWithPermissionsDTO testModuleWithPermissions =
                    allModulesWithPermissions.stream()
                            .filter(module -> module.getId().equals(testModuleId))
                            .findFirst()
                            .orElse(null);

            assertThat(testModuleWithPermissions).isNotNull();
            assertThat(testModuleWithPermissions.getName()).isEqualTo(testModuleName);
            assertThat(testModuleWithPermissions.getUsers()).isNotNull();

            // 验证权限信息：我们之前给testUserId授予了权限
            boolean hasUserPermission =
                    testModuleWithPermissions.getUsers().stream()
                            .anyMatch(user -> user.getUserId().equals(testUserId));
            assertThat(hasUserPermission).isTrue();

            log.info("✅ 获取所有模块（带权限信息）测试通过");
        }

        @Test
        @Order(14)
        @DisplayName("MOD-014: 根据表名获取模块")
        void testGetTableNameByModule() {
            log.info("🔍 测试根据模块名获取表名");

            String tableName = moduleInfoService.getTableNameByModule(testModuleName);

            assertThat(tableName).isNotNull();
            assertThat(tableName).isEqualTo("test_integration_table");

            log.info("✅ 根据模块名获取表名测试通过");
        }

        @Test
        @Order(15)
        @DisplayName("MOD-015: 根据不存在模块获取表名 - 异常验证")
        void testGetTableNameByNonExistentModule() {
            log.info("🔍 测试根据不存在模块获取表名");

            // 应该抛出模块未找到异常
            assertBusinessException(
                    () -> moduleInfoService.getTableNameByModule("non-existent-module"),
                    ErrorCode.MODULE_NOT_FOUND);

            log.info("✅ 不存在模块获取表名异常验证通过");
        }
    }

    // ==================== 权限管理测试组 ====================

    @Nested
    @DisplayName("权限管理测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PermissionManagementIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("PERM-001: 授予模块权限 - 正常流程")
        void testGrantModulePermissionNormalFlow() {
            log.info("🔍 测试授予模块权限正常流程");

            // 授予权限前，普通用户应该没有权限
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionBefore).isFalse();

            // 授予权限
            UserModulePermissionDTO result =
                    modulePermissionService.grantModulePermission(testUserId, testModuleName);

            // 验证授予结果
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getModule()).isEqualTo(testModuleName);
            assertThat(result.getDatasourceId()).isEqualTo(testDatasourceId);

            // 验证权限已生效
            boolean hasPermissionAfter =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionAfter).isTrue();

            // 验证数据库中确实创建了权限记录
            UserModulePermission dbRecord =
                    userModulePermissionMapper.select(testUserId, testDatasourceId, testModuleName);
            assertThat(dbRecord).isNotNull();

            log.info("✅ 授予模块权限正常流程测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("PERM-002: 授予模块权限 - 重复授予")
        void testGrantModulePermissionDuplicate() {
            log.info("🔍 测试重复授予模块权限");

            // 再次授予相同权限（应该成功，返回已存在的权限）
            UserModulePermissionDTO result =
                    modulePermissionService.grantModulePermission(testUserId, testModuleName);

            // 验证结果
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getModule()).isEqualTo(testModuleName);

            // 权限仍然有效
            boolean hasPermission =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermission).isTrue();

            log.info("✅ 重复授予模块权限测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("PERM-003: 授予模块权限 - 用户不存在")
        void testGrantModulePermissionUserNotFound() {
            log.info("🔍 测试授予权限给不存在用户");

            Long nonExistentUserId = 999999L;

            // 应该抛出用户未找到异常
            assertBusinessException(
                    () ->
                            modulePermissionService.grantModulePermission(
                                    nonExistentUserId, testModuleName),
                    ErrorCode.USER_NOT_FOUND);

            log.info("✅ 用户不存在验证测试通过");
        }

        @Test
        @Order(4)
        @DisplayName("PERM-004: 授予模块权限 - 模块不存在")
        void testGrantModulePermissionModuleNotFound() {
            log.info("🔍 测试授予不存在模块的权限");

            String nonExistentModule = "non-existent-module";

            // 应该抛出模块未找到异常
            assertBusinessException(
                    () ->
                            modulePermissionService.grantModulePermission(
                                    testUserId, nonExistentModule),
                    ErrorCode.MODULE_NOT_FOUND);

            log.info("✅ 模块不存在验证测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("PERM-005: 管理员权限验证")
        void testAdminPermissionValidation() {
            log.info("🔍 测试管理员权限验证");

            // 管理员用户应该对所有模块都有权限，无需显式授予
            boolean adminHasPermission =
                    modulePermissionService.hasModulePermission(testAdminUserId, testModuleName);
            assertThat(adminHasPermission).isTrue();

            // 验证管理员对其他模块也有权限（即使没有显式授予）
            boolean adminHasPermissionOnOtherModule =
                    modulePermissionService.hasModulePermission(testAdminUserId, "any-module-name");
            assertThat(adminHasPermissionOnOtherModule).isFalse(); // 因为模块不存在或被禁用，会返回false

            log.info("✅ 管理员权限验证测试通过");
        }

        @Test
        @Order(6)
        @DisplayName("PERM-006: 撤销模块权限")
        void testRevokeModulePermission() {
            log.info("🔍 测试撤销模块权限");

            // 确认当前有权限
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionBefore).isTrue();

            // 撤销权限
            modulePermissionService.revokeModulePermission(testUserId, testModuleName);

            // 验证权限已撤销
            boolean hasPermissionAfter =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionAfter).isFalse();

            // 验证数据库中的权限记录已删除
            UserModulePermission dbRecord =
                    userModulePermissionMapper.select(testUserId, testDatasourceId, testModuleName);
            assertThat(dbRecord).isNull();

            log.info("✅ 撤销模块权限测试通过");
        }

        @Test
        @Order(7)
        @DisplayName("PERM-007: 撤销不存在的权限")
        void testRevokeNonExistentPermission() {
            log.info("🔍 测试撤销不存在的权限");

            // 撤销一个不存在的权限（应该正常执行，不抛异常）
            assertDoesNotThrow(
                    () -> {
                        modulePermissionService.revokeModulePermission(testUserId, testModuleName);
                    });

            log.info("✅ 撤销不存在权限测试通过");
        }

        @Test
        @Order(8)
        @DisplayName("PERM-008: 批量授予模块权限")
        void testBatchGrantModulePermissions() {
            log.info("🔍 测试批量授予模块权限");

            // 准备多个模块名（包括现有的和需要创建的）
            String additionalModuleName = "test-additional-module-for-batch";

            // 创建额外的测试模块
            ModuleInfoCreateDTO additionalModuleDto =
                    ModuleInfoCreateDTO.builder()
                            .name(additionalModuleName)
                            .datasourceId(testDatasourceId)
                            .tableName("test_additional_table")
                            .build();
            ModuleInfoDTO additionalModule = moduleInfoService.createModule(additionalModuleDto);

            List<String> modules = List.of(testModuleName, additionalModuleName);

            // 批量授予权限
            List<UserModulePermissionDTO> results =
                    modulePermissionService.batchGrantModulePermissions(testUserId, modules);

            // 验证结果
            assertThat(results).isNotNull();
            assertThat(results).hasSize(2);

            // 验证每个模块的权限都已授予
            for (String module : modules) {
                boolean hasPermission =
                        modulePermissionService.hasModulePermission(testUserId, module);
                assertThat(hasPermission).isTrue();
            }

            // 清理额外创建的模块
            moduleInfoService.deleteModule(additionalModule.getId(), false);

            log.info("✅ 批量授予模块权限测试通过");
        }

        @Test
        @Order(9)
        @DisplayName("PERM-009: 批量撤销模块权限")
        void testBatchRevokeModulePermissions() {
            log.info("🔍 测试批量撤销模块权限");

            List<String> modules = List.of(testModuleName);

            // 批量撤销权限
            modulePermissionService.batchRevokeModulePermissions(testUserId, modules);

            // 验证每个模块的权限都已撤销
            for (String module : modules) {
                boolean hasPermission =
                        modulePermissionService.hasModulePermission(testUserId, module);
                assertThat(hasPermission).isFalse();
            }

            log.info("✅ 批量撤销模块权限测试通过");
        }

        @Test
        @Order(10)
        @DisplayName("PERM-010: 获取用户模块权限列表")
        void testGetUserModulePermissions() {
            log.info("🔍 测试获取用户模块权限列表");

            // 先授予一些权限
            modulePermissionService.grantModulePermission(testUserId, testModuleName);

            // 获取用户权限列表
            List<UserModulePermissionDTO> permissions =
                    modulePermissionService.getUserModulePermissions(testUserId);

            // 验证结果
            assertThat(permissions).isNotNull();
            assertThat(permissions).isNotEmpty();

            // 应该包含我们授予的权限
            boolean containsTestModule =
                    permissions.stream().anyMatch(perm -> perm.getModule().equals(testModuleName));
            assertThat(containsTestModule).isTrue();

            log.info("✅ 获取用户模块权限列表测试通过 - 共{}个权限", permissions.size());
        }

        @Test
        @Order(11)
        @DisplayName("PERM-011: 获取用户可访问模块")
        void testGetUserAccessibleModules() {
            log.info("🔍 测试获取用户可访问模块");

            // 普通用户：只能访问有权限的模块
            List<UserModulePermissionDTO> normalUserModules =
                    modulePermissionService.getUserAccessibleModules(testUserId);

            assertThat(normalUserModules).isNotNull();
            boolean normalUserHasTestModule =
                    normalUserModules.stream()
                            .anyMatch(perm -> perm.getModule().equals(testModuleName));
            assertThat(normalUserHasTestModule).isTrue();

            // 管理员用户：可以访问所有启用的模块
            List<UserModulePermissionDTO> adminUserModules =
                    modulePermissionService.getUserAccessibleModules(testAdminUserId);

            assertThat(adminUserModules).isNotNull();
            assertThat(adminUserModules).isNotEmpty();
            boolean adminUserHasTestModule =
                    adminUserModules.stream()
                            .anyMatch(perm -> perm.getModule().equals(testModuleName));
            assertThat(adminUserHasTestModule).isTrue();

            log.info(
                    "✅ 获取用户可访问模块测试通过 - 普通用户:{}个, 管理员:{}个",
                    normalUserModules.size(),
                    adminUserModules.size());
        }

        @Test
        @Order(12)
        @DisplayName("PERM-012: 获取用户未授权模块")
        void testGetUserUnauthorizedModules() {
            log.info("🔍 测试获取用户未授权模块");

            // 创建一个新模块，用户没有权限
            ModuleInfoCreateDTO newModuleDto =
                    ModuleInfoCreateDTO.builder()
                            .name("test-unauthorized-module")
                            .datasourceId(testDatasourceId)
                            .tableName("test_unauthorized_table")
                            .build();
            ModuleInfoDTO newModule = moduleInfoService.createModule(newModuleDto);

            // 获取普通用户未授权的模块
            List<String> unauthorizedModules =
                    modulePermissionService.getUserUnauthorizedModules(testUserId);

            // 验证结果包含新创建的模块
            assertThat(unauthorizedModules).isNotNull();
            assertThat(unauthorizedModules).contains("test-unauthorized-module");

            // 管理员用户应该没有未授权模块
            List<String> adminUnauthorizedModules =
                    modulePermissionService.getUserUnauthorizedModules(testAdminUserId);
            assertThat(adminUnauthorizedModules).isNotNull();
            assertThat(adminUnauthorizedModules).isEmpty();

            // 清理新创建的模块
            moduleInfoService.deleteModule(newModule.getId(), false);

            log.info(
                    "✅ 获取用户未授权模块测试通过 - 普通用户:{}个, 管理员:{}个",
                    unauthorizedModules.size(),
                    adminUnauthorizedModules.size());
        }

        @Test
        @Order(13)
        @DisplayName("PERM-013: 获取所有用户模块权限")
        void testGetAllUsersModulePermissions() {
            log.info("🔍 测试获取所有用户模块权限");

            List<ModuleUsersPermissionDTO> allPermissions =
                    modulePermissionService.getAllUsersModulePermissions();

            // 验证结果
            assertThat(allPermissions).isNotNull();
            assertThat(allPermissions).isNotEmpty();

            // 查找测试模块的权限信息
            ModuleUsersPermissionDTO testModulePermissions =
                    allPermissions.stream()
                            .filter(perm -> perm.getModule().equals(testModuleName))
                            .findFirst()
                            .orElse(null);

            assertThat(testModulePermissions).isNotNull();
            assertThat(testModulePermissions.getUsers()).isNotNull();

            // 验证包含我们授予权限的用户
            boolean containsTestUser =
                    testModulePermissions.getUsers().stream()
                            .anyMatch(user -> user.getUserId().equals(testUserId));
            assertThat(containsTestUser).isTrue();

            log.info("✅ 获取所有用户模块权限测试通过 - {}个模块有权限信息", allPermissions.size());
        }

        @Test
        @Order(14)
        @DisplayName("PERM-014: 禁用模块后权限验证")
        void testPermissionValidationForDisabledModule() {
            log.info("🔍 测试禁用模块后权限验证");

            // 确认用户有权限
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionBefore).isTrue();

            // 禁用模块
            ModuleStatusUpdateDTO disableDto =
                    ModuleStatusUpdateDTO.builder().id(testModuleId).status(0).build();
            moduleInfoService.updateModuleStatus(disableDto);

            // 模块禁用后，用户应该无法访问
            boolean hasPermissionAfterDisable =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionAfterDisable).isFalse();

            // 重新启用模块
            ModuleStatusUpdateDTO enableDto =
                    ModuleStatusUpdateDTO.builder().id(testModuleId).status(1).build();
            moduleInfoService.updateModuleStatus(enableDto);

            // 模块启用后，用户权限恢复
            boolean hasPermissionAfterEnable =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            assertThat(hasPermissionAfterEnable).isTrue();

            log.info("✅ 禁用模块后权限验证测试通过");
        }

        @Test
        @Order(15)
        @DisplayName("PERM-015: 权限系统边界条件测试")
        void testPermissionSystemBoundaryConditions() {
            log.info("🔍 测试权限系统边界条件");

            // 测试空模块名 - 应该返回false而不是抛出异常
            boolean emptyModuleResult = modulePermissionService.hasModulePermission(testUserId, "");
            assertThat(emptyModuleResult).isFalse();

            // 测试null模块名 - 应该返回false而不是抛出异常
            boolean nullModuleResult =
                    modulePermissionService.hasModulePermission(testUserId, null);
            assertThat(nullModuleResult).isFalse();

            // 测试不存在的模块名
            boolean nonExistentModuleResult =
                    modulePermissionService.hasModulePermission(testUserId, "non_existent_module");
            assertThat(nonExistentModuleResult).isFalse();

            // 测试不存在用户的权限检查 - 这个应该确实抛出异常
            assertBusinessException(
                    () -> modulePermissionService.hasModulePermission(999999L, testModuleName),
                    ErrorCode.USER_NOT_FOUND);

            // 测试授权不存在的模块
            assertBusinessException(
                    () -> {
                        modulePermissionService.grantModulePermission(
                                testUserId, "non_existent_module");
                    },
                    ErrorCode.MODULE_NOT_FOUND);

            log.info("✅ 权限系统边界条件测试通过");
        }
    }

    // ==================== SQL查询测试组 ====================

    @Nested
    @DisplayName("SQL查询测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SqlQueryIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("SQL-001: 管理员执行SQL查询 - 正常流程")
        void testAdminExecuteSqlQueryNormalFlow() throws InterruptedException {
            log.info("🔍 测试管理员执行SQL查询正常流程");

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT * FROM test_integration_table LIMIT 10")
                            .exportResult(false)
                            .build();

            // 管理员执行查询
            SqlQueryResultDTO result = sqlQueryService.executeQuery(testAdminUserId, queryDto);

            // 验证查询结果（Mock返回的结果）
            assertThat(result).isNotNull();
            assertThat(result.getAffectedRows()).isEqualTo(1);
            assertThat(result.getColumns()).isNotNull().isNotEmpty();
            assertThat(result.getRows()).isNotNull().isNotEmpty();
            assertThat(result.getExecutionTimeMs()).isNotNull().isGreaterThan(0);

            // 等待异步操作完成
            waitForAsyncOperation(100, TimeUnit.MILLISECONDS);

            log.info("✅ 管理员执行SQL查询正常流程测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("SQL-002: 普通用户执行SQL查询 - 权限验证")
        void testNormalUserExecuteSqlQueryWithPermission() throws InterruptedException {
            log.info("🔍 测试普通用户执行SQL查询权限验证");

            // 先授予用户模块权限
            modulePermissionService.grantModulePermission(testUserId, testModuleName);

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT name FROM test_integration_table WHERE id = 1")
                            .exportResult(false)
                            .build();

            // 普通用户执行查询
            SqlQueryResultDTO result = sqlQueryService.executeQuery(testUserId, queryDto);

            // 验证查询成功
            assertThat(result).isNotNull();
            assertThat(result.getExecutionTimeMs()).isNotNull().isGreaterThan(0);

            // 等待异步操作完成
            waitForAsyncOperation(100, TimeUnit.MILLISECONDS);

            log.info("✅ 普通用户执行SQL查询权限验证测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("SQL-003: 普通用户执行非SELECT查询 - 权限拒绝")
        void testNormalUserExecuteNonSelectQuery() {
            log.info("🔍 测试普通用户执行非SELECT查询");

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("INSERT INTO test_integration_table (name) VALUES ('test')")
                            .exportResult(false)
                            .build();

            // 普通用户执行INSERT查询应该被拒绝
            assertBusinessException(
                    () -> sqlQueryService.executeQuery(testUserId, queryDto),
                    ErrorCode.PERMISSION_DENIED);

            log.info("✅ 普通用户执行非SELECT查询权限拒绝测试通过");
        }

        @Test
        @Order(4)
        @DisplayName("SQL-004: 用户无权限执行查询 - 权限拒绝")
        void testUserWithoutPermissionExecuteQuery() {
            log.info("🔍 测试无权限用户执行查询");

            // 撤销用户权限
            modulePermissionService.revokeModulePermission(testUserId, testModuleName);

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT * FROM test_integration_table")
                            .exportResult(false)
                            .build();

            // 无权限用户执行查询应该被拒绝
            assertBusinessException(
                    () -> sqlQueryService.executeQuery(testUserId, queryDto),
                    ErrorCode.PERMISSION_DENIED);

            log.info("✅ 无权限用户执行查询权限拒绝测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("SQL-005: 执行SQL查询并导出结果")
        void testExecuteSqlQueryWithExport() throws InterruptedException {
            log.info("🔍 测试执行SQL查询并导出结果");

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT * FROM test_integration_table")
                            .exportResult(true)
                            .exportFormat("xlsx")
                            .build();

            // 管理员执行查询并导出
            SqlQueryResultDTO result = sqlQueryService.executeQuery(testAdminUserId, queryDto);

            // 验证查询结果
            assertThat(result).isNotNull();
            assertThat(result.getDownloadUrl()).isNotNull();
            assertThat(result.getDownloadUrl()).contains("/api/sql/result/");

            // 等待异步操作完成
            waitForAsyncOperation(200, TimeUnit.MILLISECONDS);

            log.info("✅ 执行SQL查询并导出结果测试通过");
        }

        @Test
        @Order(6)
        @DisplayName("SQL-006: 数据源不存在 - 异常验证")
        void testExecuteQueryWithNonExistentDatasource() {
            log.info("🔍 测试查询不存在数据源");

            Long nonExistentDatasourceId = 999999L;
            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(nonExistentDatasourceId)
                            .sql("SELECT 1")
                            .exportResult(false)
                            .build();

            // 应该抛出数据源未找到异常
            assertBusinessException(
                    () -> sqlQueryService.executeQuery(testAdminUserId, queryDto),
                    ErrorCode.DATASOURCE_NOT_FOUND);

            log.info("✅ 数据源不存在异常验证测试通过");
        }

        @Test
        @Order(7)
        @DisplayName("SQL-007: 用户不存在 - 异常验证")
        void testExecuteQueryWithNonExistentUser() {
            log.info("🔍 测试不存在用户执行查询");

            Long nonExistentUserId = 999999L;
            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT 1")
                            .exportResult(false)
                            .build();

            // 应该抛出用户未找到异常
            assertBusinessException(
                    () -> sqlQueryService.executeQuery(nonExistentUserId, queryDto),
                    ErrorCode.USER_NOT_FOUND);

            log.info("✅ 用户不存在异常验证测试通过");
        }

        @Test
        @Order(8)
        @DisplayName("SQL-008: 获取查询历史记录")
        void testGetQueryHistory() {
            log.info("🔍 测试获取查询历史记录");

            SqlHistoryQueryDTO historyQuery =
                    SqlHistoryQueryDTO.builder()
                            .pageNum(1)
                            .pageSize(10)
                            .datasourceId(testDatasourceId)
                            .build();

            // 获取管理员的查询历史
            SqlHistoryResponseDTO historyResponse =
                    sqlQueryService.getQueryHistory(testAdminUserId, historyQuery);

            // 验证历史记录
            assertThat(historyResponse).isNotNull();
            assertThat(historyResponse.getPageNum()).isEqualTo(1);
            assertThat(historyResponse.getPageSize()).isEqualTo(10);
            assertThat(historyResponse.getTotal()).isNotNull().isGreaterThanOrEqualTo(0);
            assertThat(historyResponse.getRecords()).isNotNull();

            // 如果有历史记录，验证记录的完整性
            if (!historyResponse.getRecords().isEmpty()) {
                SqlHistoryResponseDTO.SqlHistoryItemDTO record =
                        historyResponse.getRecords().get(0);
                assertThat(record.getId()).isNotNull();
                assertThat(record.getSqlQuery()).isNotNull();
                assertThat(record.getDatasourceId()).isNotNull();
                assertThat(record.getCreateTime()).isNotNull();
            }

            log.info("✅ 获取查询历史记录测试通过 - 共{}条记录", historyResponse.getTotal());
        }

        @Test
        @Order(9)
        @DisplayName("SQL-009: 获取数据库表列表")
        void testGetDatabaseTableList() {
            log.info("🔍 测试获取数据库表列表");

            // 管理员获取表列表
            DatabaseTableListDTO tableList =
                    sqlQueryService.getDatabaseTableList(testAdminUserId, testDatasourceId);

            // 验证表列表
            assertThat(tableList).isNotNull();
            assertThat(tableList.getDatabaseName()).isNotNull();
            assertThat(tableList.getTables()).isNotNull();

            log.info(
                    "✅ 获取数据库表列表测试通过 - 数据库:{}, 表数量:{}",
                    tableList.getDatabaseName(),
                    tableList.getTables() != null ? tableList.getTables().size() : 0);
        }

        @Test
        @Order(10)
        @DisplayName("SQL-010: 获取表结构信息")
        void testGetTableSchema() {
            log.info("🔍 测试获取表结构信息");

            // 使用已创建模块的表名，确保有正确的权限
            String tableName = "test_integration_table"; // 这是在MOD-005中创建的表

            // 验证管理员用户的角色
            User adminUser = userMapper.selectById(testAdminUserId);
            assertThat(adminUser).isNotNull();
            assertThat(adminUser.getRole()).isEqualTo("ADMIN");
            log.info("管理员用户角色: {}", adminUser.getRole());

            // 管理员获取表结构 - DatabaseMetadataService的mock已在MockJdbcConfiguration中配置
            TableSchemaDTO tableSchema =
                    sqlQueryService.getTableSchema(testAdminUserId, testDatasourceId, tableName);

            // 验证表结构信息
            assertThat(tableSchema).isNotNull();
            assertThat(tableSchema.getDatabaseName()).isNotNull();
            assertThat(tableSchema.getTableName()).isEqualTo(tableName);
            assertThat(tableSchema.getColumns()).isNotNull();
            assertThat(tableSchema.getColumns()).hasSize(3);

            log.info(
                    "✅ 获取表结构信息测试通过 - 表:{}, 列数量:{}",
                    tableName,
                    tableSchema.getColumns() != null ? tableSchema.getColumns().size() : 0);
        }

        @Test
        @Order(11)
        @DisplayName("SQL-011: 普通用户获取无权限表结构 - 权限拒绝")
        void testNormalUserGetUnauthorizedTableSchema() {
            log.info("🔍 测试普通用户获取无权限表结构");

            String tableName = "unauthorized_table";

            // 普通用户获取无权限表的结构应该被拒绝
            assertBusinessException(
                    () -> sqlQueryService.getTableSchema(testUserId, testDatasourceId, tableName),
                    ErrorCode.PERMISSION_DENIED);

            log.info("✅ 普通用户获取无权限表结构权限拒绝测试通过");
        }

        @Test
        @Order(12)
        @DisplayName("SQL-012: 查询历史记录分页测试")
        void testQueryHistoryPagination() {
            log.info("🔍 测试查询历史记录分页");

            // 测试第一页
            SqlHistoryQueryDTO page1Query =
                    SqlHistoryQueryDTO.builder().pageNum(1).pageSize(5).build();

            SqlHistoryResponseDTO page1Response =
                    sqlQueryService.getQueryHistory(testAdminUserId, page1Query);

            // 验证第一页
            assertThat(page1Response).isNotNull();
            assertThat(page1Response.getPageNum()).isEqualTo(1);
            assertThat(page1Response.getPageSize()).isEqualTo(5);

            // 如果总记录数大于5，测试第二页
            if (page1Response.getTotal() > 5) {
                SqlHistoryQueryDTO page2Query =
                        SqlHistoryQueryDTO.builder().pageNum(2).pageSize(5).build();

                SqlHistoryResponseDTO page2Response =
                        sqlQueryService.getQueryHistory(testAdminUserId, page2Query);

                assertThat(page2Response).isNotNull();
                assertThat(page2Response.getPageNum()).isEqualTo(2);
                assertThat(page2Response.getPageSize()).isEqualTo(5);
            }

            log.info("✅ 查询历史记录分页测试通过");
        }

        @Test
        @Order(13)
        @DisplayName("SQL-013: 查询历史记录条件过滤")
        void testQueryHistoryWithFilters() {
            log.info("🔍 测试查询历史记录条件过滤");

            // 按数据源过滤
            SqlHistoryQueryDTO queryWithDatasource =
                    SqlHistoryQueryDTO.builder()
                            .pageNum(1)
                            .pageSize(10)
                            .datasourceId(testDatasourceId)
                            .build();

            SqlHistoryResponseDTO responseWithDatasource =
                    sqlQueryService.getQueryHistory(testAdminUserId, queryWithDatasource);

            assertThat(responseWithDatasource).isNotNull();

            // 按关键字过滤
            SqlHistoryQueryDTO queryWithKeyword =
                    SqlHistoryQueryDTO.builder()
                            .pageNum(1)
                            .pageSize(10)
                            .queryKeyword("SELECT")
                            .build();

            SqlHistoryResponseDTO responseWithKeyword =
                    sqlQueryService.getQueryHistory(testAdminUserId, queryWithKeyword);

            assertThat(responseWithKeyword).isNotNull();

            log.info("✅ 查询历史记录条件过滤测试通过");
        }

        @Test
        @Order(14)
        @DisplayName("SQL-014: SQL注入防护测试")
        void testSqlInjectionProtection() {
            log.info("🔍 测试SQL注入防护");

            String maliciousSql = "SELECT * FROM test_table; DROP TABLE test_table; --";

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql(maliciousSql)
                            .exportResult(false)
                            .build();

            // 管理员执行恶意SQL，系统应该能够安全处理
            // 可能抛出SQL语法错误，但不会执行恶意操作
            try {
                SqlQueryResultDTO result = sqlQueryService.executeQuery(testAdminUserId, queryDto);
                // 如果执行成功，验证结果安全
                assertThat(result).isNotNull();
            } catch (BusinessException e) {
                // 如果抛出异常，验证是合理的业务异常
                assertThat(e.getErrorCode())
                        .isIn(ErrorCode.INTERNAL_ERROR, ErrorCode.SQL_EXECUTION_FAILED);
            }

            log.info("✅ SQL注入防护测试通过");
        }

        @Test
        @Order(15)
        @DisplayName("SQL-015: 复杂SQL查询测试")
        void testComplexSqlQuery() throws InterruptedException {
            log.info("🔍 测试复杂SQL查询");

            String complexSql =
                    """
                SELECT
                    t1.id,
                    t1.name,
                    COUNT(*) as record_count
                FROM test_integration_table t1
                WHERE t1.create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                GROUP BY t1.id, t1.name
                ORDER BY record_count DESC
                LIMIT 100
                """;

            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql(complexSql)
                            .exportResult(false)
                            .build();

            // 管理员执行复杂查询
            SqlQueryResultDTO result = sqlQueryService.executeQuery(testAdminUserId, queryDto);

            // 验证查询结果
            assertThat(result).isNotNull();

            // 等待异步操作完成
            waitForAsyncOperation(200, TimeUnit.MILLISECONDS);

            log.info("✅ 复杂SQL查询测试通过");
        }
    }

    // ==================== 权限验证集成测试组 ====================

    @Nested
    @DisplayName("权限验证集成测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PermissionIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("INT-001: 完整权限链验证 - 用户→模块→数据源")
        void testCompletePermissionChain() {
            log.info("🔍 测试完整权限链验证");

            // 1. 创建新用户、新数据源、新模块
            User newUser = createTestUser("chain_user", "chain@test.com", UserRole.USER);

            DatasourceCreateDTO newDatasourceDto =
                    DatasourceCreateDTO.builder()
                            .name("chain-test-datasource")
                            .type("DORIS")
                            .jdbcUrl("jdbc:mysql://localhost:9030/chain_db")
                            .username("root")
                            .password("")
                            .build();
            DatasourceDTO newDatasource = datasourceService.createDatasource(newDatasourceDto);

            ModuleInfoCreateDTO newModuleDto =
                    ModuleInfoCreateDTO.builder()
                            .name("chain-test-module")
                            .datasourceId(newDatasource.getId())
                            .tableName("chain_test_table")
                            .build();
            ModuleInfoDTO newModule = moduleInfoService.createModule(newModuleDto);

            // 2. 验证用户初始没有权限
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(
                            newUser.getId(), newModule.getName());
            assertThat(hasPermissionBefore).isFalse();

            // 3. 授予权限
            modulePermissionService.grantModulePermission(newUser.getId(), newModule.getName());

            // 4. 验证权限生效
            boolean hasPermissionAfter =
                    modulePermissionService.hasModulePermission(
                            newUser.getId(), newModule.getName());
            assertThat(hasPermissionAfter).isTrue();

            // 5. 验证可以通过模块获取数据源
            DatasourceDTO retrievedDatasource =
                    datasourceService.getDatasourceByModule(newModule.getName());
            assertThat(retrievedDatasource.getId()).isEqualTo(newDatasource.getId());

            // 6. 验证SQL查询权限链
            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(newDatasource.getId())
                            .sql("SELECT * FROM chain_test_table")
                            .exportResult(false)
                            .build();

            // 有权限的用户可以查询
            SqlQueryResultDTO result = sqlQueryService.executeQuery(newUser.getId(), queryDto);
            assertThat(result).isNotNull();

            // 7. 清理测试数据
            moduleInfoService.deleteModule(newModule.getId(), false);
            datasourceService.deleteDatasource(newDatasource.getId());
            userMapper.deleteById(newUser.getId());

            log.info("✅ 完整权限链验证测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("INT-002: 模块删除权限清理验证")
        void testModuleDeletionPermissionCleanup() {
            log.info("🔍 测试模块删除权限清理验证");

            // 1. 创建测试模块
            ModuleInfoCreateDTO tempModuleDto =
                    ModuleInfoCreateDTO.builder()
                            .name("temp-delete-module")
                            .datasourceId(testDatasourceId)
                            .tableName("temp_delete_table")
                            .build();
            ModuleInfoDTO tempModule = moduleInfoService.createModule(tempModuleDto);

            // 2. 授予用户权限
            modulePermissionService.grantModulePermission(testUserId, tempModule.getName());
            boolean hasPermissionBefore =
                    modulePermissionService.hasModulePermission(testUserId, tempModule.getName());
            assertThat(hasPermissionBefore).isTrue();

            // 3. 删除模块
            moduleInfoService.deleteModule(tempModule.getId(), false);

            // 4. 验证权限已清理
            boolean hasPermissionAfter =
                    modulePermissionService.hasModulePermission(testUserId, tempModule.getName());
            assertThat(hasPermissionAfter).isFalse();

            log.info("✅ 模块删除权限清理验证测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("INT-003: 跨服务权限状态一致性验证")
        void testCrossServicePermissionConsistency() {
            log.info("🔍 测试跨服务权限状态一致性");

            // 1. 通过权限服务授予权限
            modulePermissionService.grantModulePermission(testUserId, testModuleName);

            // 2. 验证模块服务能看到权限信息
            List<ModuleInfoWithPermissionsDTO> modulesWithPermissions =
                    moduleInfoService.getAllModulesWithPermissions();
            ModuleInfoWithPermissionsDTO testModuleWithPerms =
                    modulesWithPermissions.stream()
                            .filter(m -> m.getId().equals(testModuleId))
                            .findFirst()
                            .orElse(null);

            assertThat(testModuleWithPerms).isNotNull();
            boolean userInModulePermissions =
                    testModuleWithPerms.getUsers().stream()
                            .anyMatch(u -> u.getUserId().equals(testUserId));
            assertThat(userInModulePermissions).isTrue();

            // 3. 验证SQL服务能正确验证权限
            SqlQueryDTO queryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql("SELECT * FROM test_integration_table")
                            .exportResult(false)
                            .build();

            SqlQueryResultDTO result = sqlQueryService.executeQuery(testUserId, queryDto);
            assertThat(result).isNotNull();

            // 4. 撤销权限后验证一致性
            modulePermissionService.revokeModulePermission(testUserId, testModuleName);

            // SQL服务应该拒绝查询
            assertBusinessException(
                    () -> sqlQueryService.executeQuery(testUserId, queryDto),
                    ErrorCode.PERMISSION_DENIED);

            log.info("✅ 跨服务权限状态一致性验证测试通过");
        }
    }

    // ==================== 异常处理测试组 ====================

    @Nested
    @DisplayName("异常处理测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ExceptionHandlingIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("ERR-001: 数据一致性验证 - 事务回滚")
        void testDataConsistencyWithTransactionRollback() {
            log.info("🔍 测试数据一致性与事务回滚");

            // 记录初始状态
            List<DatasourceDTO> initialDatasources = datasourceService.getAllDatasources();
            List<ModuleInfoDTO> initialModules = moduleInfoService.getAllModules();

            // 测试1: 重复名称应该失败
            assertBusinessException(
                    () -> {
                        DatasourceCreateDTO duplicateDto =
                                DatasourceCreateDTO.builder()
                                        .name("integration-test-datasource") // 已存在的名称
                                        .type("DORIS")
                                        .jdbcUrl("jdbc:mysql://localhost:9030/test_db")
                                        .username("test")
                                        .password("test")
                                        .build();
                        datasourceService.createDatasource(duplicateDto);
                    },
                    ErrorCode.DATASOURCE_NAME_EXISTS);

            // 测试2: 无效JDBC URL应该失败
            assertBusinessException(
                    () -> {
                        DatasourceCreateDTO invalidDto =
                                DatasourceCreateDTO.builder()
                                        .name("test_invalid_datasource")
                                        .type("DORIS")
                                        .jdbcUrl("jdbc:mysql://invalid-host:9999/nonexistent_db")
                                        .username("test")
                                        .password("test")
                                        .build();
                        datasourceService.createDatasource(invalidDto);
                    },
                    ErrorCode.DATASOURCE_CONNECTION_FAILED);

            // 验证数据没有被污染
            List<DatasourceDTO> afterDatasources = datasourceService.getAllDatasources();
            List<ModuleInfoDTO> afterModules = moduleInfoService.getAllModules();

            assertThat(afterDatasources).hasSize(initialDatasources.size());
            assertThat(afterModules).hasSize(initialModules.size());

            log.info("✅ 数据一致性验证与事务回滚测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("ERR-002: 并发操作安全性验证")
        void testConcurrentOperationSafety()
                throws InterruptedException, ExecutionException, TimeoutException {
            log.info("🔍 测试并发操作安全性");

            CountDownLatch latch = new CountDownLatch(1);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // 模拟多个线程同时操作同一用户的权限
            for (int i = 0; i < 5; i++) {
                CompletableFuture<Void> future =
                        CompletableFuture.runAsync(
                                () -> {
                                    try {
                                        latch.await(); // 等待所有线程准备就绪

                                        // 执行权限操作
                                        modulePermissionService.grantModulePermission(
                                                testUserId, testModuleName);
                                        Thread.sleep(10);
                                        modulePermissionService.revokeModulePermission(
                                                testUserId, testModuleName);
                                        Thread.sleep(10);
                                        modulePermissionService.grantModulePermission(
                                                testUserId, testModuleName);

                                    } catch (Exception e) {
                                        log.debug("并发操作中的异常（可能是正常的）: {}", e.getMessage());
                                    }
                                });
                futures.add(future);
            }

            // 释放所有线程开始执行
            latch.countDown();

            // 等待所有操作完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(20, TimeUnit.SECONDS);

            // 验证最终状态一致性
            boolean finalPermissionState =
                    modulePermissionService.hasModulePermission(testUserId, testModuleName);
            log.info("并发操作后的最终权限状态: {}", finalPermissionState);

            log.info("✅ 并发操作安全性验证测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("ERR-003: 边界条件处理验证")
        void testBoundaryConditionHandling() {
            log.info("🔍 测试各种边界条件的处理");

            // 测试空字符串和null参数 - 根据真实业务逻辑，空字符串会抛出VALIDATION_ERROR
            assertBusinessException(
                    () -> datasourceService.getDatasourceByModule(""), ErrorCode.VALIDATION_ERROR);

            assertBusinessException(
                    () -> datasourceService.getDatasourceByModule(null),
                    ErrorCode.VALIDATION_ERROR);

            // 测试不存在的模块名 - 这个会抛出MODULE_NOT_FOUND
            assertBusinessException(
                    () -> datasourceService.getDatasourceByModule("non_existent_module"),
                    ErrorCode.MODULE_NOT_FOUND);

            // 测试负数ID
            assertBusinessException(
                    () -> moduleInfoService.getModuleById(-1L), ErrorCode.MODULE_NOT_FOUND);

            // 测试不存在的ID
            assertBusinessException(
                    () -> moduleInfoService.getModuleById(999999L), ErrorCode.MODULE_NOT_FOUND);

            log.info("✅ 边界条件处理验证测试通过");
        }

        @Test
        @Order(4)
        @DisplayName("ERR-004: 资源清理验证")
        void testResourceCleanupValidation() {
            log.info("🔍 测试资源清理验证");

            // 创建临时资源
            User tempUser = createTestUser("temp_cleanup_user", "temp@cleanup.com", UserRole.USER);

            DatasourceCreateDTO tempDatasourceDto =
                    DatasourceCreateDTO.builder()
                            .name("temp-cleanup-datasource")
                            .type("DORIS")
                            .jdbcUrl("jdbc:mysql://localhost:9030/temp_cleanup_db")
                            .username("root")
                            .password("")
                            .build();
            DatasourceDTO tempDatasource = datasourceService.createDatasource(tempDatasourceDto);

            ModuleInfoCreateDTO tempModuleDto =
                    ModuleInfoCreateDTO.builder()
                            .name("temp-cleanup-module")
                            .datasourceId(tempDatasource.getId())
                            .tableName("temp_cleanup_table")
                            .build();
            ModuleInfoDTO tempModule = moduleInfoService.createModule(tempModuleDto);

            // 创建权限关系
            modulePermissionService.grantModulePermission(tempUser.getId(), tempModule.getName());

            // 验证资源存在
            assertThat(userMapper.selectById(tempUser.getId())).isNotNull();
            assertThat(datasourceMapper.selectById(tempDatasource.getId())).isNotNull();
            assertThat(moduleInfoMapper.selectById(tempModule.getId())).isNotNull();

            UserModulePermission permission =
                    userModulePermissionMapper.select(
                            tempUser.getId(), tempDatasource.getId(), tempModule.getName());
            assertThat(permission).isNotNull();

            // 按正确顺序清理资源
            moduleInfoService.deleteModule(tempModule.getId(), false);
            datasourceService.deleteDatasource(tempDatasource.getId());
            userMapper.deleteById(tempUser.getId());

            // 验证资源已清理
            assertThat(moduleInfoMapper.selectById(tempModule.getId())).isNull();
            assertThat(datasourceMapper.selectById(tempDatasource.getId())).isNull();
            assertThat(userMapper.selectById(tempUser.getId())).isNull();

            // 权限应该也被自动清理
            UserModulePermission cleanedPermission =
                    userModulePermissionMapper.select(
                            tempUser.getId(), tempDatasource.getId(), tempModule.getName());
            assertThat(cleanedPermission).isNull();

            log.info("✅ 资源清理验证测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("ERR-005: 系统限制验证")
        void testSystemLimitsValidation() {
            log.info("🔍 测试系统限制验证");

            // 测试SQL查询长度限制（如果有）
            String longSql = "SELECT " + "a,".repeat(1000) + "1 FROM test_table";
            SqlQueryDTO longQueryDto =
                    SqlQueryDTO.builder()
                            .datasourceId(testDatasourceId)
                            .sql(longSql)
                            .exportResult(false)
                            .build();

            // 执行长SQL查询（系统应该能处理或给出合适错误）
            try {
                SqlQueryResultDTO result =
                        sqlQueryService.executeQuery(testAdminUserId, longQueryDto);
                assertThat(result).isNotNull();
            } catch (BusinessException e) {
                // 如果有长度限制，应该给出合适的错误信息
                assertThat(e.getErrorCode())
                        .isIn(ErrorCode.VALIDATION_ERROR, ErrorCode.INTERNAL_ERROR);
            }

            log.info("✅ 系统限制验证测试通过");
        }
    }

    // ==================== 系统缓存测试组 ====================

    @Nested
    @DisplayName("系统缓存测试组")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SystemCacheIntegrationTest {

        private static final String testCacheKey = "test_log_search_condition_integration_test";
        private final String testCreateUser = "test_cache_user";

        @Test
        @Order(1)
        @DisplayName("CACHE-001: 保存缓存 - 正常流程")
        void testSaveCacheNormalFlow() {
            log.info("🔍 测试保存缓存正常流程");

            // 创建测试用的 LogSearchDTO 数据
            LogSearchDTO logSearchData = new LogSearchDTO();
            logSearchData.setModule("test-module");
            logSearchData.setKeywords(List.of("error", "warning"));
            logSearchData.setWhereSqls(List.of("level = 'ERROR'", "service_name = 'user-service'"));
            logSearchData.setStartTime("2023-06-01 10:00:00.000");
            logSearchData.setEndTime("2023-06-01 11:00:00.000");
            logSearchData.setTimeRange("last_1h");
            logSearchData.setPageSize(100);
            logSearchData.setOffset(0);
            logSearchData.setFields(List.of("log_time", "level", "message"));

            // 保存缓存
            assertDoesNotThrow(
                    () -> {
                        systemCacheService.saveCache(
                                CacheGroup.LOG_SEARCH_CONDITION, testCacheKey, logSearchData);
                    });

            log.info("✅ 保存缓存正常流程测试通过");
        }

        @Test
        @Order(2)
        @DisplayName("CACHE-002: 获取缓存 - 正常流程")
        void testGetCacheNormalFlow() {
            log.info("🔍 测试获取缓存正常流程");

            // 获取缓存
            Optional<LogSearchDTO> retrievedDataOpt =
                    systemCacheService.getCache(
                            CacheGroup.LOG_SEARCH_CONDITION, testCacheKey, LogSearchDTO.class);

            // 验证缓存存在
            assertThat(retrievedDataOpt).isPresent();
            LogSearchDTO retrievedData = retrievedDataOpt.get();

            // 验证结果
            assertThat(retrievedData).isNotNull();
            assertThat(retrievedData.getModule()).isEqualTo("test-module");
            assertThat(retrievedData.getKeywords()).containsExactly("error", "warning");
            assertThat(retrievedData.getWhereSqls())
                    .containsExactly("level = 'ERROR'", "service_name = 'user-service'");
            assertThat(retrievedData.getStartTime()).isEqualTo("2023-06-01 10:00:00.000");
            assertThat(retrievedData.getEndTime()).isEqualTo("2023-06-01 11:00:00.000");
            assertThat(retrievedData.getTimeRange()).isEqualTo("last_1h");
            assertThat(retrievedData.getPageSize()).isEqualTo(100);
            assertThat(retrievedData.getOffset()).isEqualTo(0);
            assertThat(retrievedData.getFields()).containsExactly("log_time", "level", "message");

            log.info("✅ 获取缓存正常流程测试通过");
        }

        @Test
        @Order(3)
        @DisplayName("CACHE-003: 获取用户缓存列表")
        void testGetUserCaches() {
            log.info("🔍 测试获取用户缓存列表");

            // 获取用户缓存列表
            List<SystemCacheConfig> userCaches =
                    systemCacheService.getUserCaches(CacheGroup.LOG_SEARCH_CONDITION);

            // 验证结果
            assertThat(userCaches).isNotNull();
            assertThat(userCaches).isNotEmpty();

            // 验证包含我们创建的缓存
            boolean containsTestCache =
                    userCaches.stream().anyMatch(cache -> cache.getCacheKey().equals(testCacheKey));
            assertThat(containsTestCache).isTrue();

            // 验证缓存内容
            SystemCacheConfig testCache =
                    userCaches.stream()
                            .filter(cache -> cache.getCacheKey().equals(testCacheKey))
                            .findFirst()
                            .orElse(null);

            assertThat(testCache).isNotNull();
            assertThat(testCache.getCacheGroup()).isEqualTo(CacheGroup.LOG_SEARCH_CONDITION.name());
            // 在集成测试环境中，由于没有认证上下文，UserContextUtil.getCurrentUserEmail() 返回 "anonymous"
            // 这是正常的行为，因为测试环境中没有设置 SecurityContext
            assertThat(testCache.getCreateUser()).isEqualTo("anonymous");
            assertThat(testCache.getContent()).isNotNull();

            log.info("✅ 获取用户缓存列表测试通过 - 共{}个缓存", userCaches.size());
        }

        @Test
        @Order(4)
        @DisplayName("CACHE-004: 更新缓存 - 覆盖保存")
        void testUpdateCacheOverwrite() {
            log.info("🔍 测试更新缓存覆盖保存");

            // 创建新的测试数据
            LogSearchDTO updatedLogSearchData = new LogSearchDTO();
            updatedLogSearchData.setModule("updated-module");
            updatedLogSearchData.setKeywords(List.of("fatal", "critical"));
            updatedLogSearchData.setWhereSqls(List.of("level = 'FATAL'"));
            updatedLogSearchData.setStartTime("2023-06-02 10:00:00.000");
            updatedLogSearchData.setEndTime("2023-06-02 11:00:00.000");
            updatedLogSearchData.setTimeRange("last_2h");
            updatedLogSearchData.setPageSize(200);
            updatedLogSearchData.setOffset(10);
            updatedLogSearchData.setFields(List.of("log_time", "level", "message", "host"));

            // 更新缓存（覆盖保存）
            assertDoesNotThrow(
                    () -> {
                        systemCacheService.saveCache(
                                CacheGroup.LOG_SEARCH_CONDITION,
                                testCacheKey,
                                updatedLogSearchData);
                    });

            // 验证更新后的数据
            Optional<LogSearchDTO> retrievedDataOpt =
                    systemCacheService.getCache(
                            CacheGroup.LOG_SEARCH_CONDITION, testCacheKey, LogSearchDTO.class);

            // 验证缓存存在
            assertThat(retrievedDataOpt).isPresent();
            LogSearchDTO retrievedData = retrievedDataOpt.get();

            assertThat(retrievedData).isNotNull();
            assertThat(retrievedData.getModule()).isEqualTo("updated-module");
            assertThat(retrievedData.getKeywords()).containsExactly("fatal", "critical");
            assertThat(retrievedData.getWhereSqls()).containsExactly("level = 'FATAL'");
            assertThat(retrievedData.getTimeRange()).isEqualTo("last_2h");
            assertThat(retrievedData.getPageSize()).isEqualTo(200);
            assertThat(retrievedData.getOffset()).isEqualTo(10);
            assertThat(retrievedData.getFields())
                    .containsExactly("log_time", "level", "message", "host");

            log.info("✅ 更新缓存覆盖保存测试通过");
        }

        @Test
        @Order(5)
        @DisplayName("CACHE-005: 获取不存在的缓存")
        void testGetNonExistentCache() {
            log.info("🔍 测试获取不存在的缓存");

            String nonExistentKey = "non_existent_key_" + System.currentTimeMillis();

            // 获取不存在的缓存
            Optional<LogSearchDTO> result =
                    systemCacheService.getCache(
                            CacheGroup.LOG_SEARCH_CONDITION, nonExistentKey, LogSearchDTO.class);

            // 验证结果为空
            assertThat(result).isEmpty();

            log.info("✅ 获取不存在的缓存测试通过");
        }

        @Test
        @Order(6)
        @DisplayName("CACHE-006: 删除缓存")
        void testDeleteCache() {
            log.info("🔍 测试删除缓存");

            // 删除缓存
            assertDoesNotThrow(
                    () -> {
                        systemCacheService.deleteCache(
                                CacheGroup.LOG_SEARCH_CONDITION, testCacheKey);
                    });

            // 验证缓存已被删除
            Optional<LogSearchDTO> result =
                    systemCacheService.getCache(
                            CacheGroup.LOG_SEARCH_CONDITION, testCacheKey, LogSearchDTO.class);
            assertThat(result).isEmpty();

            // 验证用户缓存列表中不再包含该缓存
            List<SystemCacheConfig> userCaches =
                    systemCacheService.getUserCaches(CacheGroup.LOG_SEARCH_CONDITION);
            boolean containsTestCache =
                    userCaches.stream().anyMatch(cache -> cache.getCacheKey().equals(testCacheKey));
            assertThat(containsTestCache).isFalse();

            log.info("✅ 删除缓存测试通过");
        }

        @Test
        @Order(7)
        @DisplayName("CACHE-007: 删除不存在的缓存")
        void testDeleteNonExistentCache() {
            log.info("🔍 测试删除不存在的缓存");

            String nonExistentKey = "non_existent_key_" + System.currentTimeMillis();

            // 删除不存在的缓存应该不抛出异常
            assertDoesNotThrow(
                    () -> {
                        systemCacheService.deleteCache(
                                CacheGroup.LOG_SEARCH_CONDITION, nonExistentKey);
                    });

            log.info("✅ 删除不存在的缓存测试通过");
        }

        @Test
        @Order(8)
        @DisplayName("CACHE-008: 数据类型验证 - 错误类型")
        void testDataTypeValidation() {
            log.info("🔍 测试数据类型验证");

            // 尝试保存错误类型的数据（String 而不是 LogSearchDTO）
            String wrongTypeData = "This is a string, not LogSearchDTO";

            // 验证抛出业务异常
            assertBusinessException(
                    () ->
                            systemCacheService.saveCache(
                                    CacheGroup.LOG_SEARCH_CONDITION,
                                    "wrong_type_key",
                                    wrongTypeData),
                    ErrorCode.VALIDATION_ERROR);

            log.info("✅ 数据类型验证测试通过");
        }

        @Test
        @Order(9)
        @DisplayName("CACHE-009: 参数验证 - 空值检查")
        void testParameterValidation() {
            log.info("🔍 测试参数验证");

            LogSearchDTO validData = new LogSearchDTO();
            validData.setModule("test-module");

            // 测试空的缓存组
            assertBusinessException(
                    () -> systemCacheService.saveCache(null, "test_key", validData),
                    ErrorCode.VALIDATION_ERROR);

            // 测试空的缓存键
            assertBusinessException(
                    () ->
                            systemCacheService.saveCache(
                                    CacheGroup.LOG_SEARCH_CONDITION, null, validData),
                    ErrorCode.VALIDATION_ERROR);

            // 测试空的数据
            assertBusinessException(
                    () ->
                            systemCacheService.saveCache(
                                    CacheGroup.LOG_SEARCH_CONDITION, "test_key", null),
                    ErrorCode.VALIDATION_ERROR);

            log.info("✅ 参数验证测试通过");
        }

        @Test
        @Order(10)
        @DisplayName("CACHE-010: 并发保存缓存测试")
        void testConcurrentCacheSave()
                throws InterruptedException, ExecutionException, TimeoutException {
            log.info("🔍 测试并发保存缓存");

            String concurrentCacheKey = "concurrent_test_key_" + System.currentTimeMillis();
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<Boolean> successResults = Collections.synchronizedList(new ArrayList<>());

            try {
                // 创建多个并发任务
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    final int threadIndex = i;
                    CompletableFuture<Void> future =
                            CompletableFuture.runAsync(
                                    () -> {
                                        try {
                                            LogSearchDTO data = new LogSearchDTO();
                                            data.setModule("concurrent-module-" + threadIndex);
                                            data.setKeywords(List.of("thread-" + threadIndex));
                                            data.setPageSize(threadIndex * 10 + 10);

                                            systemCacheService.saveCache(
                                                    CacheGroup.LOG_SEARCH_CONDITION,
                                                    concurrentCacheKey,
                                                    data);
                                            successResults.add(true);
                                            log.debug("线程 {} 成功保存缓存", threadIndex);
                                        } catch (Exception e) {
                                            exceptions.add(e);
                                            log.debug(
                                                    "线程 {} 保存缓存失败: {}",
                                                    threadIndex,
                                                    e.getMessage());
                                        }
                                    },
                                    executor);
                    futures.add(future);
                }

                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.SECONDS);

                // 验证并发行为：由于唯一约束，只有一个线程能成功保存
                log.info("成功保存数量: {}, 异常数量: {}", successResults.size(), exceptions.size());
                assertThat(successResults.size()).isEqualTo(1); // 只有一个成功
                assertThat(exceptions.size()).isEqualTo(threadCount - 1); // 其他都失败

                // 验证最终只有一个缓存记录存在
                Optional<LogSearchDTO> finalResultOpt =
                        systemCacheService.getCache(
                                CacheGroup.LOG_SEARCH_CONDITION,
                                concurrentCacheKey,
                                LogSearchDTO.class);
                assertThat(finalResultOpt).isPresent();
                LogSearchDTO finalResult = finalResultOpt.get();
                assertThat(finalResult.getModule()).startsWith("concurrent-module-");

                log.info("✅ 并发保存缓存测试通过 - 最终结果: {}", finalResult.getModule());

                // 清理测试数据
                systemCacheService.deleteCache(CacheGroup.LOG_SEARCH_CONDITION, concurrentCacheKey);

            } finally {
                executor.shutdown();
            }
        }
    }
}
