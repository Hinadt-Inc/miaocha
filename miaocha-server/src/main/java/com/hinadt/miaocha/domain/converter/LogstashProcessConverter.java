package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.mapper.DatasourceMapper;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.domain.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Logstash进程实体与DTO转换器 */
@Component
public class LogstashProcessConverter implements Converter<LogstashProcess, LogstashProcessDTO> {

    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final ModuleInfoMapper moduleInfoMapper;
    private final DatasourceMapper datasourceMapper;
    private final UserMapper userMapper;

    public LogstashProcessConverter(
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            ModuleInfoMapper moduleInfoMapper,
            DatasourceMapper datasourceMapper,
            UserMapper userMapper) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.moduleInfoMapper = moduleInfoMapper;
        this.datasourceMapper = datasourceMapper;
        this.userMapper = userMapper;
    }

    /**
     * 将LogstashProcess实体转换为LogstashProcessResponseDTO 包括机器状态信息
     *
     * @param process Logstash进程实体
     * @return 包含机器状态信息的响应DTO
     */
    public LogstashProcessResponseDTO toResponseDTO(LogstashProcess process) {
        if (process == null) {
            return null;
        }

        LogstashProcessResponseDTO responseDTO = new LogstashProcessResponseDTO();
        responseDTO.setId(process.getId());
        responseDTO.setName(process.getName());
        responseDTO.setModuleId(process.getModuleId());
        responseDTO.setConfigContent(process.getConfigContent());
        responseDTO.setJvmOptions(process.getJvmOptions());
        responseDTO.setLogstashYml(process.getLogstashYml());
        responseDTO.setCreateTime(process.getCreateTime());
        responseDTO.setUpdateTime(process.getUpdateTime());
        responseDTO.setCreateUser(process.getCreateUser());
        responseDTO.setUpdateUser(process.getUpdateUser());

        // 查询用户昵称
        if (process.getCreateUser() != null) {
            String createUserName = userMapper.selectNicknameByEmail(process.getCreateUser());
            responseDTO.setCreateUserName(createUserName);
        }

        if (process.getUpdateUser() != null) {
            String updateUserName = userMapper.selectNicknameByEmail(process.getUpdateUser());
            responseDTO.setUpdateUserName(updateUserName);
        }

        // 获取模块信息
        if (process.getModuleId() != null) {
            ModuleInfo moduleInfo = moduleInfoMapper.selectById(process.getModuleId());
            if (moduleInfo != null) {
                responseDTO.setModuleName(moduleInfo.getName());
                responseDTO.setTableName(moduleInfo.getTableName());

                // 获取数据源信息
                DatasourceInfo datasourceInfo =
                        datasourceMapper.selectById(moduleInfo.getDatasourceId());
                if (datasourceInfo != null) {
                    responseDTO.setDatasourceName(datasourceInfo.getName());
                }
            }
        }

        // 获取关联的机器状态
        List<LogstashMachine> machineRelations =
                logstashMachineMapper.selectByLogstashProcessId(process.getId());
        List<LogstashProcessResponseDTO.LogstashMachineStatusInfoDTO> machineStatuses =
                new ArrayList<>();

        for (LogstashMachine relation : machineRelations) {
            MachineInfo machineInfo = machineMapper.selectById(relation.getMachineId());
            if (machineInfo != null) {
                LogstashProcessResponseDTO.LogstashMachineStatusInfoDTO statusInfo =
                        new LogstashProcessResponseDTO.LogstashMachineStatusInfoDTO();
                statusInfo.setLogstashMachineId(relation.getId());
                statusInfo.setMachineId(machineInfo.getId());
                statusInfo.setMachineName(machineInfo.getName());
                statusInfo.setMachineIp(machineInfo.getIp());
                statusInfo.setState(LogstashMachineState.valueOf(relation.getState()));
                statusInfo.setStateDescription(getStateDescription(relation));

                machineStatuses.add(statusInfo);
            }
        }

        responseDTO.setMachineStatuses(machineStatuses);
        return responseDTO;
    }

    /** 获取状态描述信息 */
    private String getStateDescription(LogstashMachine machine) {
        LogstashMachineState state = LogstashMachineState.valueOf(machine.getState());

        switch (state) {
            case INITIALIZING:
                return "正在初始化";
            case INITIALIZE_FAILED:
                return "初始化失败";
            case NOT_STARTED:
                return "未启动";
            case STARTING:
                return "正在启动";
            case START_FAILED:
                return "启动失败";
            case RUNNING:
                return "运行中";
            case STOPPING:
                return "正在停止";
            case STOP_FAILED:
                return "停止失败";
            default:
                return state.name();
        }
    }

    /** 将DTO转换为实体 */
    @Override
    public LogstashProcess toEntity(LogstashProcessDTO dto) {
        if (dto == null) {
            return null;
        }

        LogstashProcess entity = new LogstashProcess();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setModuleId(dto.getModuleId());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /** 将创建DTO转换为实体 */
    public LogstashProcess toEntity(LogstashProcessCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        LogstashProcess entity = new LogstashProcess();
        entity.setName(dto.getName());
        entity.setModuleId(dto.getModuleId());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /** 将实体转换为DTO */
    @Override
    public LogstashProcessDTO toDto(LogstashProcess entity) {
        if (entity == null) {
            return null;
        }

        LogstashProcessDTO dto = new LogstashProcessDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setModuleId(entity.getModuleId());
        dto.setConfigContent(entity.getConfigContent());
        dto.setJvmOptions(entity.getJvmOptions());
        dto.setLogstashYml(entity.getLogstashYml());
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
    public LogstashProcess updateEntity(LogstashProcess entity, LogstashProcessDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setModuleId(dto.getModuleId());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }

    /** 使用创建DTO更新实体 */
    public LogstashProcess updateEntity(LogstashProcess entity, LogstashProcessCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setModuleId(dto.getModuleId());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        // 审计字段由MyBatis拦截器自动处理

        return entity;
    }
}
