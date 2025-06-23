package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hinadt.miaocha.application.service.impl.ModulePermissionServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.converter.ModulePermissionConverter;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.UserModulePermission;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import io.qameta.allure.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 模块权限服务单元测试 */
@Epic("秒查日志管理系统")
@Feature("模块权限管理")
@ExtendWith(MockitoExtension.class)
@DisplayName("模块权限服务测试")
@Owner("开发团队")
public class ModulePermissionServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private DatasourceMapper datasourceMapper;
    @Mock private UserModulePermissionMapper userModulePermissionMapper;
    @Mock private ModuleInfoMapper moduleInfoMapper;
    @Mock private ModulePermissionConverter modulePermissionConverter;

    private ModulePermissionServiceImpl modulePermissionService;

    private User testUser;
    private User testAdmin;
    private DatasourceInfo testDatasource;
    private ModuleInfo testModule;
    private UserModulePermission testPermission;

    @BeforeEach
    void setUp() {
        // 重置所有Mock
        reset(
                userMapper,
                datasourceMapper,
                userModulePermissionMapper,
                moduleInfoMapper,
                modulePermissionConverter);

        // 创建服务实例
        modulePermissionService =
                new ModulePermissionServiceImpl(
                        null,
                        userMapper,
                        datasourceMapper,
                        userModulePermissionMapper,
                        moduleInfoMapper,
                        modulePermissionConverter);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 普通用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUid("test_uid");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER.name());

        // 管理员用户
        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setUid("admin_uid");
        testAdmin.setNickname("管理员");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setRole(UserRole.ADMIN.name());

        // 测试数据源
        testDatasource = new DatasourceInfo();
        testDatasource.setId(1L);
        testDatasource.setName("测试数据源");
        testDatasource.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");

        // 测试模块
        testModule = new ModuleInfo();
        testModule.setId(1L);
        testModule.setName("用户模块");
        testModule.setDatasourceId(1L);

        // 测试权限
        testPermission = new UserModulePermission();
        testPermission.setId(1L);
        testPermission.setUserId(1L);
        testPermission.setDatasourceId(1L);
        testPermission.setModule("用户模块");
        testPermission.setCreateTime(LocalDateTime.now());
        testPermission.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @Story("权限检查")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("检查普通用户模块权限 - 有权限")
    @Description("验证普通用户对有权限的模块返回true")
    void testHasModulePermission_UserWithPermission() {
        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(userModulePermissionMapper.select(1L, 1L, "用户模块")).thenReturn(testPermission);

        // 执行测试
        boolean result = modulePermissionService.hasModulePermission(1L, "用户模块");

        // 验证结果
        assertTrue(result);
        verify(userMapper).selectById(1L);
        verify(moduleInfoMapper).selectByName("用户模块");
        verify(userModulePermissionMapper).select(1L, 1L, "用户模块");
    }

    @Test
    @DisplayName("检查普通用户模块权限 - 无权限")
    void testHasModulePermission_UserWithoutPermission() {
        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(userModulePermissionMapper.select(1L, 1L, "用户模块")).thenReturn(null);

        // 执行测试
        boolean result = modulePermissionService.hasModulePermission(1L, "用户模块");

        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("检查管理员模块权限 - 总是有权限")
    void testHasModulePermission_AdminUser() {
        // Mock设置
        when(userMapper.selectById(2L)).thenReturn(testAdmin);

        // 执行测试
        boolean result = modulePermissionService.hasModulePermission(2L, "用户模块");

        // 验证结果
        assertTrue(result);
        verify(userMapper).selectById(2L);
        // 管理员不需要检查具体权限
        verify(moduleInfoMapper, never()).selectByName(anyString());
        verify(userModulePermissionMapper, never()).select(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("检查不存在用户的权限")
    void testHasModulePermission_UserNotFound() {
        // Mock设置
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> modulePermissionService.hasModulePermission(999L, "用户模块"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("获取用户模块权限")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("获取普通用户可访问模块列表")
    @Description("验证普通用户能正确获取其可访问的模块列表")
    void testGetUserAccessibleModules_NormalUser() {
        // 设置converter mock
        UserModulePermissionDTO mockDto = new UserModulePermissionDTO();
        mockDto.setId(testPermission.getId());
        mockDto.setUserId(testPermission.getUserId());
        mockDto.setDatasourceId(testPermission.getDatasourceId());
        mockDto.setModule(testPermission.getModule());
        mockDto.setDatasourceName(testDatasource.getName());
        mockDto.setDatabaseName(DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()));

        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userModulePermissionMapper.selectByUser(1L)).thenReturn(Arrays.asList(testPermission));
        when(modulePermissionConverter.toDtos(anyList())).thenReturn(Arrays.asList(mockDto));

        // 执行测试
        List<UserModulePermissionDTO> result = modulePermissionService.getUserAccessibleModules(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        UserModulePermissionDTO dto = result.get(0);
        assertEquals(testPermission.getId(), dto.getId());
        assertEquals(testPermission.getUserId(), dto.getUserId());
        assertEquals(testPermission.getDatasourceId(), dto.getDatasourceId());
        assertEquals(testPermission.getModule(), dto.getModule());
        assertEquals(testDatasource.getName(), dto.getDatasourceName());
        assertEquals(
                DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()),
                dto.getDatabaseName());

        verify(userMapper).selectById(1L);
        verify(userModulePermissionMapper).selectByUser(1L);
        verify(modulePermissionConverter).toDtos(Arrays.asList(testPermission));
    }

    @Test
    @DisplayName("获取管理员可访问模块列表")
    void testGetUserAccessibleModules_AdminUser() {
        // 准备额外的测试数据
        ModuleInfo module2 = new ModuleInfo();
        module2.setId(2L);
        module2.setName("订单模块");
        module2.setDatasourceId(1L);

        // 设置converter mock for admin permissions
        UserModulePermissionDTO mockAdminDto1 = new UserModulePermissionDTO();
        mockAdminDto1.setId(null); // 管理员没有特定的权限ID
        mockAdminDto1.setUserId(testAdmin.getId());
        mockAdminDto1.setDatasourceId(testDatasource.getId());
        mockAdminDto1.setModule(testModule.getName());
        mockAdminDto1.setDatasourceName(testDatasource.getName());
        mockAdminDto1.setDatabaseName(
                DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()));

        UserModulePermissionDTO mockAdminDto2 = new UserModulePermissionDTO();
        mockAdminDto2.setId(null);
        mockAdminDto2.setUserId(testAdmin.getId());
        mockAdminDto2.setDatasourceId(testDatasource.getId());
        mockAdminDto2.setModule(module2.getName());
        mockAdminDto2.setDatasourceName(testDatasource.getName());
        mockAdminDto2.setDatabaseName(
                DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()));

        // Mock设置
        when(userMapper.selectById(2L)).thenReturn(testAdmin);
        when(moduleInfoMapper.selectAll()).thenReturn(Arrays.asList(testModule, module2));
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);
        when(modulePermissionConverter.createAdminPermissionDto(
                        anyLong(), anyLong(), anyString(), any(DatasourceInfo.class)))
                .thenReturn(mockAdminDto1, mockAdminDto2);

        // 执行测试
        List<UserModulePermissionDTO> result = modulePermissionService.getUserAccessibleModules(2L);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证第一个模块
        UserModulePermissionDTO dto1 = result.get(0);
        assertEquals(testModule.getName(), dto1.getModule());
        assertEquals(testDatasource.getName(), dto1.getDatasourceName());
        assertNull(dto1.getId()); // 管理员没有特定的权限ID

        // 验证第二个模块
        UserModulePermissionDTO dto2 = result.get(1);
        assertEquals(module2.getName(), dto2.getModule());

        verify(userMapper).selectById(2L);
        verify(moduleInfoMapper).selectAll();
        verify(datasourceMapper, times(2)).selectById(1L);
        verify(modulePermissionConverter, times(2))
                .createAdminPermissionDto(
                        anyLong(), anyLong(), anyString(), any(DatasourceInfo.class));
    }

    @Test
    @DisplayName("获取不存在用户的模块列表")
    void testGetUserAccessibleModules_UserNotFound() {
        // Mock设置
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> modulePermissionService.getUserAccessibleModules(999L));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("权限授予")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("授予用户模块权限 - 成功")
    @Description("验证能够成功授予用户对模块的访问权限")
    void testGrantModulePermission_Success() {
        // 设置converter mock
        UserModulePermissionDTO mockDto = new UserModulePermissionDTO();
        mockDto.setId(testPermission.getId());
        mockDto.setUserId(testPermission.getUserId());
        mockDto.setDatasourceId(testPermission.getDatasourceId());
        mockDto.setModule(testPermission.getModule());
        mockDto.setDatasourceName(testDatasource.getName());
        mockDto.setDatabaseName(DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()));

        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);
        when(userModulePermissionMapper.select(1L, 1L, "用户模块")).thenReturn(null);
        when(userModulePermissionMapper.insert(any(UserModulePermission.class))).thenReturn(1);
        when(modulePermissionConverter.toDto(any(UserModulePermission.class))).thenReturn(mockDto);

        // 执行测试
        UserModulePermissionDTO result = modulePermissionService.grantModulePermission(1L, "用户模块");

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getDatasourceId());
        assertEquals("用户模块", result.getModule());

        verify(userMapper).selectById(1L);
        verify(moduleInfoMapper, times(2)).selectByName("用户模块");
        verify(datasourceMapper).selectById(1L); // 只在service中检查datasource存在性
        verify(userModulePermissionMapper).select(1L, 1L, "用户模块");
        verify(userModulePermissionMapper).insert(any(UserModulePermission.class));
        verify(modulePermissionConverter).toDto(any(UserModulePermission.class));
    }

    @Test
    @DisplayName("授予用户模块权限 - 权限已存在")
    void testGrantModulePermission_PermissionExists() {
        // 设置converter mock
        UserModulePermissionDTO mockDto = new UserModulePermissionDTO();
        mockDto.setId(testPermission.getId());
        mockDto.setUserId(testPermission.getUserId());
        mockDto.setDatasourceId(testPermission.getDatasourceId());
        mockDto.setModule(testPermission.getModule());
        mockDto.setDatasourceName(testDatasource.getName());
        mockDto.setDatabaseName(DatasourceType.extractDatabaseName(testDatasource.getJdbcUrl()));

        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);
        when(userModulePermissionMapper.select(1L, 1L, "用户模块")).thenReturn(testPermission);
        when(modulePermissionConverter.toDto(any(UserModulePermission.class))).thenReturn(mockDto);

        // 执行测试
        UserModulePermissionDTO result = modulePermissionService.grantModulePermission(1L, "用户模块");

        // 验证结果
        assertNotNull(result);
        assertEquals(testPermission.getId(), result.getId());

        // 不应该插入新权限
        verify(userModulePermissionMapper, never()).insert(any(UserModulePermission.class));
        verify(modulePermissionConverter).toDto(testPermission);
    }

    @Test
    @DisplayName("授予用户模块权限 - 用户不存在")
    void testGrantModulePermission_UserNotFound() {
        // Mock设置
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> modulePermissionService.grantModulePermission(999L, "用户模块"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("授予用户模块权限 - 模块不存在")
    void testGrantModulePermission_ModuleNotFound() {
        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("不存在的模块")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> modulePermissionService.grantModulePermission(1L, "不存在的模块"));

        assertEquals(ErrorCode.MODULE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("权限撤销")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("撤销用户模块权限 - 成功")
    @Description("验证能够成功撤销用户对模块的访问权限")
    void testRevokeModulePermission_Success() {
        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);
        when(userModulePermissionMapper.delete(1L, 1L, "用户模块")).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> modulePermissionService.revokeModulePermission(1L, "用户模块"));

        verify(userMapper).selectById(1L);
        verify(moduleInfoMapper, times(2)).selectByName("用户模块");
        verify(datasourceMapper, times(1)).selectById(1L);
        verify(userModulePermissionMapper).delete(1L, 1L, "用户模块");
    }

    @Test
    @DisplayName("撤销用户模块权限 - 用户不存在")
    void testRevokeModulePermission_UserNotFound() {
        // Mock设置
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> modulePermissionService.revokeModulePermission(999L, "用户模块"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @Story("批量权限操作")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("批量授予用户模块权限")
    @Description("验证能够批量授予用户对多个模块的访问权限")
    void testBatchGrantModulePermissions() {
        List<String> modules = Arrays.asList("用户模块", "订单模块");

        // 创建第二个模块信息
        ModuleInfo module2 = new ModuleInfo();
        module2.setId(2L);
        module2.setName("订单模块");
        module2.setDatasourceId(1L);

        // 设置converter mock
        UserModulePermissionDTO mockDto1 = new UserModulePermissionDTO();
        mockDto1.setId(1L);
        mockDto1.setUserId(1L);
        mockDto1.setDatasourceId(1L);
        mockDto1.setModule("用户模块");

        UserModulePermissionDTO mockDto2 = new UserModulePermissionDTO();
        mockDto2.setId(2L);
        mockDto2.setUserId(1L);
        mockDto2.setDatasourceId(1L);
        mockDto2.setModule("订单模块");

        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(moduleInfoMapper.selectByName("订单模块")).thenReturn(module2);
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);
        when(userModulePermissionMapper.select(eq(1L), eq(1L), anyString())).thenReturn(null);
        when(userModulePermissionMapper.insert(any(UserModulePermission.class))).thenReturn(1);
        when(modulePermissionConverter.toDto(any(UserModulePermission.class)))
                .thenReturn(mockDto1, mockDto2);

        // 执行测试
        List<UserModulePermissionDTO> result =
                modulePermissionService.batchGrantModulePermissions(1L, modules);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(userMapper).selectById(1L);
        verify(moduleInfoMapper, times(2)).selectByName("用户模块");
        verify(moduleInfoMapper, times(2)).selectByName("订单模块");
        verify(userModulePermissionMapper, times(2)).insert(any(UserModulePermission.class));
        verify(modulePermissionConverter, times(2)).toDto(any(UserModulePermission.class));
    }

    @Test
    @DisplayName("批量撤销用户模块权限")
    void testBatchRevokeModulePermissions() {
        List<String> modules = Arrays.asList("用户模块", "订单模块");

        // 创建第二个模块信息
        ModuleInfo module2 = new ModuleInfo();
        module2.setId(2L);
        module2.setName("订单模块");
        module2.setDatasourceId(1L);

        // Mock设置
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(moduleInfoMapper.selectByName("用户模块")).thenReturn(testModule);
        when(moduleInfoMapper.selectByName("订单模块")).thenReturn(module2);
        when(datasourceMapper.selectById(1L)).thenReturn(testDatasource);

        // 执行测试
        assertDoesNotThrow(() -> modulePermissionService.batchRevokeModulePermissions(1L, modules));

        verify(userMapper, times(3)).selectById(1L);
        verify(moduleInfoMapper, times(2)).selectByName("用户模块");
        verify(moduleInfoMapper, times(2)).selectByName("订单模块");
        verify(userModulePermissionMapper).delete(1L, 1L, "用户模块");
        verify(userModulePermissionMapper).delete(1L, 1L, "订单模块");
    }
}
