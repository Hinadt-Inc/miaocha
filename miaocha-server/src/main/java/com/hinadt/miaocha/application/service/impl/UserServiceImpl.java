package com.hinadt.miaocha.application.service.impl;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.application.service.UserService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.config.security.JwtUtils;
import com.hinadt.miaocha.domain.converter.UserConverter;
import com.hinadt.miaocha.domain.dto.auth.LoginRequestDTO;
import com.hinadt.miaocha.domain.dto.auth.LoginResponseDTO;
import com.hinadt.miaocha.domain.dto.auth.RefreshTokenRequestDTO;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import com.hinadt.miaocha.domain.dto.user.AdminUpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UpdatePasswordDTO;
import com.hinadt.miaocha.domain.dto.user.UserCreateDTO;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import com.hinadt.miaocha.domain.dto.user.UserUpdateDTO;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import com.hinadt.miaocha.spi.LdapAuthProvider;
import com.hinadt.miaocha.spi.OAuthProvider;
import com.hinadt.miaocha.spi.model.LdapUserInfo;
import com.hinadt.miaocha.spi.model.OAuthUserInfo;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户服务实现类 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String SYSTEM_LOGIN_TYPE = "system";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserConverter userConverter;
    private final ModulePermissionService modulePermissionService;
    private final UserModulePermissionMapper userModulePermissionMapper;

    public UserServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            UserConverter userConverter,
            ModulePermissionService modulePermissionService,
            UserModulePermissionMapper userModulePermissionMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.userConverter = userConverter;
        this.modulePermissionService = modulePermissionService;
        this.userModulePermissionMapper = userModulePermissionMapper;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // 如果指定了 providerId，使用对应的认证方式
        if (loginRequest.getProviderId() != null && !loginRequest.getProviderId().isEmpty()) {
            return loginWithProvider(loginRequest);
        }

        String loginIdentifier =
                loginRequest.getLoginIdentifier() != null
                        ? loginRequest.getLoginIdentifier()
                        : loginRequest.getEmail();

        User user = userMapper.selectByEmail(loginIdentifier);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_FORBIDDEN);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        String loginType = SYSTEM_LOGIN_TYPE;
        String token = jwtUtils.generateTokenWithUserInfo(user, loginType);
        String refreshToken = jwtUtils.generateRefreshTokenWithUserInfo(user, loginType);

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
                .loginType(loginType)
                .build();
    }

    /** 使用指定提供者进行登录 */
    private LoginResponseDTO loginWithProvider(LoginRequestDTO loginRequest) {
        String providerId = loginRequest.getProviderId();

        // 尝试 LDAP 认证提供者
        return authenticateWithLdapProvider(
                providerId, loginRequest.getLoginIdentifier(), loginRequest.getPassword());
    }

    /** 使用 LDAP 认证提供者进行登录 */
    private LoginResponseDTO authenticateWithLdapProvider(
            String requestedProviderId, String loginIdentifier, String password) {
        ServiceLoader<LdapAuthProvider> loader = ServiceLoader.load(LdapAuthProvider.class);

        Optional<LdapAuthProvider> providerOpt =
                StreamSupport.stream(loader.spliterator(), false)
                        .filter(LdapAuthProvider::isAvailable)
                        .filter(
                                provider ->
                                        requestedProviderId.equals(
                                                provider.getProviderInfo().getProviderId()))
                        .findFirst();

        if (providerOpt.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "未找到可用的提供者: " + requestedProviderId);
        }

        LdapAuthProvider provider = providerOpt.get();
        LdapUserInfo ldapUser = provider.authenticate(loginIdentifier, password);

        if (ldapUser == null || ldapUser.getEmail() == null || ldapUser.getEmail().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.USER_PASSWORD_ERROR,
                    provider.getProviderInfo().getDisplayName() + " LDAP 登录, 用户名或者密码错误");
        }

        // 查找或创建本地用户
        User user = userMapper.selectByEmail(ldapUser.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，请联系管理员创建账户");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_FORBIDDEN);
        }

        // 使用实际的 providerId 作为 loginType
        String loginType = provider.getProviderInfo().getProviderId();
        String token = jwtUtils.generateTokenWithUserInfo(user, loginType);
        String refreshToken = jwtUtils.generateRefreshTokenWithUserInfo(user, loginType);

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
                .loginType(loginType)
                .build();
    }

    @Override
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        try {
            // 验证刷新token是否有效
            jwtUtils.validateToken(refreshToken);

            String loginType = jwtUtils.getLoginTypeFromToken(refreshToken);

            // 从刷新token中获取uid，然后查询数据库获取最新用户信息
            String uid = jwtUtils.getUidFromToken(refreshToken);
            User user = getUserEntityByUid(uid);

            // 使用最新的用户信息生成新的token和刷新令牌
            String newToken = jwtUtils.generateTokenWithUserInfo(user, loginType);
            String newRefreshToken = jwtUtils.generateRefreshTokenWithUserInfo(user, loginType);

            // 获取过期时间
            long expiresAt = jwtUtils.getExpirationFromToken(newToken);
            long refreshExpiresAt = jwtUtils.getExpirationFromToken(newRefreshToken);

            return LoginResponseDTO.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresAt(expiresAt)
                    .refreshExpiresAt(refreshExpiresAt)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .loginType(loginType)
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
        List<UserDTO> userDTOs =
                users.stream().map(userConverter::toDto).collect(Collectors.toList());

        // 为每个用户填充模块权限信息
        for (UserDTO userDTO : userDTOs) {
            List<UserModulePermissionDTO> permissions =
                    modulePermissionService.getUserModulePermissions(userDTO.getId());
            userDTO.setModulePermissions(permissions);
        }

        // 按角色和更新时间排序
        return sortUsersByRoleAndUpdateTime(userDTOs);
    }

    /**
     * 按角色和更新时间排序用户列表 排序规则： 1. 先按角色优先级排序：超级管理员 > 管理员 > 普通用户 2. 在同一角色内按更新时间倒序排序
     *
     * @param userDTOs 用户列表
     * @return 排序后的用户列表
     */
    private List<UserDTO> sortUsersByRoleAndUpdateTime(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .sorted(this::compareByRoleAndUpdateTime)
                .collect(Collectors.toList());
    }

    /**
     * 用户排序比较器 先按角色优先级排序，角色相同时按更新时间倒序排序
     *
     * @param user1 用户1
     * @param user2 用户2
     * @return 比较结果
     */
    private int compareByRoleAndUpdateTime(UserDTO user1, UserDTO user2) {
        // 1. 先按角色优先级排序
        int roleComparison = compareByRolePriority(user1.getRole(), user2.getRole());
        if (roleComparison != 0) {
            return roleComparison;
        }

        // 2. 角色相同时，按更新时间倒序排序（最近更新的在前）
        if (user1.getUpdateTime() != null && user2.getUpdateTime() != null) {
            return user2.getUpdateTime().compareTo(user1.getUpdateTime());
        } else if (user1.getUpdateTime() != null) {
            return -1;
        } else if (user2.getUpdateTime() != null) {
            return 1;
        }
        return 0;
    }

    /**
     * 按角色优先级比较 超级管理员 > 管理员 > 普通用户
     *
     * @param role1 角色1
     * @param role2 角色2
     * @return 比较结果
     */
    private int compareByRolePriority(String role1, String role2) {
        int priority1 = getRolePriority(role1);
        int priority2 = getRolePriority(role2);
        return Integer.compare(priority1, priority2);
    }

    /**
     * 获取角色优先级 数值越小优先级越高
     *
     * @param role 角色
     * @return 优先级数值
     */
    private int getRolePriority(String role) {
        if (UserRole.SUPER_ADMIN.name().equals(role)) {
            return 1; // 最高优先级
        } else if (UserRole.ADMIN.name().equals(role)) {
            return 2; // 中等优先级
        } else if (UserRole.USER.name().equals(role)) {
            return 3; // 最低优先级
        }
        return 999; // 未知角色排在最后
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

        userModulePermissionMapper.deleteByUserId(id);
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String newPassword) {
        User user = getUserEntityById(id);
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updatePasswordByAdmin(
            Long id, AdminUpdatePasswordDTO adminUpdatePasswordDTO, UserDTO currentUser) {
        User targetUser = getUserEntityById(id);

        // 权限验证
        validatePasswordChangePermission(currentUser, targetUser);

        userMapper.updatePassword(id, passwordEncoder.encode(adminUpdatePasswordDTO.getPassword()));
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
     * 验证修改密码权限
     *
     * @param currentUser 当前操作用户
     * @param targetUser 目标用户
     */
    private void validatePasswordChangePermission(UserDTO currentUser, User targetUser) {
        String currentUserRole = currentUser.getRole();
        String targetUserRole = targetUser.getRole();

        // 超级管理员可以修改任何人的密码
        if (UserRole.SUPER_ADMIN.name().equals(currentUserRole)) {
            return;
        }

        // 管理员只能修改普通用户的密码，不能修改其他管理员和超级管理员的密码
        if (UserRole.ADMIN.name().equals(currentUserRole)) {
            if (UserRole.ADMIN.name().equals(targetUserRole)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "管理员不能修改其他管理员的密码");
            }
            if (UserRole.SUPER_ADMIN.name().equals(targetUserRole)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "管理员不能修改超级管理员的密码");
            }
            // 管理员可以修改普通用户的密码
            if (UserRole.USER.name().equals(targetUserRole)) {
                return;
            }
        }

        // 普通用户不能修改任何人的密码
        throw new BusinessException(ErrorCode.PERMISSION_DENIED, "您没有权限修改用户密码");
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

    @Override
    public LoginResponseDTO oauthLogin(String providerId, String code, String redirectUri) {
        ServiceLoader<OAuthProvider> loader = ServiceLoader.load(OAuthProvider.class);

        Optional<OAuthProvider> providerOpt =
                StreamSupport.stream(loader.spliterator(), false)
                        .filter(
                                p ->
                                        p.getProviderInfo()
                                                .getProviderId()
                                                .equalsIgnoreCase(providerId))
                        .filter(OAuthProvider::isAvailable)
                        .findFirst();

        if (providerOpt.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Unsupported provider: " + providerId);
        }

        OAuthProvider provider = providerOpt.get();
        OAuthUserInfo userInfo = provider.authenticate(code, redirectUri);

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Authentication failed or invalid user info");
        }

        User user = userMapper.selectByEmail(userInfo.getEmail());
        if (user == null) {
            // Create new user
            user = new User();
            user.setUid(userInfo.getUid() != null ? userInfo.getUid() : generateUid());
            user.setEmail(userInfo.getEmail());

            // 优先使用SPI提供的nickname，否则使用邮箱前缀
            String nickname = userInfo.getNickname();
            if (nickname == null || nickname.isEmpty()) {
                nickname = userInfo.getEmail().substring(0, userInfo.getEmail().indexOf('@'));
            }
            user.setNickname(nickname);

            user.setRole(UserRole.USER.name());
            user.setStatus(1);
            user.setPassword(
                    passwordEncoder.encode(UUID.randomUUID().toString())); // Random password
            userMapper.insert(user);
        }

        String token = jwtUtils.generateTokenWithUserInfo(user, providerId);
        String refreshToken = jwtUtils.generateRefreshTokenWithUserInfo(user, providerId);

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
                .loginType(providerId)
                .build();
    }
}
