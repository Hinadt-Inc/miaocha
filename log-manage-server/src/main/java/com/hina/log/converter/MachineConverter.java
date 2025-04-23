package com.hina.log.converter;

import com.hina.log.dto.MachineCreateDTO;
import com.hina.log.dto.MachineDTO;
import com.hina.log.entity.Machine;
import org.springframework.stereotype.Component;

/**
 * 机器实体与DTO转换器
 */
@Component
public class MachineConverter implements Converter<Machine, MachineDTO> {

    /**
     * 将DTO转换为实体
     */
    @Override
    public Machine toEntity(MachineDTO dto) {
        if (dto == null) {
            return null;
        }

        Machine entity = new Machine();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /**
     * 将创建DTO转换为实体
     */
    public Machine toEntity(MachineCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Machine entity = new Machine();
        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setSshKey(dto.getSshKey());

        return entity;
    }

    /**
     * 将实体转换为DTO
     */
    @Override
    public MachineDTO toDto(Machine entity) {
        if (entity == null) {
            return null;
        }

        MachineDTO dto = new MachineDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIp(entity.getIp());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    /**
     * 使用DTO更新实体
     */
    @Override
    public Machine updateEntity(Machine entity, MachineDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());

        return entity;
    }

    /**
     * 使用创建DTO更新实体
     */
    public Machine updateEntity(Machine entity, MachineCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setSshKey(dto.getSshKey());

        return entity;
    }
}