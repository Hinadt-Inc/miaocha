package com.hina.log.application.service.impl;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.hina.log.application.security.JwtUtils;
import com.hina.log.application.service.UserService;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.converter.UserConverter;
import com.hina.log.domain.dto.auth.LoginRequestDTO;
import com.hina.log.domain.dto.auth.LoginResponseDTO;
import com.hina.log.domain.dto.auth.RefreshTokenRequestDTO;
import com.hina.log.domain.dto.user.UpdatePasswordDTO;
import com.hina.log.domain.dto.user.UserCreateDTO;
import com.hina.log.domain.dto.user.UserDTO;
import com.hina.log.domain.dto.user.UserUpdateDTO;
import com.hina.log.domain.entity.User;
import com.hina.log.domain.entity.enums.UserRole;
import com.hina.log.domain.mapper.UserMapper;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户服务实现类 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserConverter userConverter;

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        User user = userMapper.selectByEmail(loginRequest.getEmail());
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getUid());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUid());

        long expiresAt = jwtUtils.getExpirationFromToken(token);
        long refreshExpiresAt = jwtUtils.getExpirationFromToken(refreshToken);

        return LoginResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .refreshExpiresAt(refreshExpiresAt)
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    @Override
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        try {
            // 验证刷新token并生成新的token
            String newToken = jwtUtils.generateTokenFromRefreshToken(refreshToken);
            if (newToken == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "无效的刷新令牌");
            }

            // 从刷新token中获取用户ID
            String uid = jwtUtils.getUidFromToken(refreshToken);
            User user = getUserEntityByUid(uid);

            // 同时生成新的刷新令牌
            String newRefreshToken = jwtUtils.generateRefreshToken(uid);

            // 获取过期时间
            long expiresAt = jwtUtils.getExpirationFromToken(newToken);
            long refreshExpiresAt = jwtUtils.getExpirationFromToken(newRefreshToken);

            return LoginResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken) // 返回新的refreshToken
                    .expiresAt(expiresAt)
                    .refreshExpiresAt(refreshExpiresAt)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .build();
        } catch (ExpiredJwtException e) {
            // 特殊处理刷新令牌过期的情况
            log.warn("Refresh token has expired: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "刷新令牌已过期，请重新登录");
        } catch (Exception e) {
            // 处理其他异常
            log.error("Error during token refresh: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "无效的刷新令牌");
        }
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = getUserEntityById(id);
        return userConverter.toDto(user);
    }

    @Override
    public UserDTO getUserByUid(String uid) {
        User user = getUserEntityByUid(uid);
        return userConverter.toDto(user);
    }

    @Override
    public User getUserEntityById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public User getUserEntityByUid(String uid) {
        User user = userMapper.selectByUid(uid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userMapper.selectAll();
        return users.stream().map(userConverter::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO createUser(UserCreateDTO userCreateDTO) {
        // 检查邮箱是否已存在
        User existUser = userMapper.selectByEmail(userCreateDTO.getEmail());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USER_NAME_EXISTS, "邮箱已存在");
        }

        // 检查角色是否有效
        if (!isValidRole(userCreateDTO.getRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无效的角色");
        }

        User user = userConverter.toEntity(userCreateDTO);
        user.setUid(generateUid());
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setStatus(1);

        userMapper.insert(user);
        return userConverter.toDto(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(UserUpdateDTO userUpdateDTO) {
        User user = getUserEntityById(userUpdateDTO.getId());

        // 超级管理员不能被修改
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "超级管理员不能被修改");
        }

        // 检查邮箱是否与其他用户冲突
        User existUser = userMapper.selectByEmail(userUpdateDTO.getEmail());
        if (existUser != null && !existUser.getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.USER_NAME_EXISTS, "邮箱已存在");
        }

        // 检查角色是否有效
        if (!isValidRole(userUpdateDTO.getRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无效的角色");
        }

        user = userConverter.updateEntity(user, userUpdateDTO);

        userMapper.update(user);
        return userConverter.toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);

        // 超级管理员不能被删除
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "超级管理员不能被删除");
        }

        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String newPassword) {
        User user = getUserEntityById(id);
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateOwnPassword(Long userId, UpdatePasswordDTO updatePasswordDTO) {
        User user = getUserEntityById(userId);

        // 验证旧密码
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "旧密码不正确");
        }

        // 新旧密码不能相同
        if (updatePasswordDTO.getOldPassword().equals(updatePasswordDTO.getNewPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "新密码不能与旧密码相同");
        }

        userMapper.updatePassword(
                userId, passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
    }

    /**
     * 生成唯一用户ID
     *
     * @return 用户ID
     */
    private String generateUid() {
        return NanoIdUtils.randomNanoId();
    }

    /**
     * 检查角色是否有效
     *
     * @param role 角色
     * @return 是否有效
     */
    private boolean isValidRole(String role) {
        try {
            UserRole.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
