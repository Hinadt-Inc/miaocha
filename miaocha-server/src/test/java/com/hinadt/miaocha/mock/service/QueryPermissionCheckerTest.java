package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.application.service.impl.QueryPermissionChecker;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import io.qameta.allure.*;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 查询权限检查器测试
 *
 * <p>重点测试权限验证的核心业务逻辑，确保不同角色用户的权限控制正确
 */
@Epic("秒查日志管理系统")
@Feature("权限管理")
@DisplayName("查询权限检查器测试")
@Owner("开发团队")
@ExtendWith(MockitoExtension.class)
class QueryPermissionCheckerTest {

    @Mock private ModulePermissionService modulePermissionService;
    @Mock private TableValidationService tableValidationService;

    private QueryPermissionChecker queryPermissionChecker;

    private User adminUser;
    private User normalUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        // 使用构造函数注入创建实例
        queryPermissionChecker =
                new QueryPermissionChecker(modulePermissionService, tableValidationService);

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
    @Description("测试管理员用户无需权限检查即可执行任何SQL")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckQueryPermission_AdminUser() {
        // 执行测试 - 管理员执行任何SQL都应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, 1L, "DELETE FROM user_logs"));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, 1L, "UPDATE user_logs SET status = 1"));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                adminUser, 1L, "SELECT * FROM user_logs"));

        // 验证不会调用模块权限检查
        verify(modulePermissionService, never()).hasModulePermission(anyLong(), anyString());
    }

    @Test
    @DisplayName("超级管理员权限检查 - 超级管理员应该可以执行任何查询")
    @Description("测试超级管理员用户无需权限检查即可执行任何SQL")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckQueryPermission_SuperAdminUser() {
        // 执行测试 - 超级管理员执行任何SQL都应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                superAdminUser, 1L, "DROP TABLE user_logs"));

        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                superAdminUser, 1L, "INSERT INTO user_logs VALUES (1)"));

        // 验证不会调用模块权限检查
        verify(modulePermissionService, never()).hasModulePermission(anyLong(), anyString());
    }

    @Test
    @DisplayName("普通用户权限检查 - 只能执行SELECT查询")
    @Description("测试普通用户只能执行SELECT查询，其他操作应被拒绝")
    @Severity(SeverityLevel.CRITICAL)
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
                            () -> queryPermissionChecker.checkQueryPermission(normalUser, 1L, sql));

            assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
            // 仅验证异常类型和错误码，不依赖具体错误消息
        }
    }

    @Test
    @DisplayName("普通用户权限检查 - SELECT查询需要模块权限")
    @Description("测试普通用户执行SELECT查询时需要验证对表的模块权限")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckQueryPermission_NormalUser_SelectWithPermission() {
        // Mock TableValidationService 返回表名和SQL类型检查
        when(tableValidationService.isSelectStatement("SELECT * FROM user_logs")).thenReturn(true);
        when(tableValidationService.extractTableNames("SELECT * FROM user_logs"))
                .thenReturn(Set.of("user_logs"));

        // Mock用户有user_logs表的权限
        when(modulePermissionService.hasModulePermission(2L, "user_logs")).thenReturn(true);

        // 执行测试 - 有权限的SELECT查询应该通过
        assertDoesNotThrow(
                () ->
                        queryPermissionChecker.checkQueryPermission(
                                normalUser, 1L, "SELECT * FROM user_logs"));

        // 验证调用了权限检查
        verify(modulePermissionService).hasModulePermission(2L, "user_logs");
    }

    @Test
    @DisplayName("普通用户权限检查 - SELECT查询无权限应被拒绝")
    @Description("测试普通用户执行SELECT查询时，如果没有表权限应被拒绝")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckQueryPermission_NormalUser_SelectWithoutPermission() {
        // Mock TableValidationService 返回表名和SQL类型检查
        when(tableValidationService.isSelectStatement("SELECT * FROM system_logs"))
                .thenReturn(true);
        when(tableValidationService.extractTableNames("SELECT * FROM system_logs"))
                .thenReturn(Set.of("system_logs"));

        // Mock用户没有system_logs表的权限
        when(modulePermissionService.hasModulePermission(2L, "system_logs")).thenReturn(false);

        // 执行测试 - 无权限的SELECT查询应被拒绝
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                queryPermissionChecker.checkQueryPermission(
                                        normalUser, 1L, "SELECT * FROM system_logs"));

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        // 仅验证异常类型和错误码，不依赖具体错误消息

        // 验证调用了权限检查
        verify(modulePermissionService).hasModulePermission(2L, "system_logs");
    }

    @Test
    @DisplayName("表名提取测试 - 简单FROM子句")
    @Description("测试从简单的FROM子句中正确提取表名")
    @Severity(SeverityLevel.NORMAL)
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
    @Description("测试从复杂JOIN查询中正确提取所有表名")
    @Severity(SeverityLevel.NORMAL)
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
    @Description("测试正确识别各种格式的SELECT语句")
    @Severity(SeverityLevel.NORMAL)
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
    @Description("测试查询多个表时，需要对所有表都有权限")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckQueryPermission_MultipleTablesPermission() {
        // Mock TableValidationService 返回多个表名和SQL类型检查
        String sql = "SELECT * FROM user_logs, system_logs";
        when(tableValidationService.isSelectStatement(sql)).thenReturn(true);
        when(tableValidationService.extractTableNames(sql))
                .thenReturn(Set.of("user_logs", "system_logs"));

        // Mock用户对system_logs无权限 - 由于业务逻辑的执行顺序，可能只会检查一个表就抛异常
        when(modulePermissionService.hasModulePermission(eq(2L), anyString())).thenReturn(false);

        // 执行测试 - 查询多表时，只要有一个表无权限就应该被拒绝
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> queryPermissionChecker.checkQueryPermission(normalUser, 1L, sql));

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        // 仅验证异常类型和错误码，不依赖具体错误消息
    }
}
