package com.hina.log.service.impl;

import com.hina.log.dto.permission.UserModulePermissionDTO;
import com.hina.log.dto.permission.UserPermissionModuleStructureDTO;
import com.hina.log.dto.permission.UserPermissionModuleStructureDTO.ModuleInfoDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.User;
import com.hina.log.entity.UserModulePermission;
import com.hina.log.entity.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.mapper.UserModulePermissionMapper;
import com.hina.log.service.ModulePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModulePermissionServiceImpl implements ModulePermissionService {

    private final LogstashProcessMapper logstashProcessMapper;
    private final UserMapper userMapper;
    private final DatasourceMapper datasourceMapper;
    private final UserModulePermissionMapper userModulePermissionMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        UserModulePermission permission = userModulePermissionMapper.select(userId, datasourceId, module);
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
        Datasource datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND);
        }

        // 检查权限是否已存在
        UserModulePermission existingPermission = userModulePermissionMapper.select(userId, datasourceId, module);
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
        Datasource datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
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
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserPermissionModuleStructureDTO> getUserAccessibleModules(Long userId) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取所有数据源
        List<Datasource> allDatasources = datasourceMapper.selectAll();
        if (allDatasources.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有模块
        List<LogstashProcess> allModules = logstashProcessMapper.selectAll();

        // 数据源ID -> 数据源对象的映射
        Map<Long, Datasource> datasourceMap = new HashMap<>();
        allDatasources.forEach(ds -> datasourceMap.put(ds.getId(), ds));

        // 结果容器
        Map<Long, UserPermissionModuleStructureDTO> resultMap = new HashMap<>();

        // 超级管理员和管理员拥有所有模块的权限
        String role = user.getRole();
        boolean isAdmin = UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role);

        if (isAdmin) {
            // 管理员拥有所有数据源的权限
            for (Datasource ds : allDatasources) {
                UserPermissionModuleStructureDTO structureDTO = new UserPermissionModuleStructureDTO();
                structureDTO.setDatasourceId(ds.getId());
                structureDTO.setDatasourceName(ds.getName());
                structureDTO.setDatabaseName(ds.getDatabase());

                // 获取所有模块
                List<ModuleInfoDTO> modules = new ArrayList<>();
                for (LogstashProcess process : allModules) {
                    if (StringUtils.hasText(process.getModule())) {
                        ModuleInfoDTO moduleInfo = new ModuleInfoDTO();
                        moduleInfo.setModuleName(process.getModule());
                        moduleInfo.setPermissionId(null); // 管理员没有特定的权限ID
                        modules.add(moduleInfo);
                    }
                }

                structureDTO.setModules(modules);
                resultMap.put(ds.getId(), structureDTO);
            }
        } else {
            // 非管理员，查询用户所有的模块权限
            // 获取用户的所有模块权限
            List<UserModulePermission> allPermissions = userModulePermissionMapper.selectByUser(userId);

            // 按数据源分组
            Map<Long, List<UserModulePermission>> permissionsByDatasource = allPermissions.stream()
                    .collect(Collectors.groupingBy(UserModulePermission::getDatasourceId));

            for (Datasource ds : allDatasources) {
                Long datasourceId = ds.getId();
                List<UserModulePermission> permissions = permissionsByDatasource.getOrDefault(datasourceId, new ArrayList<>());

                if (!permissions.isEmpty()) {
                    UserPermissionModuleStructureDTO structureDTO = new UserPermissionModuleStructureDTO();
                    structureDTO.setDatasourceId(datasourceId);
                    structureDTO.setDatasourceName(ds.getName());
                    structureDTO.setDatabaseName(ds.getDatabase());

                    // 添加模块信息
                    List<ModuleInfoDTO> modules = new ArrayList<>();
                    for (UserModulePermission permission : permissions) {
                        ModuleInfoDTO moduleInfo = new ModuleInfoDTO();
                        moduleInfo.setModuleName(permission.getModule());
                        moduleInfo.setPermissionId(permission.getId());
                        modules.add(moduleInfo);
                    }

                    structureDTO.setModules(modules);
                    resultMap.put(datasourceId, structureDTO);
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    @Override
    public List<UserModulePermissionDTO> getAllUsersModulePermissions() {
        // 获取所有用户的模块权限
        List<UserModulePermission> allPermissions = userModulePermissionMapper.selectAll();

        // 转换为DTO
        return allPermissions.stream()
                .map(this::convertToDTO)
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

        List<LogstashProcess> processes = logstashProcessMapper.selectAll();
        for (LogstashProcess process : processes) {
            if (module.equals(process.getModule())) {
                return true;
            }
        }

        return false;
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

        List<LogstashProcess> processes = logstashProcessMapper.selectAll();
        for (LogstashProcess process : processes) {
            if (module.equals(process.getModule())) {
                return process.getDatasourceId();
            }
        }

        throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
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

        if (permission.getCreateTime() != null) {
            dto.setCreateTime(permission.getCreateTime().format(DATE_TIME_FORMATTER));
        }

        if (permission.getUpdateTime() != null) {
            dto.setUpdateTime(permission.getUpdateTime().format(DATE_TIME_FORMATTER));
        }

        return dto;
    }
}
