package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO;
import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO.UserPermissionInfoDTO;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.UserModulePermission;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 模块权限转换器 */
@Component
@RequiredArgsConstructor
public class ModulePermissionConverter
        implements Converter<UserModulePermission, UserModulePermissionDTO> {

    private final UserMapper userMapper;
    private final DatasourceMapper datasourceMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public UserModulePermission toEntity(UserModulePermissionDTO dto) {
        if (dto == null) {
            return null;
        }

        UserModulePermission entity = new UserModulePermission();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setModule(dto.getModule());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    @Override
    public UserModulePermissionDTO toDto(UserModulePermission entity) {
        if (entity == null) {
            return null;
        }

        UserModulePermissionDTO dto = new UserModulePermissionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setModule(entity.getModule());

        // 查询数据源信息
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(entity.getDatasourceId());
        if (datasourceInfo != null) {
            dto.setDatasourceName(datasourceInfo.getName());
            dto.setDatabaseName(datasourceInfo.getDatabase());
        }

        // 格式化时间
        if (entity.getCreateTime() != null) {
            dto.setCreateTime(entity.getCreateTime().format(DATE_TIME_FORMATTER));
        }

        if (entity.getUpdateTime() != null) {
            dto.setUpdateTime(entity.getUpdateTime().format(DATE_TIME_FORMATTER));
        }

        // 设置审计字段
        dto.setCreateUser(entity.getCreateUser());
        dto.setUpdateUser(entity.getUpdateUser());

        // 查询用户昵称
        if (entity.getCreateUser() != null) {
            String createUserName = userMapper.selectNicknameByEmail(entity.getCreateUser());
            dto.setCreateUserName(createUserName);
        }

        if (entity.getUpdateUser() != null) {
            String updateUserName = userMapper.selectNicknameByEmail(entity.getUpdateUser());
            dto.setUpdateUserName(updateUserName);
        }

        return dto;
    }

    @Override
    public UserModulePermission updateEntity(
            UserModulePermission entity, UserModulePermissionDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setUserId(dto.getUserId());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setModule(dto.getModule());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /**
     * 将UserModulePermission实体列表转换为ModuleUsersPermissionDTO列表 按数据源和模块分组聚合
     *
     * @param permissions 权限实体列表
     * @param userMap 用户映射(userId -> User)
     * @param datasourceMap 数据源映射(datasourceId -> DatasourceInfo)
     * @return 模块用户权限聚合DTO列表
     */
    public List<ModuleUsersPermissionDTO> toModuleUsersPermissionDtos(
            List<UserModulePermission> permissions,
            Map<Long, User> userMap,
            Map<Long, DatasourceInfo> datasourceMap) {

        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 按数据源和模块分组
        Map<String, ModuleUsersPermissionDTO> moduleGroupMap = new HashMap<>();

        for (UserModulePermission permission : permissions) {
            // 构建分组key: datasourceId + "_" + module
            String groupKey = permission.getDatasourceId() + "_" + permission.getModule();

            // 获取或创建ModuleUsersPermissionDTO
            ModuleUsersPermissionDTO moduleDto = moduleGroupMap.get(groupKey);
            if (moduleDto == null) {
                moduleDto = createModuleUsersPermissionDTO(permission, datasourceMap);
                moduleGroupMap.put(groupKey, moduleDto);
            }

            // 创建用户权限信息DTO
            User user = userMap.get(permission.getUserId());
            if (user != null) {
                UserPermissionInfoDTO userInfo = createUserPermissionInfoDTO(permission, user);
                moduleDto.getUsers().add(userInfo);
            }
        }

        return new ArrayList<>(moduleGroupMap.values());
    }

    /**
     * 批量转换UserModulePermission实体列表为DTO列表
     *
     * @param entities 实体列表
     * @return DTO列表
     */
    public List<UserModulePermissionDTO> toDtos(List<UserModulePermission> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** 创建ModuleUsersPermissionDTO */
    private ModuleUsersPermissionDTO createModuleUsersPermissionDTO(
            UserModulePermission permission, Map<Long, DatasourceInfo> datasourceMap) {

        ModuleUsersPermissionDTO moduleDto = new ModuleUsersPermissionDTO();
        moduleDto.setDatasourceId(permission.getDatasourceId());
        moduleDto.setModule(permission.getModule());

        // 设置数据源名称
        DatasourceInfo datasource = datasourceMap.get(permission.getDatasourceId());
        if (datasource != null) {
            moduleDto.setDatasourceName(datasource.getName());
        }

        moduleDto.setUsers(new ArrayList<>());
        return moduleDto;
    }

    /** 创建UserPermissionInfoDTO */
    private UserPermissionInfoDTO createUserPermissionInfoDTO(
            UserModulePermission permission, User user) {
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

        // 设置审计字段
        userInfo.setCreateUser(permission.getCreateUser());
        userInfo.setUpdateUser(permission.getUpdateUser());

        // 查询并设置用户昵称
        if (permission.getCreateUser() != null) {
            User createUser = userMapper.selectByEmail(permission.getCreateUser());
            if (createUser != null) {
                userInfo.setCreateUserName(createUser.getNickname());
            }
        }

        if (permission.getUpdateUser() != null) {
            User updateUser = userMapper.selectByEmail(permission.getUpdateUser());
            if (updateUser != null) {
                userInfo.setUpdateUserName(updateUser.getNickname());
            }
        }

        return userInfo;
    }

    /**
     * 为管理员创建UserModulePermissionDTO 管理员没有特定的权限ID，但可以访问所有模块
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @param module 模块名称
     * @param datasource 数据源信息
     * @return 权限DTO
     */
    public UserModulePermissionDTO createAdminPermissionDto(
            Long userId, Long datasourceId, String module, DatasourceInfo datasource) {

        UserModulePermissionDTO dto = new UserModulePermissionDTO();
        dto.setId(null); // 管理员没有特定的权限ID
        dto.setUserId(userId);
        dto.setDatasourceId(datasourceId);
        dto.setModule(module);

        if (datasource != null) {
            dto.setDatasourceName(datasource.getName());
            dto.setDatabaseName(datasource.getDatabase());
        }

        return dto;
    }
}
