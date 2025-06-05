package com.hina.log.application.service;

import com.hina.log.domain.dto.auth.LoginRequestDTO;
import com.hina.log.domain.dto.auth.LoginResponseDTO;
import com.hina.log.domain.dto.auth.RefreshTokenRequestDTO;
import com.hina.log.domain.dto.user.AdminUpdatePasswordDTO;
import com.hina.log.domain.dto.user.UpdatePasswordDTO;
import com.hina.log.domain.dto.user.UserCreateDTO;
import com.hina.log.domain.dto.user.UserDTO;
import com.hina.log.domain.dto.user.UserUpdateDTO;
import com.hina.log.domain.entity.User;
import java.util.List;

/** 用户服务接口 */
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
     * @return 用户DTO
     */
    UserDTO getUserById(Long id);

    /**
     * 通过UID获取用户
     *
     * @param uid 用户UID
     * @return 用户DTO
     */
    UserDTO getUserByUid(String uid);

    /**
     * 获取所有用户
     *
     * @return 用户DTO列表
     */
    List<UserDTO> getAllUsers();

    /**
     * 创建用户
     *
     * @param userCreateDTO 用户创建DTO
     * @return 创建的用户DTO
     */
    UserDTO createUser(UserCreateDTO userCreateDTO);

    /**
     * 更新用户
     *
     * @param userUpdateDTO 用户更新DTO
     * @return 更新的用户DTO
     */
    UserDTO updateUser(UserUpdateDTO userUpdateDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 修改密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     */
    void updatePassword(Long id, String newPassword);

    /**
     * 管理员修改用户密码
     *
     * @param id 用户ID
     * @param adminUpdatePasswordDTO 管理员密码更新DTO
     */
    void updatePasswordByAdmin(Long id, AdminUpdatePasswordDTO adminUpdatePasswordDTO);

    /**
     * 修改自己的密码
     *
     * @param userId 当前用户ID
     * @param updatePasswordDTO 密码更新DTO
     */
    void updateOwnPassword(Long userId, UpdatePasswordDTO updatePasswordDTO);

    /**
     * 获取用户实体（内部使用）
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User getUserEntityById(Long id);

    /**
     * 通过UID获取用户实体（内部使用）
     *
     * @param uid 用户UID
     * @return 用户实体
     */
    User getUserEntityByUid(String uid);
}
