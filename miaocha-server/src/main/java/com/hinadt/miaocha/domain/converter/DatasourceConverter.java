package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.DatasourceCreateDTO;
import com.hinadt.miaocha.domain.dto.DatasourceDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.mapper.UserMapper;
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
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setDatabase(dto.getDatabase());
        entity.setJdbcParams(dto.getJdbcParams());
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
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setDatabase(dto.getDatabase());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    /** 将实体转换为DTO */
    @Override
    public DatasourceDTO toDto(DatasourceInfo entity) {
        return toDto(entity, false); // 默认不包含敏感信息
    }

    /** 将实体转换为DTO，可控制是否包含敏感信息 */
    public DatasourceDTO toDto(DatasourceInfo entity, boolean includeSensitiveData) {
        if (entity == null) {
            return null;
        }

        DatasourceDTO dto = new DatasourceDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setIp(entity.getIp());
        dto.setPort(entity.getPort());
        dto.setDatabase(entity.getDatabase());
        dto.setJdbcParams(entity.getJdbcParams());
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

    /** 使用DTO更新实体 */
    @Override
    public DatasourceInfo updateEntity(DatasourceInfo entity, DatasourceDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setDatabase(dto.getDatabase());
        entity.setJdbcParams(dto.getJdbcParams());

        return entity;
    }

    /** 使用创建DTO更新实体 */
    public DatasourceInfo updateEntity(DatasourceInfo entity, DatasourceCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setDatabase(dto.getDatabase());
        entity.setJdbcParams(dto.getJdbcParams());

        return entity;
    }
}
