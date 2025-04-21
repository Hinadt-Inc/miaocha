package com.hina.log.service;

import com.hina.log.dto.auth.LoginRequestDTO;
import com.hina.log.dto.auth.LoginResponseDTO;
import com.hina.log.dto.user.UpdatePasswordDTO;
import com.hina.log.dto.user.UserCreateDTO;
import com.hina.log.dto.user.UserDTO;
import com.hina.log.dto.user.UserUpdateDTO;
import com.hina.log.entity.User;
import com.hina.log.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserMapper;
import com.hina.log.security.JwtUtils;
import com.hina.log.service.impl.UserServiceImpl;
import com.hina.log.converter.UserConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserCreateDTO createDTO;
    private UserUpdateDTO updateDTO;
    private LoginRequestDTO loginRequestDTO;
    private UpdatePasswordDTO updatePasswordDTO;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUid("test_uid");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setRole(UserRole.USER.name());
        testUser.setStatus(1);

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

        // 设置默认行为
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectByUid("test_uid")).thenReturn(testUser);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(jwtUtils.generateToken(anyString())).thenReturn("test_token");

        // 设置转换器行为 - more granular toDto implementation
        when(userConverter.toDto(any(User.class))).thenAnswer(invocation -> {
            User entity = invocation.getArgument(0);
            UserDTO dto = new UserDTO();
            dto.setId(entity.getId());
            dto.setUid(entity.getUid());
            dto.setNickname(entity.getNickname());
            dto.setEmail(entity.getEmail());
            dto.setRole(entity.getRole());
            dto.setStatus(entity.getStatus());
            return dto;
        });

        // Mock entity conversion from createDTO
        User newUser = new User();
        newUser.setNickname(createDTO.getNickname());
        newUser.setEmail(createDTO.getEmail());
        newUser.setRole(createDTO.getRole());
        when(userConverter.toEntity(any(UserCreateDTO.class))).thenReturn(newUser);

        // Mock entity conversion from updateDTO
        User updatedUser = new User();
        updatedUser.setId(updateDTO.getId());
        updatedUser.setNickname(updateDTO.getNickname());
        updatedUser.setEmail(updateDTO.getEmail());
        updatedUser.setRole(updateDTO.getRole());
        updatedUser.setStatus(updateDTO.getStatus());
        when(userConverter.toEntity(any(UserUpdateDTO.class))).thenReturn(updatedUser);

        // Mock update entity
        when(userConverter.updateEntity(any(User.class), any(UserUpdateDTO.class))).thenAnswer(invocation -> {
            User entity = invocation.getArgument(0);
            UserUpdateDTO dto = invocation.getArgument(1);
            entity.setNickname(dto.getNickname());
            entity.setEmail(dto.getEmail());
            entity.setRole(dto.getRole());
            entity.setStatus(dto.getStatus());
            return entity;
        });
    }

    @Test
    void testLogin_Success() {
        // 执行测试
        LoginResponseDTO response = userService.login(loginRequestDTO);

        // 验证结果
        assertNotNull(response);
        assertEquals("test_token", response.getToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getNickname(), response.getNickname());
        assertEquals(testUser.getRole(), response.getRole());

        // 验证调用
        verify(userMapper).selectByEmail(loginRequestDTO.getEmail());
        verify(passwordEncoder).matches(loginRequestDTO.getPassword(), testUser.getPassword());
        verify(jwtUtils).generateToken(testUser.getUid());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // 设置密码不匹配
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.login(loginRequestDTO);
        });

        assertEquals(ErrorCode.USER_PASSWORD_ERROR, exception.getErrorCode());
        verify(userMapper).selectByEmail(loginRequestDTO.getEmail());
        verify(passwordEncoder).matches(loginRequestDTO.getPassword(), testUser.getPassword());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void testLogin_UserNotFound() {
        // 设置用户不存在
        when(userMapper.selectByEmail(anyString())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.login(loginRequestDTO);
        });

        assertEquals(ErrorCode.USER_PASSWORD_ERROR, exception.getErrorCode());
        verify(userMapper).selectByEmail(loginRequestDTO.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void testGetUserById_Success() {
        // 执行测试
        UserDTO result = userService.getUserById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUid(), result.getUid());
        assertEquals(testUser.getNickname(), result.getNickname());

        // 验证调用
        verify(userMapper).selectById(1L);
    }

    @Test
    void testGetUserById_NotFound() {
        // 设置用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
    }

    @Test
    void testGetUserByUid_Success() {
        // 执行测试
        UserDTO result = userService.getUserByUid("test_uid");

        // 验证结果
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUid(), result.getUid());
        assertEquals(testUser.getNickname(), result.getNickname());

        // 验证调用
        verify(userMapper).selectByUid("test_uid");
    }

    @Test
    void testGetUserByUid_NotFound() {
        // 设置用户不存在
        when(userMapper.selectByUid("invalid_uid")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserByUid("invalid_uid");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectByUid("invalid_uid");
    }

    @Test
    void testGetAllUsers() {
        // 准备数据
        List<User> users = Arrays.asList(testUser);
        when(userMapper.selectAll()).thenReturn(users);

        // 执行测试
        List<UserDTO> result = userService.getAllUsers();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getId(), result.get(0).getId());

        // 验证调用
        verify(userMapper).selectAll();
    }

    @Test
    void testCreateUser_Success() {
        // 设置邮箱不存在
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // 执行测试
        UserDTO result = userService.createUser(createDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(createDTO.getNickname(), result.getNickname());
        assertEquals(createDTO.getEmail(), result.getEmail());
        assertEquals(createDTO.getRole(), result.getRole());
        assertEquals(1, result.getStatus());

        // 验证调用
        verify(userMapper).selectByEmail(createDTO.getEmail());
        verify(passwordEncoder).encode(createDTO.getPassword());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testCreateUser_EmailExists() {
        // 设置邮箱已存在
        when(userMapper.selectByEmail("new@example.com")).thenReturn(testUser);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(createDTO);
        });

        assertEquals(ErrorCode.USER_NAME_EXISTS, exception.getErrorCode());
        verify(userMapper).selectByEmail(createDTO.getEmail());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testCreateUser_InvalidRole() {
        // 设置无效角色
        createDTO.setRole("INVALID_ROLE");

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(createDTO);
        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(userMapper).selectByEmail(createDTO.getEmail());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // 设置邮箱不冲突
        when(userMapper.selectByEmail("update@example.com")).thenReturn(null);
        when(userMapper.update(any(User.class))).thenReturn(1);

        // 执行测试
        UserDTO result = userService.updateUser(updateDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(updateDTO.getNickname(), result.getNickname());
        assertEquals(updateDTO.getEmail(), result.getEmail());
        assertEquals(updateDTO.getRole(), result.getRole());
        assertEquals(updateDTO.getStatus(), result.getStatus());

        // 验证调用
        verify(userMapper).selectById(updateDTO.getId());
        verify(userMapper).selectByEmail(updateDTO.getEmail());
        verify(userMapper).update(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        // 设置用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);
        updateDTO.setId(999L);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(updateDTO);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(updateDTO.getId());
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testUpdateUser_SuperAdmin() {
        // 设置超级管理员
        testUser.setRole(UserRole.SUPER_ADMIN.name());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(updateDTO);
        });

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        verify(userMapper).selectById(updateDTO.getId());
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testUpdateUser_EmailConflict() {
        // 设置邮箱冲突
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("update@example.com");
        when(userMapper.selectByEmail("update@example.com")).thenReturn(anotherUser);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(updateDTO);
        });

        assertEquals(ErrorCode.USER_NAME_EXISTS, exception.getErrorCode());
        verify(userMapper).selectById(updateDTO.getId());
        verify(userMapper).selectByEmail(updateDTO.getEmail());
        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void testDeleteUser_Success() {
        // 设置删除成功
        when(userMapper.deleteById(1L)).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            userService.deleteUser(1L);
        });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(userMapper).deleteById(1L);
    }

    @Test
    void testDeleteUser_NotFound() {
        // 设置用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteUser_SuperAdmin() {
        // 设置超级管理员
        testUser.setRole(UserRole.SUPER_ADMIN.name());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteUser(1L);
        });

        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void testUpdatePassword_Success() {
        // 设置更新成功
        when(userMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            userService.updatePassword(1L, "new_password");
        });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).encode("new_password");
        verify(userMapper).updatePassword(eq(1L), anyString());
    }

    @Test
    void testUpdatePassword_UserNotFound() {
        // 设置用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updatePassword(999L, "new_password");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_Success() {
        // 设置旧密码匹配
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(userMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> {
            userService.updateOwnPassword(1L, updatePasswordDTO);
        });

        // 验证调用
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches(updatePasswordDTO.getOldPassword(), testUser.getPassword());
        verify(passwordEncoder).encode(updatePasswordDTO.getNewPassword());
        verify(userMapper).updatePassword(eq(1L), anyString());
    }

    @Test
    void testUpdateOwnPassword_UserNotFound() {
        // 设置用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateOwnPassword(999L, updatePasswordDTO);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).selectById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_OldPasswordWrong() {
        // 设置旧密码不匹配
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(false);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateOwnPassword(1L, updatePasswordDTO);
        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches(updatePasswordDTO.getOldPassword(), testUser.getPassword());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void testUpdateOwnPassword_SamePassword() {
        // 设置新旧密码相同
        updatePasswordDTO.setNewPassword("old_password");

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateOwnPassword(1L, updatePasswordDTO);
        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(userMapper).selectById(1L);
        verify(passwordEncoder).matches(updatePasswordDTO.getOldPassword(), testUser.getPassword());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }
}