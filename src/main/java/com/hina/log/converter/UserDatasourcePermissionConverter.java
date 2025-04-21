package com.hina.log.converter;

import com.hina.log.dto.permission.UserDatasourcePermissionDTO;
import com.hina.log.entity.UserDatasourcePermission;
import org.springframework.stereotype.Component;

/**
 * 用户数据源权限实体与DTO转换器
 */
@Component
public class UserDatasourcePermissionConverter
        implements Converter<UserDatasourcePermission, UserDatasourcePermissionDTO> {

    /**
     * 将DTO转换为实体
     */
    @Override
    public UserDatasourcePermission toEntity(UserDatasourcePermissionDTO dto) {
        if (dto == null) {
            return null;
        }

        UserDatasourcePermission entity = new UserDatasourcePermission();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /**
     * 将实体转换为DTO
     */
    @Override
    public UserDatasourcePermissionDTO toDto(UserDatasourcePermission entity) {
        if (entity == null) {
            return null;
        }

        UserDatasourcePermissionDTO dto = new UserDatasourcePermissionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setTableName(entity.getTableName());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    /**
     * 使用DTO更新实体
     */
    @Override
    public UserDatasourcePermission updateEntity(UserDatasourcePermission entity, UserDatasourcePermissionDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setUserId(dto.getUserId());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());

        return entity;
    }

    /**
     * 创建新的权限实体
     */
    public UserDatasourcePermission createEntity(Long userId, Long datasourceId, String tableName) {
        UserDatasourcePermission entity = new UserDatasourcePermission();
        entity.setUserId(userId);
        entity.setDatasourceId(datasourceId);
        entity.setTableName(tableName);

        return entity;
    }
}