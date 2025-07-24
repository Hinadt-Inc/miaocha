package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.auth.LoginRequestDTO;
import com.hinadt.miaocha.domain.dto.auth.LoginResponseDTO;
import com.hinadt.miaocha.domain.dto.auth.RefreshTokenRequestDTO;
import com.hinadt.miaocha.domain.dto.user.AdminUpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UserCreateDTO;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import com.hinadt.miaocha.domain.dto.user.UserUpdateDTO;
import com.hinadt.miaocha.domain.entity.User;
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
     * @param currentUser 当前操作用户
     */
    void updatePasswordByAdmin(
            Long id, AdminUpdatePasswordDTO adminUpdatePasswordDTO, UserDTO currentUser);

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

    /**
     * OAuth 登录
     *
     * @param providerId 提供者ID
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 登录响应
     */
    LoginResponseDTO oauthLogin(String providerId, String code, String redirectUri);
}
