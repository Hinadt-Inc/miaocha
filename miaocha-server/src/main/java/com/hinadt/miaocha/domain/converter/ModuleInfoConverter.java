package com.hinadt.miaocha.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoWithPermissionsDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO.UserPermissionInfoDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 模块信息实体与DTO转换器 */
@Component
@Slf4j
public class ModuleInfoConverter implements Converter<ModuleInfo, ModuleInfoDTO> {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ModuleInfoConverter(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /** 将创建请求DTO转换为实体 */
    public ModuleInfo toEntity(ModuleInfoCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        ModuleInfo entity = new ModuleInfo();
        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        // 注意：不设置dorisSql，这应该只能通过executeDorisSql方法设置

        return entity;
    }

    /** 将更新请求DTO转换为实体（不包括dorisSql） */
    public ModuleInfo toEntity(ModuleInfoUpdateDTO dto) {
        if (dto == null) {
            return null;
        }

        ModuleInfo entity = new ModuleInfo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        // 注意：不设置dorisSql，这应该只能通过executeDorisSql方法设置

        return entity;
    }

    /** 将响应DTO转换为实体 */
    @Override
    public ModuleInfo toEntity(ModuleInfoDTO dto) {
        if (dto == null) {
            return null;
        }

        ModuleInfo entity = new ModuleInfo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setDorisSql(dto.getDorisSql());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /** 将实体转换为响应DTO */
    @Override
    public ModuleInfoDTO toDto(ModuleInfo entity) {
        if (entity == null) {
            return null;
        }

        ModuleInfoDTO dto = new ModuleInfoDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setTableName(entity.getTableName());
        dto.setDorisSql(entity.getDorisSql());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateUser(entity.getCreateUser());
        dto.setUpdateUser(entity.getUpdateUser());

        // 转换查询配置JSON字符串为DTO对象
        if (entity.getQueryConfig() != null && !entity.getQueryConfig().trim().isEmpty()) {
            try {
                QueryConfigDTO queryConfigDTO =
                        objectMapper.readValue(entity.getQueryConfig(), QueryConfigDTO.class);
                dto.setQueryConfig(queryConfigDTO);
            } catch (JsonProcessingException e) {
                // 如果JSON解析失败，记录日志但不抛出异常，返回null
                // 这样可以保证在queryConfig字段有问题时仍能正常显示其他信息
                log.warn("解析查询配置JSON字符串为DTO对象失败: {}", e.getMessage());
                dto.setQueryConfig(null);
            }
        }

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

    /** 将实体转换为响应DTO，并设置数据源名称 */
    public ModuleInfoDTO toDto(ModuleInfo entity, DatasourceInfo datasourceInfo) {
        ModuleInfoDTO dto = toDto(entity);
        if (dto != null && datasourceInfo != null) {
            dto.setDatasourceName(datasourceInfo.getName());
        }
        return dto;
    }

    /** 使用响应DTO更新实体 */
    @Override
    public ModuleInfo updateEntity(ModuleInfo entity, ModuleInfoDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setDorisSql(dto.getDorisSql());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /** 使用创建请求DTO更新实体 */
    public ModuleInfo updateEntity(ModuleInfo entity, ModuleInfoCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        // 注意：不更新dorisSql，保持原有值

        return entity;
    }

    /** 使用更新请求DTO更新实体（不包括dorisSql） */
    public ModuleInfo updateEntity(ModuleInfo entity, ModuleInfoUpdateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        // 注意：不更新dorisSql，保持原有值

        return entity;
    }

    /** 将实体转换为包含权限信息的响应DTO */
    public ModuleInfoWithPermissionsDTO toWithPermissionsDto(
            ModuleInfo entity, DatasourceInfo datasourceInfo, List<UserPermissionInfoDTO> users) {
        if (entity == null) {
            return null;
        }

        ModuleInfoWithPermissionsDTO dto = new ModuleInfoWithPermissionsDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setTableName(entity.getTableName());
        dto.setDorisSql(entity.getDorisSql());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateUser(entity.getCreateUser());
        dto.setUpdateUser(entity.getUpdateUser());

        // 转换查询配置JSON字符串为DTO对象
        if (entity.getQueryConfig() != null && !entity.getQueryConfig().trim().isEmpty()) {
            try {
                QueryConfigDTO queryConfigDTO =
                        objectMapper.readValue(entity.getQueryConfig(), QueryConfigDTO.class);
                dto.setQueryConfig(queryConfigDTO);
            } catch (JsonProcessingException e) {
                // 如果JSON解析失败，记录日志但不抛出异常，返回null
                log.warn("解析查询配置JSON字符串为DTO对象失败: {}", e.getMessage());
                dto.setQueryConfig(null);
            }
        }

        // 设置数据源名称
        if (datasourceInfo != null) {
            dto.setDatasourceName(datasourceInfo.getName());
        }

        // 查询用户昵称
        if (entity.getCreateUser() != null) {
            String createUserName = userMapper.selectNicknameByEmail(entity.getCreateUser());
            dto.setCreateUserName(createUserName);
        }

        if (entity.getUpdateUser() != null) {
            String updateUserName = userMapper.selectNicknameByEmail(entity.getUpdateUser());
            dto.setUpdateUserName(updateUserName);
        }

        // 设置权限用户列表
        dto.setUsers(users);

        return dto;
    }
}
