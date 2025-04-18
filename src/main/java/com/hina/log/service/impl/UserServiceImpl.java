package com.hina.log.service.impl;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.hina.log.dto.auth.LoginRequestDTO;
import com.hina.log.dto.auth.LoginResponseDTO;
import com.hina.log.dto.user.UpdatePasswordDTO;
import com.hina.log.dto.user.UserCreateDTO;
import com.hina.log.dto.user.UserUpdateDTO;
import com.hina.log.entity.User;
import com.hina.log.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserMapper;
import com.hina.log.security.JwtUtils;
import com.hina.log.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

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

        return LoginResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public User getUserByUid(String uid) {
        User user = userMapper.selectByUid(uid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    @Override
    @Transactional
    public User createUser(UserCreateDTO userCreateDTO) {
        // 检查邮箱是否已存在
        User existUser = userMapper.selectByEmail(userCreateDTO.getEmail());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USER_NAME_EXISTS, "邮箱已存在");
        }

        // 检查角色是否有效
        if (!isValidRole(userCreateDTO.getRole())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无效的角色");
        }

        User user = new User();
        user.setNickname(userCreateDTO.getNickname());
        user.setEmail(userCreateDTO.getEmail());
        user.setUid(generateUid());
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setRole(userCreateDTO.getRole());
        user.setStatus(1);

        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(UserUpdateDTO userUpdateDTO) {
        User user = userMapper.selectById(userUpdateDTO.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

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

        user.setNickname(userUpdateDTO.getNickname());
        user.setEmail(userUpdateDTO.getEmail());
        user.setRole(userUpdateDTO.getRole());
        user.setStatus(userUpdateDTO.getStatus());

        userMapper.update(user);
        return user;
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员不能被删除
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "超级管理员不能被删除");
        }

        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateOwnPassword(Long userId, UpdatePasswordDTO updatePasswordDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "旧密码不正确");
        }

        // 新旧密码不能相同
        if (updatePasswordDTO.getOldPassword().equals(updatePasswordDTO.getNewPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "新密码不能与旧密码相同");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
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