package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.MachineCreateDTO;
import com.hinadt.miaocha.domain.dto.MachineDTO;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

/** 机器实体与DTO转换器 */
@Component
public class MachineConverter implements Converter<MachineInfo, MachineDTO> {

    private final UserMapper userMapper;
    private final LogstashMachineMapper logstashMachineMapper;

    public MachineConverter(UserMapper userMapper, LogstashMachineMapper logstashMachineMapper) {
        this.userMapper = userMapper;
        this.logstashMachineMapper = logstashMachineMapper;
    }

    /** 将DTO转换为实体 */
    @Override
    public MachineInfo toEntity(MachineDTO dto) {
        if (dto == null) {
            return null;
        }

        MachineInfo entity = new MachineInfo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        // MachineDTO不包含password字段（安全考虑）
        // MachineInfo没有privateKey和description字段
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /** 将创建DTO转换为实体 */
    public MachineInfo toEntity(MachineCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        MachineInfo entity = new MachineInfo();
        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setSshKey(dto.getSshKey());

        return entity;
    }

    /** 将实体转换为DTO */
    @Override
    public MachineDTO toDto(MachineInfo entity) {
        if (entity == null) {
            return null;
        }

        MachineDTO dto = new MachineDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setIp(entity.getIp());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        // MachineDTO不包含password字段（安全考虑）
        // MachineInfo没有privateKey和description字段
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateUser(entity.getCreateUser());
        dto.setUpdateUser(entity.getUpdateUser());

        // 查询Logstash进程实例数量
        if (entity.getId() != null) {
            int logstashMachineCount = logstashMachineMapper.countByMachineId(entity.getId());
            dto.setLogstashMachineCount(logstashMachineCount);
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

    /** 使用DTO更新实体 */
    @Override
    public MachineInfo updateEntity(MachineInfo entity, MachineDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setIp(dto.getIp());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        // MachineDTO不包含password字段（安全考虑）
        // MachineInfo没有privateKey和description字段

        return entity;
    }

    /** 使用创建DTO更新实体 */
    public MachineInfo updateEntity(MachineInfo entity, MachineCreateDTO dto) {
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
