package com.hina.log.converter;

import com.hina.log.dto.LogstashProcessCreateDTO;
import com.hina.log.dto.LogstashProcessDTO;
import com.hina.log.entity.LogstashProcess;
import org.springframework.stereotype.Component;

/**
 * Logstash进程实体与DTO转换器
 */
@Component
public class LogstashProcessConverter implements Converter<LogstashProcess, LogstashProcessDTO> {

    /**
     * 将DTO转换为实体
     */
    @Override
    public LogstashProcess toEntity(LogstashProcessDTO dto) {
        if (dto == null) {
            return null;
        }

        LogstashProcess entity = new LogstashProcess();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setModule(dto.getModule());
        entity.setConfigContent(dto.getConfigContent());
        entity.setDorisSql(dto.getDorisSql());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setState(dto.getState());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /**
     * 将创建DTO转换为实体
     */
    public LogstashProcess toEntity(LogstashProcessCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        LogstashProcess entity = new LogstashProcess();
        entity.setName(dto.getName());
        entity.setModule(dto.getModule());
        entity.setConfigContent(dto.getConfigContent());
        entity.setDorisSql(dto.getDorisSql());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());

        return entity;
    }

    /**
     * 将实体转换为DTO
     */
    @Override
    public LogstashProcessDTO toDto(LogstashProcess entity) {
        if (entity == null) {
            return null;
        }

        LogstashProcessDTO dto = new LogstashProcessDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setModule(entity.getModule());
        dto.setConfigContent(entity.getConfigContent());
        dto.setDorisSql(entity.getDorisSql());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setTableName(entity.getTableName());
        dto.setState(entity.getState());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    /**
     * 使用DTO更新实体
     */
    @Override
    public LogstashProcess updateEntity(LogstashProcess entity, LogstashProcessDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setModule(dto.getModule());
        entity.setConfigContent(dto.getConfigContent());
        entity.setDorisSql(dto.getDorisSql());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setState(dto.getState());

        return entity;
    }

    /**
     * 使用创建DTO更新实体
     */
    public LogstashProcess updateEntity(LogstashProcess entity, LogstashProcessCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setModule(dto.getModule());
        entity.setConfigContent(dto.getConfigContent());
        entity.setDorisSql(dto.getDorisSql());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());

        return entity;
    }
}