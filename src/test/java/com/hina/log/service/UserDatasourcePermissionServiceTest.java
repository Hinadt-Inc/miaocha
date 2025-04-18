package com.hina.log.service;

import com.hina.log.entity.User;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserDatasourcePermissionMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.impl.UserDatasourcePermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户数据源权限服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserDatasourcePermissionServiceTest {

    @Mock
    private UserDatasourcePermissionMapper permissionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDatasourcePermissionServiceImpl permissionService;

    private User normalUser;
    private User adminUser;
    private User superAdminUser;
    private UserDatasourcePermission testPermission;

    @BeforeEach
    void setUp() {
        // 准备普通用户
        normalUser = new User();
        normalUser.setId(1L);
        normalUser.setUid("normal_user");
        normalUser.setNickname("普通用户");
        normalUser.setRole(UserRole.USER.name());
        normalUser.setStatus(1);

        // 准备管理员用户
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUid("admin_user");
        adminUser.setNickname("管理员用户");
        adminUser.setRole(UserRole.ADMIN.name());
        adminUser.setStatus(1);

        // 准备超级管理员用户
        superAdminUser = new User();
        superAdminUser.setId(3L);
        superAdminUser.setUid("super_admin");
        superAdminUser.setNickname("超级管理员");
        superAdminUser.setRole(UserRole.SUPER_ADMIN.name());
        superAdminUser.setStatus(1);

        // 准备测试权限
        testPermission = new UserDatasourcePermission();
        testPermission.setId(1L);
        testPermission.setUserId(1L);
        testPermission.setDatasourceId(1L);
        testPermission.setTableName("test_table");

        // 设置默认行为
        when(userMapper.selectById(1L)).thenReturn(normalUser);
        when(userMapper.selectById(2L)).thenReturn(adminUser);
        when(userMapper.selectById(3L)).thenReturn(superAdminUser);
        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "test_table")).thenReturn(testPermission);
    }

    @Test
    void testGetUserDatasourcePermissions() {
        // 准备数据
        List<UserDatasourcePermission> permissions = Arrays.asList(testPermission);
        when(permissionMapper.selectByUserAndDatasource(1L, 1L)).thenReturn(permissions);

        // 执行测试
        List<UserDatasourcePermission> result = permissionService.getUserDatasourcePermissions(1L, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPermission.getId(), result.get(0).getId());
        assertEquals(testPermission.getUserId(), result.get(0).getUserId());
        assertEquals(testPermission.getDatasourceId(), result.get(0).getDatasourceId());
        assertEquals(testPermission.getTableName(), result.get(0).getTableName());

        // 验证调用
        verify(permissionMapper).selectByUserAndDatasource(1L, 1L);
    }

    @Test
    void testHasTablePermission_NormalUser_WithPermission() {
        // 准备数据
        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "test_table")).thenReturn(testPermission);

        // 执行测试
        boolean result = permissionService.hasTablePermission(1L, 1L, "test_table");

        // 验证结果
        assertTrue(result);

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "test_table");
    }

    @Test
    void testHasTablePermission_NormalUser_WithoutPermission() {
        // 准备数据
        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "other_table")).thenReturn(null);
        when(permissionMapper.selectAllTablesPermission(1L, 1L)).thenReturn(null);

        // 执行测试
        boolean result = permissionService.hasTablePermission(1L, 1L, "other_table");

        // 验证结果
        assertFalse(result);

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(permissionMapper).selectAllTablesPermission(1L, 1L);
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "other_table");
    }

    @Test
    void testHasTablePermission_NormalUser_WithWildcardPermission() {
        // 准备数据
        UserDatasourcePermission wildcardPermission = new UserDatasourcePermission();
        wildcardPermission.setId(2L);
        wildcardPermission.setUserId(1L);
        wildcardPermission.setDatasourceId(1L);
        wildcardPermission.setTableName("*");

        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "other_table")).thenReturn(null);
        when(permissionMapper.selectAllTablesPermission(1L, 1L)).thenReturn(wildcardPermission);

        // 执行测试
        boolean result = permissionService.hasTablePermission(1L, 1L, "other_table");

        // 验证结果
        assertTrue(result);

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(permissionMapper).selectAllTablesPermission(1L, 1L);
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "other_table");
    }

    @Test
    void testHasTablePermission_AdminUser() {
        // 执行测试
        boolean result = permissionService.hasTablePermission(2L, 1L, "any_table");

        // 验证结果
        assertTrue(result);

        // 验证调用
        verify(userMapper).selectById(2L);
        verify(permissionMapper, never()).selectAllTablesPermission(anyLong(), anyLong());
        verify(permissionMapper, never()).selectByUserDatasourceAndTable(anyLong(), anyLong(), anyString());
    }

    @Test
    void testHasTablePermission_SuperAdminUser() {
        // 执行测试
        boolean result = permissionService.hasTablePermission(3L, 1L, "any_table");

        // 验证结果
        assertTrue(result);

        // 验证调用
        verify(userMapper).selectById(3L);
        verify(permissionMapper, never()).selectAllTablesPermission(anyLong(), anyLong());
        verify(permissionMapper, never()).selectByUserDatasourceAndTable(anyLong(), anyLong(), anyString());
    }

    @Test
    void testHasTablePermission_UserNotFound() {
        // 准备数据
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            permissionService.hasTablePermission(999L, 1L, "test_table");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(permissionMapper, never()).selectAllTablesPermission(anyLong(), anyLong());
        verify(permissionMapper, never()).selectByUserDatasourceAndTable(anyLong(), anyLong(), anyString());
    }

    @Test
    void testGrantTablePermission_Success_NewPermission() {
        // 准备数据
        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "new_table")).thenReturn(null);
        when(permissionMapper.insert(any(UserDatasourcePermission.class))).thenReturn(1);

        // 执行测试
        UserDatasourcePermission result = permissionService.grantTablePermission(1L, 1L, "new_table");

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getDatasourceId());
        assertEquals("new_table", result.getTableName());

        // 验证调用
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "new_table");
        verify(permissionMapper).insert(any(UserDatasourcePermission.class));
    }

    @Test
    void testGrantTablePermission_Success_ExistingPermission() {
        // 准备数据 - 权限已存在
        UserDatasourcePermission existingPermission = new UserDatasourcePermission();
        existingPermission.setId(5L);
        existingPermission.setUserId(1L);
        existingPermission.setDatasourceId(1L);
        existingPermission.setTableName("existing_table");

        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "existing_table")).thenReturn(existingPermission);

        // 执行测试
        UserDatasourcePermission result = permissionService.grantTablePermission(1L, 1L, "existing_table");

        // 验证结果
        assertNotNull(result);
        assertEquals(existingPermission.getId(), result.getId());
        assertEquals(existingPermission.getUserId(), result.getUserId());
        assertEquals(existingPermission.getDatasourceId(), result.getDatasourceId());
        assertEquals(existingPermission.getTableName(), result.getTableName());

        // 验证调用
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "existing_table");
        verify(permissionMapper, never()).insert(any(UserDatasourcePermission.class));
    }

    @Test
    void testRevokeTablePermission_ById_Success() {
        // 准备数据
        when(permissionMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            permissionService.revokeTablePermission(1L);
        });

        // 验证调用
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    void testRevokeTablePermission_ByParams_Success() {
        // 执行测试
        assertDoesNotThrow(() -> {
            permissionService.revokeTablePermission(1L, 1L, "test_table");
        });

        // 验证调用
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "test_table");
        verify(permissionMapper).deleteById(testPermission.getId());
    }

    @Test
    void testRevokeTablePermission_ByParams_NotFound() {
        // 准备数据
        when(permissionMapper.selectByUserDatasourceAndTable(1L, 1L, "non_existent_table")).thenReturn(null);

        // 执行测试
        assertDoesNotThrow(() -> {
            permissionService.revokeTablePermission(1L, 1L, "non_existent_table");
        });

        // 验证调用
        verify(permissionMapper).selectByUserDatasourceAndTable(1L, 1L, "non_existent_table");
        verify(permissionMapper, never()).deleteById(anyLong());
    }
}