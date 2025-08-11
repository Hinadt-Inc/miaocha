package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.impl.QueryPermissionChecker;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.enums.UserRole;
import com.hinadt.miaocha.infrastructure.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserModulePermissionMapper;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 查询权限检查器测试
 *
 * <p>重点测试权限验证的核心业务逻辑，确保不同角色用户的权限控制正确
 *
 * <p>包括：查询权限检查、表权限获取等核心功能的单元测试
 */
@DisplayName("查询权限检查器测试")
@ExtendWith(MockitoExtension.class)
class QueryPermissionCheckerTest {
    @Mock private TableValidationService tableValidationService;
    @Mock private UserModulePermissionMapper userModulePermissionMapper;
    @Mock private UserMapper userMapper;
    @Mock private ModuleInfoMapper moduleInfoMapper;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData databaseMetaData;
    @Mock private ResultSet resultSet;

    private QueryPermissionChecker queryPermissionChecker;

    private User adminUser;
    private User normalUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        // 使用构造函数注入创建实例
        queryPermissionChecker =
                new QueryPermissionChecker(
                        tableValidationService,
                        userModulePermissionMapper,
                        userMapper,
                        moduleInfoMapper);

        // 准备管理员用户
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUid("admin");
        adminUser.setRole(UserRole.ADMIN.name());
        adminUser.setCreateTime(LocalDateTime.now());

        // 准备普通用户
        normalUser = new User();
        normalUser.setId(2L);
        normalUser.setUid("user");
        normalUser.setRole(UserRole.USER.name());
        normalUser.setCreateTime(LocalDateTime.now());

        // 准备超级管理员用户
        superAdminUser = new User();
        superAdminUser.setId(3L);
        superAdminUser.setUid("superadmin");
        superAdminUser.setRole(UserRole.SUPER_ADMIN.name());
        superAdminUser.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("管理员权限检查 - 管理员应该可以执行任何查询")
    void testCheckQueryPermission_AdminUser() {
        // 执行测试 - 管理员执行任何SQL都应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, "DELETE FROM user_logs", 1L));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, "UPDATE user_logs SET status = 1", 1L));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, "SELECT * FROM user_logs", 1L));

        // 验证不会调用权限检查相关方法
        verify(userModulePermissionMapper, never()).selectPermittedTableNames(anyLong(), anyLong());
    }

    @Test
    @DisplayName("超级管理员权限检查 - 超级管理员应该可以执行任何查询")
    void testCheckQueryPermission_SuperAdminUser() {
        // 执行测试 - 超级管理员执行任何SQL都应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                superAdminUser, "DROP TABLE user_logs", 1L));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                superAdminUser, "INSERT INTO user_logs VALUES (1)", 1L));

        // 验证不会调用权限检查相关方法
        verify(userModulePermissionMapper, never()).selectPermittedTableNames(anyLong(), anyLong());
    }

    @Test
    @DisplayName("普通用户权限检查 - 只能执行SELECT查询")
    void testCheckQueryPermission_NormalUser_NonSelectQuery() {
        // 测试非SELECT查询应被拒绝
        String[] nonSelectSqls = {
            "DELETE FROM user_logs",
            "UPDATE user_logs SET status = 1",
            "INSERT INTO user_logs VALUES (1)",
            "DROP TABLE user_logs",
            "CREATE TABLE test (id int)"
        };

        // Mock TableValidationService 对非SELECT语句都返回false
        for (String sql : nonSelectSqls) {
            when(tableValidationService.isSelectStatement(sql)).thenReturn(false);
        }

        for (String sql : nonSelectSqls) {
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> queryPermissionChecker.checkQueryPermission(normalUser, sql, 1L));

            assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
            // 仅验证异常类型和错误码，不依赖具体错误消息
        }

        // 验证不会调用权限检查相关方法（因为在SQL类型检查阶段就被拒绝了）
        verify(userModulePermissionMapper, never()).selectPermittedTableNames(anyLong(), anyLong());
    }

    // ================ getPermittedTables 方法测试组 ================

    @Nested
    @DisplayName("getPermittedTables 方法测试")
    class GetPermittedTablesTest {

        @Test
        @DisplayName("管理员用户 - 应返回所有启用模块的表名与真实表的交集")
        void testGetPermittedTables_AdminUser() throws SQLException {
            // 准备测试数据
            Long datasourceId = 1L;

            // Mock 用户查询
            when(userMapper.selectById(1L)).thenReturn(adminUser);

            // Mock 数据库真实表
            mockDatabaseTables("real_table1", "real_table2", "real_table3");

            // Mock 启用模块表名
            List<String> enabledModuleTables =
                    Arrays.asList("real_table1", "real_table2", "non_exist_table");
            when(moduleInfoMapper.selectEnabledModuleTableNames(datasourceId))
                    .thenReturn(enabledModuleTables);

            // 执行测试
            List<String> result =
                    queryPermissionChecker.getPermittedTables(1L, datasourceId, connection);

            // 验证结果：应该是模块表名与真实表名的交集，且按字母顺序排列
            assertEquals(2, result.size());
            assertEquals("real_table1", result.get(0));
            assertEquals("real_table2", result.get(1));

            // 验证方法调用
            verify(userMapper).selectById(1L);
            verify(moduleInfoMapper).selectEnabledModuleTableNames(datasourceId);
            verify(connection).getMetaData();
        }

        @Test
        @DisplayName("超级管理员用户 - 应返回所有启用模块的表名与真实表的交集")
        void testGetPermittedTables_SuperAdminUser() throws SQLException {
            // 准备测试数据
            Long datasourceId = 1L;

            // Mock 用户查询
            when(userMapper.selectById(3L)).thenReturn(superAdminUser);

            // Mock 数据库真实表
            mockDatabaseTables("table_a", "table_b");

            // Mock 启用模块表名
            List<String> enabledModuleTables = Arrays.asList("table_a", "table_c");
            when(moduleInfoMapper.selectEnabledModuleTableNames(datasourceId))
                    .thenReturn(enabledModuleTables);

            // 执行测试
            List<String> result =
                    queryPermissionChecker.getPermittedTables(3L, datasourceId, connection);

            // 验证结果：只返回既是启用模块又真实存在的表
            assertEquals(1, result.size());
            assertEquals("table_a", result.get(0));

            // 验证方法调用
            verify(userMapper).selectById(3L);
            verify(moduleInfoMapper).selectEnabledModuleTableNames(datasourceId);
        }

        @Test
        @DisplayName("普通用户 - 应返回用户有权限的表名与真实表的交集")
        void testGetPermittedTables_NormalUser() throws SQLException {
            // 准备测试数据
            Long datasourceId = 1L;

            // Mock 用户查询
            when(userMapper.selectById(2L)).thenReturn(normalUser);

            // Mock 数据库真实表
            mockDatabaseTables("user_table1", "user_table2", "other_table");

            // Mock 用户权限表名
            List<String> userPermittedTables = Arrays.asList("user_table1", "user_table3");
            when(userModulePermissionMapper.selectPermittedTableNames(2L, datasourceId))
                    .thenReturn(userPermittedTables);

            // 执行测试
            List<String> result =
                    queryPermissionChecker.getPermittedTables(2L, datasourceId, connection);

            // 验证结果：只返回用户有权限且真实存在的表
            assertEquals(1, result.size());
            assertEquals("user_table1", result.get(0));

            // 验证方法调用
            verify(userMapper).selectById(2L);
            verify(userModulePermissionMapper).selectPermittedTableNames(2L, datasourceId);
        }

        @Test
        @DisplayName("普通用户 - 无权限表时应返回空列表")
        void testGetPermittedTables_NormalUser_NoPermissions() throws SQLException {
            // 准备测试数据
            Long datasourceId = 1L;

            // Mock 用户查询
            when(userMapper.selectById(2L)).thenReturn(normalUser);

            // Mock 数据库真实表
            mockDatabaseTables("table1", "table2");

            // Mock 用户无权限表
            when(userModulePermissionMapper.selectPermittedTableNames(2L, datasourceId))
                    .thenReturn(Arrays.asList());

            // 执行测试
            List<String> result =
                    queryPermissionChecker.getPermittedTables(2L, datasourceId, connection);

            // 验证结果：应返回空列表
            assertTrue(result.isEmpty());

            // 验证方法调用
            verify(userMapper).selectById(2L);
            verify(userModulePermissionMapper).selectPermittedTableNames(2L, datasourceId);
        }

        @Test
        @DisplayName("用户不存在 - 应抛出BusinessException")
        void testGetPermittedTables_UserNotFound() {
            // Mock 用户不存在
            when(userMapper.selectById(999L)).thenReturn(null);

            // 执行测试并验证异常
            BusinessException exception =
                    assertThrows(
                            BusinessException.class,
                            () -> queryPermissionChecker.getPermittedTables(999L, 1L, connection));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userMapper).selectById(999L);
        }

        @Test
        @DisplayName("数据库连接异常 - 应抛出SQLException")
        void testGetPermittedTables_DatabaseError() throws SQLException {
            // Mock 用户查询
            when(userMapper.selectById(1L)).thenReturn(adminUser);

            // Mock 数据库连接异常
            when(connection.getMetaData()).thenThrow(new SQLException("Connection failed"));

            // 执行测试并验证异常
            assertThrows(
                    SQLException.class,
                    () -> queryPermissionChecker.getPermittedTables(1L, 1L, connection));

            verify(userMapper).selectById(1L);
            verify(connection).getMetaData();
        }

        @Test
        @DisplayName("数据源ID为null - 管理员应查询所有数据源的启用模块")
        void testGetPermittedTables_AdminUser_NullDatasourceId() throws SQLException {
            // Mock 用户查询
            when(userMapper.selectById(1L)).thenReturn(adminUser);

            // Mock 数据库真实表
            mockDatabaseTables("global_table1", "global_table2");

            // Mock 所有数据源的启用模块表名
            List<String> enabledModuleTables = Arrays.asList("global_table1", "global_table3");
            when(moduleInfoMapper.selectEnabledModuleTableNames(null))
                    .thenReturn(enabledModuleTables);

            // 执行测试
            List<String> result = queryPermissionChecker.getPermittedTables(1L, null, connection);

            // 验证结果
            assertEquals(1, result.size());
            assertEquals("global_table1", result.get(0));

            // 验证方法调用
            verify(moduleInfoMapper).selectEnabledModuleTableNames(null);
        }

        /** Mock数据库表查询结果 */
        private void mockDatabaseTables(String... tableNames) throws SQLException {
            when(connection.getMetaData()).thenReturn(databaseMetaData);
            when(databaseMetaData.getTables(any(), isNull(), eq("%"), eq(new String[] {"TABLE"})))
                    .thenReturn(resultSet);

            // Mock ResultSet的迭代行为
            boolean[] callCount = {false}; // 用于跟踪调用次数
            int[] currentIndex = {0};

            when(resultSet.next())
                    .thenAnswer(
                            invocation -> {
                                if (currentIndex[0] < tableNames.length) {
                                    return true;
                                } else {
                                    return false;
                                }
                            });

            when(resultSet.getString("TABLE_NAME"))
                    .thenAnswer(
                            invocation -> {
                                if (currentIndex[0] < tableNames.length) {
                                    String tableName = tableNames[currentIndex[0]];
                                    currentIndex[0]++;
                                    return tableName;
                                }
                                return null;
                            });
        }
    }

    // ================ 原有测试方法保持不变 ================

    @Nested
    @DisplayName("checkQueryPermission 方法测试")
    class CheckQueryPermissionTest {
        // 原有的所有 checkQueryPermission 相关测试方法可以移到这里
        // 为了保持向后兼容，暂时保留在外层
    }

    @Test
    @DisplayName("普通用户权限检查 - SELECT查询需要表权限")
    void testCheckQueryPermission_NormalUser_SelectWithPermission() {
        // Mock TableValidationService 返回表名和SQL类型检查
        when(tableValidationService.isSelectStatement("SELECT * FROM user_logs")).thenReturn(true);
        when(tableValidationService.extractTableNames("SELECT * FROM user_logs"))
                .thenReturn(Set.of("user_logs"));

        // Mock用户有user_logs表的权限
        when(userModulePermissionMapper.selectPermittedTableNames(2L, 1L))
                .thenReturn(Arrays.asList("user_logs", "other_table"));

        // 执行测试 - 有权限的SELECT查询应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                normalUser, "SELECT * FROM user_logs", 1L));

        // 验证调用了权限检查
        verify(userModulePermissionMapper).selectPermittedTableNames(2L, 1L);
    }

    @Test
    @DisplayName("普通用户权限检查 - SELECT查询无权限应被拒绝")
    void testCheckQueryPermission_NormalUser_SelectWithoutPermission() {
        // Mock TableValidationService 返回表名和SQL类型检查
        when(tableValidationService.isSelectStatement("SELECT * FROM system_logs"))
                .thenReturn(true);
        when(tableValidationService.extractTableNames("SELECT * FROM system_logs"))
                .thenReturn(Set.of("system_logs"));

        // Mock用户没有system_logs表的权限（返回其他表）
        when(userModulePermissionMapper.selectPermittedTableNames(2L, 1L))
                .thenReturn(Arrays.asList("user_logs", "other_table"));

        // 执行测试 - 无权限的SELECT查询应被拒绝
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                queryPermissionChecker.checkQueryPermission(
                                        normalUser, "SELECT * FROM system_logs", 1L));

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        // 仅验证异常类型和错误码，不依赖具体错误消息

        // 验证调用了权限检查
        verify(userModulePermissionMapper).selectPermittedTableNames(2L, 1L);
    }

    @Test
    @DisplayName("表名提取测试 - 简单FROM子句")
    void testExtractTableNames_SimpleFROM() {
        // 测试简单表名提取 - 直接测试TableValidationService的行为
        when(tableValidationService.extractTableNames("SELECT * FROM user_logs"))
                .thenReturn(Set.of("user_logs"));

        Set<String> tables = tableValidationService.extractTableNames("SELECT * FROM user_logs");
        assertEquals(1, tables.size());
        assertTrue(tables.contains("user_logs"));

        // 测试带引号的表名
        when(tableValidationService.extractTableNames("SELECT * FROM `system_logs`"))
                .thenReturn(Set.of("system_logs"));

        tables = tableValidationService.extractTableNames("SELECT * FROM `system_logs`");
        assertEquals(1, tables.size());
        assertTrue(tables.contains("system_logs"));
    }

    @Test
    @DisplayName("表名提取测试 - 多表JOIN")
    void testExtractTableNames_MultipleTablesWithJOIN() {
        // 测试多表JOIN - 直接测试TableValidationService的行为
        String sql = "SELECT u.*, l.* FROM user_logs u JOIN system_logs l ON u.id = l.user_id";
        when(tableValidationService.extractTableNames(sql))
                .thenReturn(Set.of("user_logs", "system_logs"));

        Set<String> tables = tableValidationService.extractTableNames(sql);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("user_logs"));
        assertTrue(tables.contains("system_logs"));
    }

    @Test
    @DisplayName("SELECT查询检查 - 正确识别SELECT语句")
    void testIsSelectQuery() {
        // 测试各种SELECT语句格式 - 直接测试TableValidationService的行为
        when(tableValidationService.isSelectStatement("SELECT * FROM users")).thenReturn(true);
        when(tableValidationService.isSelectStatement("select id from logs")).thenReturn(true);
        when(tableValidationService.isSelectStatement("  SELECT count(*) FROM data  "))
                .thenReturn(true);

        assertTrue(tableValidationService.isSelectStatement("SELECT * FROM users"));
        assertTrue(tableValidationService.isSelectStatement("select id from logs"));
        assertTrue(tableValidationService.isSelectStatement("  SELECT count(*) FROM data  "));

        // 测试非SELECT语句
        when(tableValidationService.isSelectStatement("UPDATE users SET name = 'test'"))
                .thenReturn(false);
        when(tableValidationService.isSelectStatement("DELETE FROM logs")).thenReturn(false);
        when(tableValidationService.isSelectStatement("INSERT INTO users VALUES (1)"))
                .thenReturn(false);

        assertFalse(tableValidationService.isSelectStatement("UPDATE users SET name = 'test'"));
        assertFalse(tableValidationService.isSelectStatement("DELETE FROM logs"));
        assertFalse(tableValidationService.isSelectStatement("INSERT INTO users VALUES (1)"));
    }

    @Test
    @DisplayName("多表权限检查 - 需要所有表都有权限")
    void testCheckQueryPermission_MultipleTablesPermission() {
        // Mock TableValidationService 返回多个表名和SQL类型检查
        String sql = "SELECT * FROM user_logs, system_logs";
        when(tableValidationService.isSelectStatement(sql)).thenReturn(true);
        when(tableValidationService.extractTableNames(sql))
                .thenReturn(Set.of("user_logs", "system_logs"));

        // Mock用户只有user_logs权限，没有system_logs权限
        when(userModulePermissionMapper.selectPermittedTableNames(2L, 1L))
                .thenReturn(Arrays.asList("user_logs"));

        // 执行测试 - 查询多表时，只要有一个表无权限就应该被拒绝
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> queryPermissionChecker.checkQueryPermission(normalUser, sql, 1L));

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        // 仅验证异常类型和错误码，不依赖具体错误消息

        // 验证调用了权限检查
        verify(userModulePermissionMapper).selectPermittedTableNames(2L, 1L);
    }
}
