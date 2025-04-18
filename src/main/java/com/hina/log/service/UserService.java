package com.hina.log.service;

import com.hina.log.dto.auth.LoginRequestDTO;
import com.hina.log.dto.auth.LoginResponseDTO;
import com.hina.log.dto.auth.RefreshTokenRequestDTO;
import com.hina.log.dto.user.UserCreateDTO;
import com.hina.log.dto.user.UserUpdateDTO;
import com.hina.log.dto.user.UpdatePasswordDTO;
import com.hina.log.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponseDTO login(LoginRequestDTO loginRequest);

    /**
     * 刷新令牌
     *
     * @param refreshTokenRequest 刷新令牌请求
     * @return 登录响应
     */
    LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest);

    /**
     * 通过ID获取用户
     *
     * @param id 用户ID
     * @return 用户
     */
    User getUserById(Long id);

    /**
     * 通过UID获取用户
     *
     * @param uid 用户UID
     * @return 用户
     */
    User getUserByUid(String uid);

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 创建用户
     *
     * @param userCreateDTO 用户创建DTO
     * @return 创建的用户
     */
    User createUser(UserCreateDTO userCreateDTO);

    /**
     * 更新用户
     *
     * @param userUpdateDTO 用户更新DTO
     * @return 更新的用户
     */
    User updateUser(UserUpdateDTO userUpdateDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 修改密码
     *
     * @param id          用户ID
     * @param newPassword 新密码
     */
    void updatePassword(Long id, String newPassword);

    /**
     * 修改自己的密码
     *
     * @param userId            当前用户ID
     * @param updatePasswordDTO 密码更新DTO
     */
    void updateOwnPassword(Long userId, UpdatePasswordDTO updatePasswordDTO);
}