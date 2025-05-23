package com.hina.log.domain.converter;

import com.hina.log.domain.dto.logstash.LogstashMachineDTO;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.Machine;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.domain.mapper.MachineMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Logstash机器关联实体与DTO转换器
 */
@Component
public class LogstashMachineConverter implements Converter<LogstashMachine, LogstashMachineDTO> {

    private final MachineMapper machineMapper;

    public LogstashMachineConverter(MachineMapper machineMapper) {
        this.machineMapper = machineMapper;
    }

    /**
     * 根据LogstashProcess创建LogstashMachine实体
     * 复制配置信息到LogstashMachine
     *
     * @param process LogstashProcess实体
     * @param machineId 机器ID
     * @return 新的LogstashMachine实体
     */
    public LogstashMachine createFromProcess(LogstashProcess process, Long machineId) {
        if (process == null || machineId == null) {
            return null;
        }

        LogstashMachine logstashMachine = new LogstashMachine();
        logstashMachine.setLogstashProcessId(process.getId());
        logstashMachine.setMachineId(machineId);
        logstashMachine.setState(LogstashMachineState.INITIALIZING.name());
        
        // 复制配置信息
        logstashMachine.setConfigContent(process.getConfigContent());
        logstashMachine.setJvmOptions(process.getJvmOptions());
        logstashMachine.setLogstashYml(process.getLogstashYml());
        
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        logstashMachine.setCreateTime(now);
        logstashMachine.setUpdateTime(now);
        
        return logstashMachine;
    }

    /**
     * 将DTO转换为实体
     */
    @Override
    public LogstashMachine toEntity(LogstashMachineDTO dto) {
        if (dto == null) {
            return null;
        }

        LogstashMachine entity = new LogstashMachine();
        entity.setId(dto.getId());
        entity.setLogstashProcessId(dto.getLogstashProcessId());
        entity.setMachineId(dto.getMachineId());
        entity.setProcessPid(dto.getProcessPid());
        entity.setState(dto.getState());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /**
     * 将实体转换为DTO
     */
    @Override
    public LogstashMachineDTO toDto(LogstashMachine entity) {
        if (entity == null) {
            return null;
        }

        LogstashMachineDTO dto = new LogstashMachineDTO();
        dto.setId(entity.getId());
        dto.setLogstashProcessId(entity.getLogstashProcessId());
        dto.setMachineId(entity.getMachineId());
        dto.setProcessPid(entity.getProcessPid());
        dto.setState(entity.getState());
        dto.setConfigContent(entity.getConfigContent());
        dto.setJvmOptions(entity.getJvmOptions());
        dto.setLogstashYml(entity.getLogstashYml());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        // 转换枚举状态
        LogstashMachineState state = LogstashMachineState.valueOf(entity.getState());
        dto.setState(state.name());
                dto.setStateDescription(state.getDescription());

        // 获取机器信息
        try {
            Machine machine = machineMapper.selectById(entity.getMachineId());
            if (machine != null) {
                dto.setMachineName(machine.getName());
                dto.setMachineIp(machine.getIp());
            }
        } catch (Exception e) {
            dto.setMachineName("未知机器");
            dto.setMachineIp("未知IP");
        }

        return dto;
    }

    /**
     * 使用DTO更新实体
     */
    @Override
    public LogstashMachine updateEntity(LogstashMachine entity, LogstashMachineDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setProcessPid(dto.getProcessPid());
        entity.setState(dto.getState());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());

        return entity;
    }
}
