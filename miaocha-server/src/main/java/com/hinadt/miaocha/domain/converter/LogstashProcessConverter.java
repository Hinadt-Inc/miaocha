package com.hinadt.miaocha.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.entity.*;
import com.hinadt.miaocha.infrastructure.mapper.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Converter between LogstashProcess entity and DTOs. */
@Component
public class LogstashProcessConverter implements Converter<LogstashProcess, LogstashProcessDTO> {

    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final ModuleInfoMapper moduleInfoMapper;
    private final DatasourceMapper datasourceMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Convert LogstashProcess entity to LogstashProcessResponseDTO including machine statuses.
     *
     * @param process Logstash process entity
     * @return response DTO including machine status info
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

        // Parse alert recipients JSON into list
        if (StringUtils.hasText(process.getAlertRecipients())) {
            try {
                List<String> recipients =
                        objectMapper.readValue(
                                process.getAlertRecipients(), new TypeReference<List<String>>() {});
                responseDTO.setAlertRecipients(recipients);
            } catch (Exception ignored) {
                responseDTO.setAlertRecipients(List.of());
            }
        } else {
            responseDTO.setAlertRecipients(List.of());
        }

        // Query user nicknames
        if (process.getCreateUser() != null) {
            String createUserName = userMapper.selectNicknameByEmail(process.getCreateUser());
            responseDTO.setCreateUserName(createUserName);
        }

        if (process.getUpdateUser() != null) {
            String updateUserName = userMapper.selectNicknameByEmail(process.getUpdateUser());
            responseDTO.setUpdateUserName(updateUserName);
        }

        // Fetch module info
        if (process.getModuleId() != null) {
            ModuleInfo moduleInfo = moduleInfoMapper.selectById(process.getModuleId());
            if (moduleInfo != null) {
                responseDTO.setModuleName(moduleInfo.getName());
                responseDTO.setTableName(moduleInfo.getTableName());

                // Fetch datasource info
                DatasourceInfo datasourceInfo =
                        datasourceMapper.selectById(moduleInfo.getDatasourceId());
                if (datasourceInfo != null) {
                    responseDTO.setDatasourceName(datasourceInfo.getName());
                }
            }
        }

        // Fetch related machine statuses
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
                statusInfo.setProcessPid(relation.getProcessPid());
                statusInfo.setDeployPath(relation.getDeployPath());
                statusInfo.setLastUpdateTime(relation.getUpdateTime());

                machineStatuses.add(statusInfo);
            }
        }

        responseDTO.setLogstashMachineStatusInfo(machineStatuses);
        return responseDTO;
    }

    /** Get human-readable state description. */
    private String getStateDescription(LogstashMachine machine) {
        LogstashMachineState state = LogstashMachineState.valueOf(machine.getState());

        switch (state) {
            case INITIALIZING:
                return "Initializing";
            case INITIALIZE_FAILED:
                return "Initialize failed";
            case NOT_STARTED:
                return "Not started";
            case STARTING:
                return "Starting";
            case START_FAILED:
                return "Start failed";
            case RUNNING:
                return "Running";
            case STOPPING:
                return "Stopping";
            case STOP_FAILED:
                return "Stop failed";
            default:
                return state.name();
        }
    }

    /** Convert DTO to entity. */
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
        // Audit fields are handled by MyBatis interceptor

        return entity;
    }

    /** Convert create DTO to entity. */
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
        // Audit fields are handled by MyBatis interceptor

        return entity;
    }

    /** Convert entity to DTO. */
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

        // Query user nicknames
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

    /** Update entity with DTO. */
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
        // Audit fields are handled by MyBatis interceptor

        return entity;
    }

    /** Update entity with create DTO. */
    public LogstashProcess updateEntity(LogstashProcess entity, LogstashProcessCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setName(dto.getName());
        entity.setModuleId(dto.getModuleId());
        entity.setConfigContent(dto.getConfigContent());
        entity.setJvmOptions(dto.getJvmOptions());
        entity.setLogstashYml(dto.getLogstashYml());
        // Audit fields are handled by MyBatis interceptor

        return entity;
    }
}
