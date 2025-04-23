package com.hina.log.converter;

import com.hina.log.dto.DatasourceCreateDTO;
import com.hina.log.dto.DatasourceDTO;
import com.hina.log.entity.Datasource;
import org.springframework.stereotype.Component;

/**
 * 数据源实体与DTO转换器
 */
@Component
public class DatasourceConverter implements Converter<Datasource, DatasourceDTO> {

    /**
     * 将DTO转换为实体
     */
    @Override
    public Datasource toEntity(DatasourceDTO dto) {
        if (dto == null) {
            return null;
        }

        Datasource entity = new Datasource();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setDatabase(dto.getDatabase());
        entity.setJdbcParams(dto.getJdbcParams());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /**
     * 将创建DTO转换为实体
     */
    public Datasource toEntity(DatasourceCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Datasource entity = new Datasource();
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

    /**
     * 将实体转换为DTO
     */
    @Override
    public DatasourceDTO toDto(Datasource entity) {
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
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    /**
     * 使用DTO更新实体
     */
    @Override
    public Datasource updateEntity(Datasource entity, DatasourceDTO dto) {
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

    /**
     * 使用创建DTO更新实体
     */
    public Datasource updateEntity(Datasource entity, DatasourceCreateDTO dto) {
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