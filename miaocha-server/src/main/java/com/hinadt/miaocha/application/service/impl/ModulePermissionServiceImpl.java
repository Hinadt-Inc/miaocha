package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO;
import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO.UserPermissionInfoDTO;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.UserModulePermission;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import com.hinadt.miaocha.domain.mapper.UserModulePermissionMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 模块权限服务实现类 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModulePermissionServiceImpl implements ModulePermissionService {

    private final LogstashProcessMapper logstashProcessMapper;
    private final UserMapper userMapper;
    private final DatasourceMapper datasourceMapper;
    private final UserModulePermissionMapper userModulePermissionMapper;
    private final com.hinadt.miaocha.domain.mapper.ModuleInfoMapper moduleInfoMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean hasModulePermission(Long userId, String module) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员和管理员拥有所有模块的权限
        String role = user.getRole();
        if (UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role)) {
            return true;
        }

        // 根据模块名称获取数据源ID
        Long datasourceId = getDatasourceIdByModule(module);

        // 检查用户是否拥有此模块的权限
        UserModulePermission permission =
                userModulePermissionMapper.select(userId, datasourceId, module);
        return permission != null;
    }

    @Override
    @Transactional
    public UserModulePermissionDTO grantModulePermission(Long userId, String module) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查模块是否存在
        if (!moduleExists(module)) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        // 根据模块名称获取数据源ID
        Long datasourceId = getDatasourceIdByModule(module);

        // 检查数据源是否存在
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 检查权限是否已存在
        UserModulePermission existingPermission =
                userModulePermissionMapper.select(userId, datasourceId, module);
        if (existingPermission != null) {
            // 权限已存在，直接返回
            return convertToDTO(existingPermission);
        }

        // 创建新的权限
        UserModulePermission permission = new UserModulePermission();
        permission.setUserId(userId);
        permission.setDatasourceId(datasourceId);
        permission.setModule(module);

        // 插入数据库
        userModulePermissionMapper.insert(permission);

        // 返回创建的权限DTO
        return convertToDTO(permission);
    }

    @Override
    @Transactional
    public void revokeModulePermission(Long userId, String module) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查模块是否存在
        if (!moduleExists(module)) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        // 根据模块名称获取数据源ID
        Long datasourceId = getDatasourceIdByModule(module);

        // 检查数据源是否存在
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 删除权限
        userModulePermissionMapper.delete(userId, datasourceId, module);
    }

    @Override
    public List<UserModulePermissionDTO> getUserModulePermissions(Long userId) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取用户的所有模块权限
        List<UserModulePermission> permissions = userModulePermissionMapper.selectByUser(userId);

        // 转换为DTO
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ModuleUsersPermissionDTO> getAllUsersModulePermissions() {
        // 获取所有用户的模块权限
        List<UserModulePermission> allPermissions = userModulePermissionMapper.selectAll();

        if (allPermissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有用户ID并批量查询用户信息
        List<Long> userIds =
                allPermissions.stream()
                        .map(UserModulePermission::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

        List<User> users = userMapper.selectByIds(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, user -> user));

        // 获取所有数据源ID并查询数据源信息
        List<Long> datasourceIds =
                allPermissions.stream()
                        .map(UserModulePermission::getDatasourceId)
                        .distinct()
                        .collect(Collectors.toList());

        Map<Long, DatasourceInfo> datasourceMap = new HashMap<>();
        for (Long datasourceId : datasourceIds) {
            DatasourceInfo datasource = datasourceMapper.selectById(datasourceId);
            if (datasource != null) {
                datasourceMap.put(datasourceId, datasource);
            }
        }

        // 按数据源和模块分组
        Map<String, ModuleUsersPermissionDTO> moduleGroupMap = new HashMap<>();

        for (UserModulePermission permission : allPermissions) {
            // 构建分组key: datasourceId + "_" + module
            String groupKey = permission.getDatasourceId() + "_" + permission.getModule();

            // 获取或创建ModuleUsersPermissionDTO
            ModuleUsersPermissionDTO moduleDto = moduleGroupMap.get(groupKey);
            if (moduleDto == null) {
                moduleDto = new ModuleUsersPermissionDTO();
                moduleDto.setDatasourceId(permission.getDatasourceId());
                moduleDto.setModule(permission.getModule());

                // 设置数据源名称
                DatasourceInfo datasource = datasourceMap.get(permission.getDatasourceId());
                if (datasource != null) {
                    moduleDto.setDatasourceName(datasource.getName());
                }

                moduleDto.setUsers(new ArrayList<>());
                moduleGroupMap.put(groupKey, moduleDto);
            }

            // 创建用户权限信息DTO
            User user = userMap.get(permission.getUserId());
            if (user != null) {
                UserPermissionInfoDTO userInfo = new UserPermissionInfoDTO();
                userInfo.setPermissionId(permission.getId());
                userInfo.setUserId(permission.getUserId());
                userInfo.setNickname(user.getNickname());
                userInfo.setEmail(user.getEmail());
                userInfo.setRole(user.getRole());

                // 格式化时间
                if (permission.getCreateTime() != null) {
                    userInfo.setCreateTime(permission.getCreateTime().format(DATE_TIME_FORMATTER));
                }
                if (permission.getUpdateTime() != null) {
                    userInfo.setUpdateTime(permission.getUpdateTime().format(DATE_TIME_FORMATTER));
                }

                // 添加到用户列表
                moduleDto.getUsers().add(userInfo);
            }
        }

        return new ArrayList<>(moduleGroupMap.values());
    }

    @Override
    @Transactional
    public List<UserModulePermissionDTO> batchGrantModulePermissions(
            Long userId, List<String> modules) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (modules == null || modules.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserModulePermissionDTO> result = new ArrayList<>();

        // 对每个模块授予权限
        for (String module : modules) {
            try {
                // 检查模块是否存在
                if (!moduleExists(module)) {
                    log.warn("模块不存在: {}", module);
                    continue;
                }

                // 根据模块名称获取数据源ID
                Long datasourceId = getDatasourceIdByModule(module);

                // 检查数据源是否存在
                DatasourceInfo datasourceInfo = datasourceMapper.selectById(datasourceId);
                if (datasourceInfo == null) {
                    log.warn("数据源不存在: {}", datasourceId);
                    continue;
                }

                // 检查权限是否已存在
                UserModulePermission existingPermission =
                        userModulePermissionMapper.select(userId, datasourceId, module);
                if (existingPermission != null) {
                    // 权限已存在，直接添加到结果中
                    result.add(convertToDTO(existingPermission));
                    continue;
                }

                // 创建新的权限
                UserModulePermission permission = new UserModulePermission();
                permission.setUserId(userId);
                permission.setDatasourceId(datasourceId);
                permission.setModule(module);

                // 插入数据库
                userModulePermissionMapper.insert(permission);

                // 添加到结果中
                result.add(convertToDTO(permission));
            } catch (Exception e) {
                log.error("授予模块权限失败: {}, 用户ID: {}, 错误: {}", module, userId, e.getMessage());
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void batchRevokeModulePermissions(Long userId, List<String> modules) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 批量撤销权限
        for (String module : modules) {
            try {
                revokeModulePermission(userId, module);
            } catch (Exception e) {
                log.error("撤销模块权限失败: userId={}, module={}", userId, module, e);
                // 继续处理其他模块
            }
        }
    }

    @Override
    public List<String> getUserUnauthorizedModules(Long userId) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员和管理员拥有所有模块的权限，返回空列表
        String role = user.getRole();
        if (UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role)) {
            return new ArrayList<>();
        }

        // 获取所有模块
        List<com.hinadt.miaocha.domain.entity.ModuleInfo> allModules = moduleInfoMapper.selectAll();

        // 获取用户已有的模块权限
        List<UserModulePermission> userPermissions =
                userModulePermissionMapper.selectByUser(userId);

        // 将用户已有的模块权限转换为模块名称集合
        Set<String> userModules =
                userPermissions.stream()
                        .map(UserModulePermission::getModule)
                        .collect(Collectors.toSet());

        // 筛选出用户没有权限的模块
        return allModules.stream()
                .map(com.hinadt.miaocha.domain.entity.ModuleInfo::getName)
                .filter(StringUtils::hasText) // 过滤掉空模块名
                .filter(module -> !userModules.contains(module)) // 过滤掉用户已有权限的模块
                .distinct() // 去重
                .collect(Collectors.toList());
    }

    /**
     * 检查模块是否存在
     *
     * @param module 模块名称
     * @return 是否存在
     */
    private boolean moduleExists(String module) {
        if (!StringUtils.hasText(module)) {
            return false;
        }

        com.hinadt.miaocha.domain.entity.ModuleInfo moduleInfo =
                moduleInfoMapper.selectByName(module);
        return moduleInfo != null;
    }

    /**
     * 根据模块名称获取数据源ID
     *
     * @param module 模块名称
     * @return 数据源ID
     */
    private Long getDatasourceIdByModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        com.hinadt.miaocha.domain.entity.ModuleInfo moduleInfo =
                moduleInfoMapper.selectByName(module);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        return moduleInfo.getDatasourceId();
    }

    /**
     * 将模块权限实体转换为DTO
     *
     * @param permission 模块权限实体
     * @return 模块权限DTO
     */
    private UserModulePermissionDTO convertToDTO(UserModulePermission permission) {
        if (permission == null) {
            return null;
        }

        UserModulePermissionDTO dto = new UserModulePermissionDTO();
        dto.setId(permission.getId());
        dto.setUserId(permission.getUserId());
        dto.setDatasourceId(permission.getDatasourceId());
        dto.setModule(permission.getModule());

        // 查询数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(permission.getDatasourceId());
        if (datasourceInfo != null) {
            dto.setDatasourceName(datasourceInfo.getName());
            dto.setDatabaseName(datasourceInfo.getDatabase());
        }

        if (permission.getCreateTime() != null) {
            dto.setCreateTime(permission.getCreateTime().format(DATE_TIME_FORMATTER));
        }

        if (permission.getUpdateTime() != null) {
            dto.setUpdateTime(permission.getUpdateTime().format(DATE_TIME_FORMATTER));
        }

        dto.setCreateUser(permission.getCreateUser());
        dto.setUpdateUser(permission.getUpdateUser());

        // 查询用户昵称
        if (permission.getCreateUser() != null) {
            String createUserName = userMapper.selectNicknameByEmail(permission.getCreateUser());
            dto.setCreateUserName(createUserName);
        }

        if (permission.getUpdateUser() != null) {
            String updateUserName = userMapper.selectNicknameByEmail(permission.getUpdateUser());
            dto.setUpdateUserName(updateUserName);
        }

        return dto;
    }

    @Override
    public List<UserModulePermissionDTO> getUserAccessibleModules(Long userId) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员和管理员拥有所有模块的权限
        String role = user.getRole();
        boolean isAdmin =
                UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role);

        List<UserModulePermissionDTO> result = new ArrayList<>();

        if (isAdmin) {
            // 管理员拥有所有模块的权限
            List<com.hinadt.miaocha.domain.entity.ModuleInfo> allModules =
                    moduleInfoMapper.selectAll();

            for (com.hinadt.miaocha.domain.entity.ModuleInfo moduleInfo : allModules) {
                UserModulePermissionDTO dto = new UserModulePermissionDTO();
                dto.setId(null); // 管理员没有特定的权限ID
                dto.setUserId(userId);
                dto.setDatasourceId(moduleInfo.getDatasourceId());
                dto.setModule(moduleInfo.getName());

                // 查询数据源信息
                DatasourceInfo datasourceInfo =
                        datasourceMapper.selectById(moduleInfo.getDatasourceId());
                if (datasourceInfo != null) {
                    dto.setDatasourceName(datasourceInfo.getName());
                    dto.setDatabaseName(datasourceInfo.getDatabase());
                }

                result.add(dto);
            }
        } else {
            // 非管理员，查询用户实际的模块权限
            List<UserModulePermission> permissions =
                    userModulePermissionMapper.selectByUser(userId);
            result = permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
        }

        return result;
    }
}
