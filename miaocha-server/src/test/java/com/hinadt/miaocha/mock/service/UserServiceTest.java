package com.hinadt.miaocha.mock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

import com.hinadt.miaocha.application.service.LdapAuthenticationService;
import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.application.service.impl.UserServiceImpl;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.config.security.JwtUtils;
import com.hinadt.miaocha.domain.converter.UserConverter;
import com.hinadt.miaocha.domain.dto.auth.LoginRequestDTO;
import com.hinadt.miaocha.domain.dto.auth.LoginResponseDTO;
import com.hinadt.miaocha.domain.dto.user.AdminUpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UserCreateDTO;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import com.hinadt.miaocha.domain.dto.user.UserUpdateDTO;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户服务单元测试
 *
 * <p>测试秒查系统的用户管理功能，包括用户注册、登录、密码管理等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
public class UserServiceTest {

    @Mock private UserMapper userMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtUtils jwtUtils;

    @Mock private UserConverter userConverter;

    @Mock private ModulePermissionService modulePermissionService;

    @Mock private UserModulePermissionMapper userModulePermissionMapper;

    @Mock private LdapAuthenticationService ldapAuthenticationService;

    private UserServiceImpl userService;

    private User testUser;
    private UserCreateDTO createDTO;
    private UserUpdateDTO updateDTO;
    private LoginRequestDTO loginRequestDTO;
    private UpdatePasswordDTO updatePasswordDTO;
    private AdminUpdatePasswordDTO adminUpdatePasswordDTO;

    @BeforeEach
    void setUp() {
        // 重置所有Mock - 这很重要，避免测试间状态污染
        reset(
                userMapper,
                passwordEncoder,
                jwtUtils,
                userConverter,
                modulePermissionService,
                userModulePermissionMapper,
                ldapAuthenticationService);

        // 手动创建UserServiceImpl，注入所有mock对象
        userService =
                new UserServiceImpl(
                        userMapper,
                        passwordEncoder,
                        jwtUtils,
                        userConverter,
                        modulePermissionService,
                        userModulePermissionMapper,
                        ldapAuthenticationService);

        // 准备测试用户
        testUser = createTestUser();

        // 准备创建用户DTO
        createDTO = new UserCreateDTO();
        createDTO.setNickname("新用户");
        createDTO.setEmail("new@example.com");
        createDTO.setPassword("password123");
        createDTO.setRole(UserRole.USER.name());

        // 准备更新用户DTO
        updateDTO = new UserUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setNickname("更新用户");
        updateDTO.setEmail("update@example.com");
        updateDTO.setRole(UserRole.USER.name());
        updateDTO.setStatus(1);

        // 准备登录DTO
        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setEmail("test@example.com");
        loginRequestDTO.setPassword("password123");

        // 准备更新密码DTO
        updatePasswordDTO = new UpdatePasswordDTO();
        updatePasswordDTO.setOldPassword("old_password");
        updatePasswordDTO.setNewPassword("new_password");

        // 准备管理员更新密码DTO
        adminUpdatePasswordDTO = new AdminUpdatePasswordDTO();
        adminUpdatePasswordDTO.setPassword("new_password");
    }

    @Test
    @DisplayName("用户登录成功")
    void testLogin_Success() {
        // 设置mock行为
        when(userMapper.selectByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtils.generateTokenWithUserInfo(testUser, "system")).thenReturn("test_token");
        when(jwtUtils.generateRefreshTokenWithUserInfo(testUser, "system"))
                .thenReturn("test_refresh_token");
        when(jwtUtils.getExpirationFromToken("test_token"))
                .thenReturn(System.currentTimeMillis() + 3600000);
        when(jwtUtils.getExpirationFromToken("test_refresh_token"))
                .thenReturn(System.currentTimeMillis() + 86400000);

        // 执行测试
        LoginResponseDTO response = userService.login(loginRequestDTO);

        // 验证结果
        assertNotNull(response);
        assertEquals("test_token", response.getToken());
        assertEquals("test_refresh_token", response.getRefreshToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getNickname(), response.getNickname());
        assertEquals(testUser.getRole(), response.getRole());

        // 验证调用
        verify(userMapper).selectByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encoded_password");
        verify(jwtUtils).generateTokenWithUserInfo(testUser, "system");
        verify(jwtUtils).generateRefreshTokenWithUserInfo(testUser, "system");
        verify(jwtUtils).getExpirationFromToken("test_token");
        verify(jwtUtils).getExpirationFromToken("test_refresh_token");
    }

    @Test
    void testLogin_InvalidCredentials() {
        // 设置mock行为
        when(userMapper.selectByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.login(loginRequestDTO);
                        });

        assertEquals(ErrorCode.USER_PASSWORD_ERROR, exception.getErrorCode());
        verify(userMapper).selectByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encoded_password");
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void testLogin_UserNotFound() {
        // 设置mock行为 - 用户不存在
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.login(loginRequestDTO);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void testGetUserById_Success() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userConverter.toDto(testUser)).thenReturn(createUserDTO());

        // 执行测试
        UserDTO result = userService.getUserById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUid(), result.getUid());
        assertEquals(testUser.getNickname(), result.getNickname());

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(userConverter).toDto(testUser);
    }

    @Test
    void testGetUserById_NotFound() {
        // 设置mock行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.getUserById(999L);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
    }

    @Test
    void testGetUserByUid_Success() {
        // 设置mock行为
        when(userMapper.selectByUid("test_uid")).thenReturn(testUser);
        when(userConverter.toDto(testUser)).thenReturn(createUserDTO());

        // 执行测试
        UserDTO result = userService.getUserByUid("test_uid");

        // 验证结果
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUid(), result.getUid());
        assertEquals(testUser.getNickname(), result.getNickname());

        // 验证调用
        verify(userMapper).selectByUid("test_uid");
        verify(userConverter).toDto(testUser);
    }

    @Test
    void testGetUserByUid_NotFound() {
        // 设置mock行为
        when(userMapper.selectByUid("invalid_uid")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.getUserByUid("invalid_uid");
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectByUid("invalid_uid");
    }

    @Test
    void testGetAllUsers() {
        // 设置mock行为
        List<User> users = Arrays.asList(testUser);
        when(userMapper.selectAll()).thenReturn(users);
        when(userConverter.toDto(testUser)).thenReturn(createUserDTO());

        // 执行测试
        List<UserDTO> result = userService.getAllUsers();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getId(), result.get(0).getId());

        // 验证调用
        verify(userMapper).selectAll();
        verify(userConverter).toDto(testUser);
    }

    @Test
    void testCreateUser_Success() {
        // 设置mock行为
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
        when(userConverter.toEntity(createDTO)).thenReturn(testUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(userConverter.toDto(any(User.class))).thenReturn(createUserDTO());

        // 执行测试
        UserDTO result = userService.createUser(createDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getNickname());
        assertNotNull(result.getEmail());

        // 验证调用
        verify(userMapper).selectByEmail("new@example.com");
        verify(userConverter).toEntity(createDTO);
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testCreateUser_EmailExists() {
        // 设置mock行为 - 邮箱已存在
        when(userMapper.selectByEmail("new@example.com")).thenReturn(testUser);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.createUser(createDTO);
                        });

        assertEquals(ErrorCode.USER_NAME_EXISTS, exception.getErrorCode());
        assertEquals("邮箱已存在", exception.getMessage());
        verify(userMapper).selectByEmail("new@example.com");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testCreateUser_InvalidRole() {
        // 设置无效角色
        createDTO.setRole("INVALID_ROLE");

        // 设置mock行为
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.createUser(createDTO);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("无效的角色", exception.getMessage());
        verify(userMapper).selectByEmail("new@example.com");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectByEmail("update@example.com")).thenReturn(null);
        when(userConverter.updateEntity(testUser, updateDTO)).thenReturn(testUser);
        when(userMapper.update(testUser)).thenReturn(1);
        when(userConverter.toDto(testUser)).thenReturn(createUserDTO());

        // 执行测试
        UserDTO result = userService.updateUser(updateDTO);

        // 验证结果
        assertNotNull(result);

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(userMapper).selectByEmail("update@example.com");
        verify(userConverter).updateEntity(testUser, updateDTO);
        verify(userMapper).update(testUser);
    }

    @Test
    void testUpdateUser_NotFound() {
        // 修改updateDTO的ID为999L以匹配测试预期
        updateDTO.setId(999L);

        // 设置mock行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateUser(updateDTO);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testUpdateUser_SuperAdmin() {
        // 设置超级管理员用户
        User superAdminUser = createTestUser();
        superAdminUser.setRole(UserRole.SUPER_ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(superAdminUser);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateUser(updateDTO);
                        });

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        assertEquals("超级管理员不能被修改", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testUpdateUser_EmailConflict() {
        // 设置mock行为
        User anotherUser = createTestUser();
        anotherUser.setId(2L);
        anotherUser.setEmail("update@example.com");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectByEmail("update@example.com")).thenReturn(anotherUser);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateUser(updateDTO);
                        });

        assertEquals(ErrorCode.USER_NAME_EXISTS, exception.getErrorCode());
        assertEquals("邮箱已存在", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(userMapper).selectByEmail("update@example.com");
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testDeleteUser_Success() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    userService.deleteUser(1L);
                });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(userMapper).deleteById(1L);
    }

    @Test
    void testDeleteUser_NotFound() {
        // 设置mock行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.deleteUser(999L);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteUser_SuperAdmin() {
        // 设置超级管理员用户
        User superAdminUser = createTestUser();
        superAdminUser.setRole(UserRole.SUPER_ADMIN.name());
        when(userMapper.selectById(1L)).thenReturn(superAdminUser);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.deleteUser(1L);
                        });

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        assertEquals("超级管理员不能被删除", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void testUpdatePassword_Success() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userMapper.updatePassword(1L, "encoded_new_password")).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    userService.updatePassword(1L, "new_password");
                });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).encode("new_password");
        verify(userMapper).updatePassword(1L, "encoded_new_password");
    }

    @Test
    void testUpdatePassword_UserNotFound() {
        // 设置mock行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updatePassword(999L, "new_password");
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_Success() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userMapper.updatePassword(1L, "encoded_new_password")).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(
                () -> {
                    userService.updateOwnPassword(1L, updatePasswordDTO);
                });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches("old_password", "encoded_password");
        verify(passwordEncoder).encode("new_password");
        verify(userMapper).updatePassword(1L, "encoded_new_password");
    }

    @Test
    void testUpdateOwnPassword_UserNotFound() {
        // 设置mock行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateOwnPassword(999L, updatePasswordDTO);
                        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_OldPasswordWrong() {
        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(false);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateOwnPassword(1L, updatePasswordDTO);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("旧密码不正确", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches("old_password", "encoded_password");
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_SamePassword() {
        // 设置新旧密码相同
        updatePasswordDTO.setNewPassword("old_password");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updateOwnPassword(1L, updatePasswordDTO);
                        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("新密码不能与旧密码相同", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches("old_password", "encoded_password");
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUid("test_uid");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setPassword("encoded_password");
        user.setRole(UserRole.USER.name());
        user.setStatus(1);
        return user;
    }

    private UserDTO createUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setId(testUser.getId());
        dto.setUid(testUser.getUid());
        dto.setNickname(testUser.getNickname());
        dto.setEmail(testUser.getEmail());
        dto.setRole(testUser.getRole());
        dto.setStatus(testUser.getStatus());
        return dto;
    }

    @Test
    @DisplayName("管理员试图修改其他管理员密码失败")
    void testUpdatePasswordByAdmin_AdminUpdateAdminFailed() {
        // 准备管理员用户
        UserDTO adminUser = new UserDTO();
        adminUser.setId(2L);
        adminUser.setRole(UserRole.ADMIN.name());

        // 准备目标用户（管理员）
        User targetUser = createTestUser();
        targetUser.setRole(UserRole.ADMIN.name());

        // 设置mock行为
        when(userMapper.selectById(1L)).thenReturn(targetUser);

        // 执行测试并验证异常
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> {
                            userService.updatePasswordByAdmin(
                                    1L, adminUpdatePasswordDTO, adminUser);
                        });

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        assertEquals("管理员不能修改其他管理员的密码", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }
}
