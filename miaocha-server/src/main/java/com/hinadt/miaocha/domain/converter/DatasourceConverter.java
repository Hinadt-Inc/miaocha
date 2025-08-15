package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.dto.DatasourceUpdateDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

/** 数据源实体与DTO转换器 */
@Component
public class DatasourceConverter implements Converter<DatasourceInfo, DatasourceDTO> {

    private final UserMapper userMapper;

    public DatasourceConverter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 将DTO转换为实体 */
    @Override
    public DatasourceInfo toEntity(DatasourceDTO dto) {
        if (dto == null) {
            return null;
        }

        DatasourceInfo entity = new DatasourceInfo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setDescription(dto.getDescription());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /** 将创建DTO转换为实体 */
    public DatasourceInfo toEntity(DatasourceCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        DatasourceInfo entity = new DatasourceInfo();
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    /** 将实体转换为DTO */
    @Override
    public DatasourceDTO toDto(DatasourceInfo entity) {
        if (entity == null) {
            return null;
        }

        DatasourceDTO dto = new DatasourceDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setJdbcUrl(entity.getJdbcUrl());
        dto.setUsername(entity.getUsername());
        dto.setDescription(entity.getDescription());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
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

    /**
     * 合并现有实体和更新DTO，创建一个用于连接测试的创建DTO.
     *
     * @param entity the existing entity
     * @param dto the update DTO
     * @return a new DatasourceCreateDTO for connection testing
     */
    public DatasourceCreateDTO toCreateDTO(DatasourceInfo entity, DatasourceUpdateDTO dto) {
        if (entity == null || dto == null) {
            return null;
        }

        DatasourceCreateDTO createDTO = new DatasourceCreateDTO();
        createDTO.setName(entity.getName());
        createDTO.setPassword(dto.getPassword());

        if (dto.getType() != null) {
            createDTO.setType(dto.getType());
        } else {
            createDTO.setType(entity.getType());
        }

        if (dto.getJdbcUrl() != null) {
            createDTO.setJdbcUrl(dto.getJdbcUrl());
        } else {
            createDTO.setJdbcUrl(entity.getJdbcUrl());
        }

        if (dto.getUsername() != null) {
            createDTO.setUsername(dto.getUsername());
        } else {
            createDTO.setUsername(entity.getUsername());
        }

        if (dto.getDescription() != null) {
            createDTO.setDescription(dto.getDescription());
        } else {
            createDTO.setDescription(entity.getDescription());
        }

        return createDTO;
    }

    /** 使用DTO更新实体 */
    @Override
    public DatasourceInfo updateEntity(DatasourceInfo entity, DatasourceDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    /** 使用创建DTO更新实体 */
    public DatasourceInfo updateEntity(DatasourceInfo entity, DatasourceCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    /** 使用更新DTO更新实体 */
    public DatasourceInfo updateEntity(DatasourceInfo entity, DatasourceUpdateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }
        if (dto.getJdbcUrl() != null) {
            entity.setJdbcUrl(dto.getJdbcUrl());
        }
        if (dto.getUsername() != null) {
            entity.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null) {
            entity.setPassword(dto.getPassword());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }

        return entity;
    }
}
