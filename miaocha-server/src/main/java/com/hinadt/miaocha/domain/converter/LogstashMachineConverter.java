package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.dto.logstash.LogstashMachineDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashMachineDetailDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Logstash机器关联实体与DTO转换器 */
@Component
public class LogstashMachineConverter implements Converter<LogstashMachine, LogstashMachineDTO> {

    private final MachineMapper machineMapper;
    private final UserMapper userMapper;
    private final ModuleInfoMapper moduleInfoMapper;

    public LogstashMachineConverter(
            MachineMapper machineMapper, UserMapper userMapper, ModuleInfoMapper moduleInfoMapper) {
        this.machineMapper = machineMapper;
        this.userMapper = userMapper;
        this.moduleInfoMapper = moduleInfoMapper;
    }

    /**
     * 根据LogstashProcess创建LogstashMachine实体 复制配置信息到LogstashMachine
     *
     * @param process LogstashProcess实体
     * @param machineId 机器ID
     * @param deployPath 部署路径
     * @return 新的LogstashMachine实体
     */
    public LogstashMachine createFromProcess(
            LogstashProcess process, Long machineId, String deployPath) {
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

        // 设置部署路径
        logstashMachine.setDeployPath(deployPath);

        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        logstashMachine.setCreateTime(now);
        logstashMachine.setUpdateTime(now);

        return logstashMachine;
    }

    /** 将DTO转换为实体 */
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
        entity.setDeployPath(dto.getDeployPath());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /** 将实体转换为DTO */
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
        dto.setDeployPath(entity.getDeployPath());
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

        // 转换枚举状态
        LogstashMachineState state = LogstashMachineState.valueOf(entity.getState());
        dto.setState(state.name());
        dto.setStateDescription(state.getDescription());

        // 获取机器信息
        try {
            MachineInfo machineInfo = machineMapper.selectById(entity.getMachineId());
            if (machineInfo != null) {
                dto.setMachineName(machineInfo.getName());
                dto.setMachineIp(machineInfo.getIp());
            }
        } catch (Exception e) {
            dto.setMachineName("未知机器");
            dto.setMachineIp("未知IP");
        }

        return dto;
    }

    /** 使用DTO更新实体 */
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
        entity.setDeployPath(dto.getDeployPath());

        return entity;
    }

    /**
     * 将LogstashMachine、LogstashProcess、Machine转换为LogstashMachineDetailDTO
     *
     * @param logstashMachine LogstashMachine实体
     * @param process LogstashProcess实体
     * @param machineInfo Machine实体
     * @return LogstashMachineDetailDTO
     */
    public LogstashMachineDetailDTO toDetailDTO(
            LogstashMachine logstashMachine, LogstashProcess process, MachineInfo machineInfo) {
        if (logstashMachine == null || process == null || machineInfo == null) {
            return null;
        }

        LogstashMachineDetailDTO detailDTO = new LogstashMachineDetailDTO();

        // 设置关联信息
        detailDTO.setId(logstashMachine.getId());
        detailDTO.setLogstashProcessId(process.getId());
        detailDTO.setMachineId(machineInfo.getId());

        // 设置进程信息
        detailDTO.setLogstashProcessName(process.getName());

        // 通过moduleId查询模块信息
        String moduleName = null;
        if (process.getModuleId() != null) {
            ModuleInfo moduleInfo = moduleInfoMapper.selectById(process.getModuleId());
            if (moduleInfo != null) {
                moduleName = moduleInfo.getName();
            }
        }
        detailDTO.setLogstashProcessModule(moduleName);

        detailDTO.setLogstashProcessDescription(null); // LogstashProcess中没有description字段
        detailDTO.setCustomPackagePath(null); // LogstashProcess中没有customPackagePath字段
        detailDTO.setProcessCreateTime(process.getCreateTime());
        detailDTO.setProcessUpdateTime(process.getUpdateTime());

        // 设置机器信息
        detailDTO.setMachineName(machineInfo.getName());
        detailDTO.setMachineIp(machineInfo.getIp());
        detailDTO.setMachinePort(machineInfo.getPort());
        detailDTO.setMachineUsername(machineInfo.getUsername());

        // 设置进程状态和配置信息
        detailDTO.setProcessPid(logstashMachine.getProcessPid());
        LogstashMachineState state = LogstashMachineState.valueOf(logstashMachine.getState());
        detailDTO.setState(state);
        detailDTO.setStateDescription(state.getDescription());

        // 设置机器特定的配置
        detailDTO.setConfigContent(logstashMachine.getConfigContent());
        detailDTO.setJvmOptions(logstashMachine.getJvmOptions());
        detailDTO.setLogstashYml(logstashMachine.getLogstashYml());

        // 设置时间信息
        detailDTO.setCreateTime(logstashMachine.getCreateTime());
        detailDTO.setUpdateTime(logstashMachine.getUpdateTime());

        // 设置审计字段信息
        detailDTO.setCreateUser(logstashMachine.getCreateUser());
        detailDTO.setUpdateUser(logstashMachine.getUpdateUser());

        // 查询用户昵称
        if (logstashMachine.getCreateUser() != null) {
            String createUserName =
                    userMapper.selectNicknameByEmail(logstashMachine.getCreateUser());
            detailDTO.setCreateUserName(createUserName);
        }

        if (logstashMachine.getUpdateUser() != null) {
            String updateUserName =
                    userMapper.selectNicknameByEmail(logstashMachine.getUpdateUser());
            detailDTO.setUpdateUserName(updateUserName);
        }

        // 设置部署路径（从实体中获取）
        detailDTO.setDeployPath(logstashMachine.getDeployPath());

        return detailDTO;
    }
}
