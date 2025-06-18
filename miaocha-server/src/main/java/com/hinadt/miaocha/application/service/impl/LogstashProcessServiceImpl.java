package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.logstash.LogstashConfigSyncService;
import com.hinadt.miaocha.application.logstash.LogstashMachineConnectionValidator;
import com.hinadt.miaocha.application.logstash.LogstashProcessDeployService;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.util.UserContextUtil;
import com.hinadt.miaocha.domain.converter.LogstashMachineConverter;
import com.hinadt.miaocha.domain.converter.LogstashProcessConverter;
import com.hinadt.miaocha.domain.dto.logstash.*;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Logstash进程管理服务实现类 支持多实例部署，基于LogstashMachine架构 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogstashProcessServiceImpl implements LogstashProcessService {

    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final ModuleInfoMapper moduleInfoMapper;
    private final LogstashProcessDeployService logstashDeployService;
    private final LogstashProcessConverter logstashProcessConverter;
    private final LogstashMachineConverter logstashMachineConverter;
    private final LogstashConfigParser logstashConfigParser;
    private final TaskService taskService;
    private final LogstashConfigSyncService configSyncService;
    private final LogstashMachineConnectionValidator connectionValidator;

    @Override
    @Transactional
    public LogstashProcessResponseDTO createLogstashProcess(LogstashProcessCreateDTO dto) {
        validateCreateRequest(dto);

        LogstashProcess process = createAndSaveProcess(dto);
        List<LogstashMachine> instances = createLogstashMachineInstances(process, dto);

        initializeInstances(instances, process);
        syncConfigurationIfNeeded(process.getId(), dto);

        log.info("成功创建Logstash进程[{}]，包含{}个实例", process.getId(), instances.size());
        return toResponseDTO(process.getId());
    }

    @Override
    @Transactional
    public void deleteLogstashProcess(Long id) {
        LogstashProcess process = validateProcessExists(id);
        validateProcessCanBeDeleted(id);

        cleanupProcessResources(id);
        logstashProcessMapper.deleteById(id);

        log.info("成功删除Logstash进程[{}]", id);
    }

    @Override
    public LogstashProcessResponseDTO getLogstashProcess(Long id) {
        validateProcessExists(id);
        return toResponseDTO(id);
    }

    @Override
    public List<LogstashProcessResponseDTO> getAllLogstashProcesses() {
        return logstashProcessMapper.selectAll().stream()
                .map(logstashProcessConverter::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO startLogstashProcess(Long id) {
        LogstashProcess process = validateProcessExists(id);
        validateProcessConfig(process);

        List<LogstashMachine> startableInstances =
                getInstancesByStates(
                        id, LogstashMachineState.NOT_STARTED, LogstashMachineState.START_FAILED);

        validateInstancesNotEmpty(startableInstances, "没有可启动的实例");

        logstashDeployService.startInstances(startableInstances, process);
        log.info("启动Logstash进程[{}]，包含{}个实例", id, startableInstances.size());

        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO stopLogstashProcess(Long id) {
        validateProcessExists(id);

        List<LogstashMachine> stoppableInstances =
                getInstancesByStates(
                        id,
                        LogstashMachineState.RUNNING,
                        LogstashMachineState.STARTING,
                        LogstashMachineState.STOP_FAILED);

        validateInstancesNotEmpty(stoppableInstances, "没有可停止的实例");

        logstashDeployService.stopInstances(stoppableInstances);
        log.info("停止Logstash进程[{}]，包含{}个实例", id, stoppableInstances.size());

        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO forceStopLogstashProcess(Long id) {
        validateProcessExists(id);

        List<LogstashMachine> allInstances = logstashMachineMapper.selectByLogstashProcessId(id);
        validateInstancesNotEmpty(allInstances, "进程未关联任何实例");

        logstashDeployService.forceStopInstances(allInstances);
        log.warn("强制停止Logstash进程[{}]，涉及{}个实例", id, allInstances.size());

        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO updateLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto) {
        validateProcessExists(id);
        validateConfigUpdateRequest(dto);

        // 验证配置更新条件
        logstashDeployService
                .getConfigService()
                .validateConfigUpdateConditions(id, dto.getLogstashMachineIds());

        // 解析并准备配置
        ProcessConfigUpdate configUpdate = prepareConfigUpdate(dto);

        // 更新数据库配置
        logstashProcessMapper.updateConfigOnly(
                id,
                configUpdate.configContent(),
                configUpdate.jvmOptions(),
                configUpdate.logstashYml());

        // 同步配置到实例
        configSyncService.updateConfigForAllInstances(
                id,
                configUpdate.configContent(),
                configUpdate.jvmOptions(),
                configUpdate.logstashYml());

        // 部署配置到目标实例
        List<LogstashMachine> targetInstances = getTargetInstances(id, dto.getLogstashMachineIds());
        if (!targetInstances.isEmpty()) {
            logstashDeployService.updateInstancesConfig(
                    targetInstances,
                    configUpdate.configContent(),
                    configUpdate.jvmOptions(),
                    configUpdate.logstashYml());
        }

        log.info("成功更新Logstash进程[{}]配置", id);
        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO refreshLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto) {
        LogstashProcess process = validateProcessExists(id);
        validateProcessConfig(process);

        List<Long> targetInstanceIds = dto != null ? dto.getLogstashMachineIds() : null;

        // 验证配置刷新条件
        logstashDeployService
                .getConfigService()
                .validateConfigRefreshConditions(id, targetInstanceIds);

        // 同步配置到实例
        configSyncService.updateConfigForAllInstances(
                id, process.getConfigContent(), process.getJvmOptions(), process.getLogstashYml());

        // 刷新目标实例配置
        List<LogstashMachine> targetInstances = getTargetInstances(id, targetInstanceIds);

        if (!targetInstances.isEmpty()) {
            logstashDeployService.refreshInstancesConfig(targetInstances, process);
        }

        log.info("成功刷新Logstash进程[{}]配置到{}个实例", id, targetInstances.size());
        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO updateLogstashProcessMetadata(
            Long id, LogstashProcessUpdateDTO dto) {
        validateProcessExists(id);
        validateModuleExists(dto.getModuleId());

        // 通过上下文获取当前用户
        String currentUser = UserContextUtil.getCurrentUserEmail();

        int updateResult =
                logstashProcessMapper.updateMetadataOnly(
                        id, dto.getName(), dto.getModuleId(), currentUser);
        if (updateResult == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新进程信息失败");
        }

        log.info(
                "成功更新Logstash进程[{}]的元信息: name={}, moduleId={}",
                id,
                dto.getName(),
                dto.getModuleId());
        return toResponseDTO(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO scaleLogstashProcess(
            Long id, LogstashProcessScaleRequestDTO dto) {
        LogstashProcess process = validateProcessExists(id);
        validateScaleRequest(dto);

        if (dto.isScaleIn()) {
            return performScaleIn(process, dto);
        }
        if (dto.isScaleOut()) {
            return performScaleOut(process, dto);
        }
        return toResponseDTO(id);
    }

    // ==================== 私有辅助方法 ====================

    private void validateCreateRequest(LogstashProcessCreateDTO dto) {
        if (dto.getMachineIds() == null || dto.getMachineIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "机器列表不能为空");
        }

        validateModuleExists(dto.getModuleId());

        if (logstashProcessMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NAME_EXISTS);
        }

        if (StringUtils.hasText(dto.getConfigContent())) {
            validateLogstashConfig(dto.getConfigContent());
        }
    }

    private LogstashProcess createAndSaveProcess(LogstashProcessCreateDTO dto) {
        LogstashProcess process = logstashProcessConverter.toEntity(dto);
        logstashProcessMapper.insert(process);
        return process;
    }

    private List<LogstashMachine> createLogstashMachineInstances(
            LogstashProcess process, LogstashProcessCreateDTO dto) {
        List<MachineInfo> machines = validateAndGetMachines(dto.getMachineIds());

        return machines.stream()
                .map(
                        machine -> {
                            // 生成部署路径
                            String deployPath = dto.getCustomDeployPath();
                            if (!StringUtils.hasText(deployPath)) {
                                // 如果没有自定义路径，生成默认路径
                                deployPath =
                                        logstashDeployService.generateDefaultInstancePath(machine);
                            }

                            // 检查路径冲突：同一台机器的同一路径不能被任何实例占用
                            LogstashMachine existingInstance =
                                    logstashMachineMapper.selectByMachineAndPath(
                                            machine.getId(), deployPath);

                            if (existingInstance != null) {
                                throw new BusinessException(
                                        ErrorCode.LOGSTASH_MACHINE_ALREADY_ASSOCIATED,
                                        String.format(
                                                "部署路径[%s]在机器[%d]上已被进程[%d]的实例[%d]占用",
                                                deployPath,
                                                machine.getId(),
                                                existingInstance.getLogstashProcessId(),
                                                existingInstance.getId()));
                            }

                            // 创建新实例，直接使用确定的路径
                            LogstashMachine logstashMachine =
                                    logstashMachineConverter.createFromProcess(
                                            process, machine.getId(), deployPath);
                            logstashMachineMapper.insert(logstashMachine);
                            return logstashMachine;
                        })
                .collect(Collectors.toList());
    }

    private List<MachineInfo> validateAndGetMachines(List<Long> machineIds) {
        List<MachineInfo> machines =
                machineIds.stream().map(this::validateAndGetMachine).collect(Collectors.toList());

        // 验证每台机器的连接
        machines.forEach(connectionValidator::validateSingleMachineConnection);
        return machines;
    }

    private MachineInfo validateAndGetMachine(Long machineId) {
        MachineInfo machine = machineMapper.selectById(machineId);
        if (machine == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
        }
        return machine;
    }

    private void initializeInstances(List<LogstashMachine> instances, LogstashProcess process) {
        try {
            logstashDeployService.initializeInstances(instances, process);
        } catch (Exception e) {
            log.error("初始化LogstashMachine实例失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "初始化实例失败: " + e.getMessage());
        }
    }

    private void syncConfigurationIfNeeded(Long processId, LogstashProcessCreateDTO dto) {
        boolean needSyncJvmOptions = !StringUtils.hasText(dto.getJvmOptions());
        boolean needSyncLogstashYml = !StringUtils.hasText(dto.getLogstashYml());

        if (needSyncJvmOptions || needSyncLogstashYml) {
            configSyncService.syncConfigurationAsync(
                    processId, needSyncJvmOptions, needSyncLogstashYml);
        }
    }

    private LogstashProcess validateProcessExists(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        return process;
    }

    private void validateProcessCanBeDeleted(Long processId) {
        List<LogstashMachine> runningInstances =
                getInstancesByStates(
                        processId, LogstashMachineState.RUNNING, LogstashMachineState.STARTING);

        if (!runningInstances.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无法删除进程，存在运行中的实例。请先停止所有实例再删除。");
        }
    }

    private List<LogstashMachine> getInstancesByStates(
            Long processId, LogstashMachineState... states) {
        Set<String> stateNames =
                Set.of(states).stream().map(LogstashMachineState::name).collect(Collectors.toSet());

        return logstashMachineMapper.selectByLogstashProcessId(processId).stream()
                .filter(instance -> stateNames.contains(instance.getState()))
                .collect(Collectors.toList());
    }

    private void validateInstancesNotEmpty(List<LogstashMachine> instances, String message) {
        if (instances.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private void cleanupProcessResources(Long processId) {
        deleteProcessDirectories(processId);
        deleteProcessTasks(processId);
        deleteProcessInstances(processId);
    }

    private void deleteProcessDirectories(Long processId) {
        try {
            List<LogstashMachine> instances =
                    logstashMachineMapper.selectByLogstashProcessId(processId);
            if (!instances.isEmpty()) {
                CompletableFuture<Boolean> deleteFuture =
                        logstashDeployService.deleteInstancesDirectory(instances);
                boolean success = deleteFuture.get();

                log.info(
                        success ? "成功删除进程物理目录，进程ID: {}" : "删除进程物理目录出现部分失败，但会继续删除数据库记录，进程ID: {}",
                        processId);
            }
        } catch (Exception e) {
            log.error("删除进程物理目录时发生错误: {}", e.getMessage(), e);
        }
    }

    private void deleteProcessTasks(Long processId) {
        try {
            List<String> taskIds = taskService.getAllProcessTaskIds(processId);
            if (taskIds.isEmpty()) {
                log.info("没有找到与进程关联的任务，进程ID: {}", processId);
                return;
            }

            log.info("找到{}个与进程关联的任务，进程ID: {}", taskIds.size(), processId);
            taskIds.forEach(this::safeDeleteTask);
        } catch (Exception e) {
            log.error("删除进程相关任务时发生错误: {}", e.getMessage(), e);
        }
    }

    private void safeDeleteTask(String taskId) {
        try {
            taskService.deleteTask(taskId);
            log.debug("删除任务成功: {}", taskId);
        } catch (Exception e) {
            log.error("删除任务失败: {}, 错误: {}", taskId, e.getMessage());
        }
    }

    private void deleteProcessInstances(Long processId) {
        List<LogstashMachine> instances =
                logstashMachineMapper.selectByLogstashProcessId(processId);
        instances.forEach(instance -> logstashMachineMapper.deleteById(instance.getId()));
    }

    private void validateConfigUpdateRequest(LogstashProcessConfigUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "配置更新请求不能为空");
        }

        boolean hasUpdate =
                StringUtils.hasText(dto.getConfigContent())
                        || StringUtils.hasText(dto.getJvmOptions())
                        || StringUtils.hasText(dto.getLogstashYml());

        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要指定一项配置内容进行更新");
        }

        if (StringUtils.hasText(dto.getConfigContent())) {
            validateLogstashConfig(dto.getConfigContent());
        }
    }

    private void validateLogstashConfig(String configContent) {
        LogstashConfigParser.ValidationResult result =
                logstashConfigParser.validateConfig(configContent);
        if (!result.isValid()) {
            throw new BusinessException(result.getErrorCode(), result.getErrorMessage());
        }
    }

    private void validateProcessConfig(LogstashProcess process) {
        if (!StringUtils.hasText(process.getConfigContent())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        if (process.getModuleId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块ID不能为空，无法启动进程");
        }

        validateModuleExists(process.getModuleId());
    }

    private void validateModuleExists(Long moduleId) {
        if (moduleInfoMapper.selectById(moduleId) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "指定的模块不存在");
        }
    }

    private ProcessConfigUpdate prepareConfigUpdate(LogstashProcessConfigUpdateRequestDTO dto) {
        String configContent =
                StringUtils.hasText(dto.getConfigContent()) ? dto.getConfigContent() : null;
        String jvmOptions = StringUtils.hasText(dto.getJvmOptions()) ? dto.getJvmOptions() : null;
        String logstashYml =
                StringUtils.hasText(dto.getLogstashYml()) ? dto.getLogstashYml() : null;

        return new ProcessConfigUpdate(configContent, jvmOptions, logstashYml);
    }

    private List<LogstashMachine> getTargetInstances(Long processId, List<Long> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return logstashMachineMapper.selectByLogstashProcessId(processId);
        }

        return instanceIds.stream()
                .map(instanceId -> validateAndGetInstance(processId, instanceId))
                .collect(Collectors.toList());
    }

    private LogstashMachine validateAndGetInstance(Long processId, Long instanceId) {
        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND, "指定的LogstashMachine实例不存在: ID=" + instanceId);
        }
        if (!instance.getLogstashProcessId().equals(processId)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "LogstashMachine实例[" + instanceId + "]不属于进程[" + processId + "]");
        }
        return instance;
    }

    private LogstashProcessResponseDTO toResponseDTO(Long processId) {
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        return logstashProcessConverter.toResponseDTO(process);
    }

    // ==================== 扩缩容相关方法 ====================

    private void validateScaleRequest(LogstashProcessScaleRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "扩容/缩容请求不能为空");
        }

        try {
            dto.validate();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    private LogstashProcessResponseDTO performScaleOut(
            LogstashProcess process, LogstashProcessScaleRequestDTO dto) {
        if (dto.getAddMachineIds() == null || dto.getAddMachineIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "扩容时必须指定要添加的机器ID列表");
        }

        List<MachineInfo> newMachines = validateAndGetMachines(dto.getAddMachineIds());
        List<LogstashMachine> newInstances =
                createInstancesForScaleOut(process, newMachines, dto.getCustomDeployPath());

        initializeInstances(newInstances, process);

        log.info("Logstash进程[{}]扩容完成，新增{}个实例", process.getId(), newInstances.size());
        return toResponseDTO(process.getId());
    }

    private List<LogstashMachine> createInstancesForScaleOut(
            LogstashProcess process, List<MachineInfo> machines, String customDeployPath) {
        return machines.stream()
                .map(
                        machine -> {
                            // 生成部署路径
                            String deployPath = customDeployPath;
                            if (!StringUtils.hasText(deployPath)) {
                                // 如果没有自定义路径，生成默认路径
                                deployPath =
                                        logstashDeployService.generateDefaultInstancePath(machine);
                            }

                            // 检查路径冲突：同一台机器的同一路径不能被任何实例占用
                            LogstashMachine existingInstance =
                                    logstashMachineMapper.selectByMachineAndPath(
                                            machine.getId(), deployPath);

                            if (existingInstance != null) {
                                throw new BusinessException(
                                        ErrorCode.LOGSTASH_MACHINE_ALREADY_ASSOCIATED,
                                        String.format(
                                                "部署路径[%s]在机器[%d]上已被进程[%d]的实例[%d]占用",
                                                deployPath,
                                                machine.getId(),
                                                existingInstance.getLogstashProcessId(),
                                                existingInstance.getId()));
                            }

                            // 创建新实例，直接使用确定的路径
                            LogstashMachine logstashMachine =
                                    logstashMachineConverter.createFromProcess(
                                            process, machine.getId(), deployPath);
                            logstashMachineMapper.insert(logstashMachine);
                            return logstashMachine;
                        })
                .collect(Collectors.toList());
    }

    private LogstashProcessResponseDTO performScaleIn(
            LogstashProcess process, LogstashProcessScaleRequestDTO dto) {
        if (dto.getRemoveLogstashMachineIds() == null
                || dto.getRemoveLogstashMachineIds().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "缩容时必须指定要移除的LogstashMachine实例ID列表");
        }

        List<LogstashMachine> removeInstances =
                validateInstancesForScaleIn(process.getId(), dto.getRemoveLogstashMachineIds());

        if (!dto.getForceScale()) {
            validateInstancesNotRunningForScaleIn(removeInstances);
        } else {
            stopRunningInstancesForScaleIn(removeInstances);
        }

        cleanupScaleInResources(dto.getRemoveLogstashMachineIds(), removeInstances);

        log.info("Logstash进程[{}]缩容完成，成功移除{}个实例", process.getId(), removeInstances.size());
        return toResponseDTO(process.getId());
    }

    private List<LogstashMachine> validateInstancesForScaleIn(
            Long processId, List<Long> instanceIds) {
        List<LogstashMachine> allInstances =
                logstashMachineMapper.selectByLogstashProcessId(processId);

        if (allInstances.size() <= instanceIds.size()) {
            throw new BusinessException(
                    ErrorCode.LOGSTASH_CANNOT_SCALE_TO_ZERO,
                    String.format(
                            "缩容后必须至少保留一个LogstashMachine实例，当前共%d个实例，不能移除%d个",
                            allInstances.size(), instanceIds.size()));
        }

        return instanceIds.stream()
                .map(instanceId -> validateAndGetInstance(processId, instanceId))
                .collect(Collectors.toList());
    }

    private void validateInstancesNotRunningForScaleIn(List<LogstashMachine> instances) {
        List<LogstashMachine> runningInstances =
                instances.stream()
                        .filter(
                                instance -> {
                                    LogstashMachineState state =
                                            LogstashMachineState.valueOf(instance.getState());
                                    return state == LogstashMachineState.RUNNING
                                            || state == LogstashMachineState.STARTING;
                                })
                        .toList();

        if (!runningInstances.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.LOGSTASH_MACHINE_RUNNING_CANNOT_REMOVE,
                    "存在运行中的实例，不能在非强制模式下缩容。请先停止实例或使用强制模式。");
        }
    }

    private void stopRunningInstancesForScaleIn(List<LogstashMachine> instances) {
        List<LogstashMachine> runningInstances =
                getInstancesByStates(
                        instances, LogstashMachineState.RUNNING, LogstashMachineState.STARTING);

        if (!runningInstances.isEmpty()) {
            log.info("强制缩容模式：停止{}个运行中的LogstashMachine实例", runningInstances.size());
            try {
                logstashDeployService.stopInstances(runningInstances);
                Thread.sleep(5000); // 等待停止完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待停止操作被中断");
            } catch (Exception e) {
                log.warn("批量停止LogstashMachine实例失败: {}", e.getMessage());
            }
        }
    }

    private List<LogstashMachine> getInstancesByStates(
            List<LogstashMachine> instances, LogstashMachineState... states) {
        Set<String> stateNames =
                Set.of(states).stream().map(LogstashMachineState::name).collect(Collectors.toSet());

        return instances.stream()
                .filter(instance -> stateNames.contains(instance.getState()))
                .collect(Collectors.toList());
    }

    private void cleanupScaleInResources(List<Long> instanceIds, List<LogstashMachine> instances) {
        deleteTasksForInstances(instanceIds);
        deleteDirectoriesForInstances(instances);
        deleteInstanceRecords(instances);
    }

    private void deleteTasksForInstances(List<Long> instanceIds) {
        instanceIds.forEach(
                instanceId -> {
                    try {
                        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);
                        taskIds.forEach(this::safeDeleteTask);
                        log.info("已删除LogstashMachine实例[{}]的{}个任务记录", instanceId, taskIds.size());
                    } catch (Exception e) {
                        log.error(
                                "删除LogstashMachine实例[{}]的任务记录时发生异常: {}",
                                instanceId,
                                e.getMessage(),
                                e);
                    }
                });
    }

    private void deleteDirectoriesForInstances(List<LogstashMachine> instances) {
        try {
            logstashDeployService.deleteInstancesDirectory(instances);
            log.info("已删除{}个LogstashMachine实例的目录", instances.size());
        } catch (Exception e) {
            log.error("批量删除LogstashMachine实例目录时发生错误: {}", e.getMessage(), e);
        }
    }

    private void deleteInstanceRecords(List<LogstashMachine> instances) {
        instances.forEach(
                instance -> {
                    logstashMachineMapper.deleteById(instance.getId());
                    log.info("已删除LogstashMachine实例[{}]", instance.getId());
                });
    }

    // ==================== 单个LogstashMachine实例操作方法实现 ====================

    @Override
    @Transactional
    public void startLogstashInstance(Long instanceId) {
        LogstashMachine instance = validateInstanceExists(instanceId);
        LogstashProcess process = validateProcessExists(instance.getLogstashProcessId());
        validateProcessConfig(process);

        // 检查实例状态是否允许启动
        LogstashMachineState currentState = LogstashMachineState.valueOf(instance.getState());
        if (currentState != LogstashMachineState.NOT_STARTED
                && currentState != LogstashMachineState.START_FAILED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format(
                            "实例[%s]当前状态[%s]不允许启动，只有未启动或启动失败状态的实例才能启动",
                            instanceId, currentState.getDescription()));
        }

        List<LogstashMachine> instances = List.of(instance);
        logstashDeployService.startInstances(instances, process);
    }

    @Override
    @Transactional
    public void stopLogstashInstance(Long instanceId) {
        LogstashMachine instance = validateInstanceExists(instanceId);

        // 检查实例状态是否允许停止
        LogstashMachineState currentState = LogstashMachineState.valueOf(instance.getState());
        if (currentState != LogstashMachineState.RUNNING
                && currentState != LogstashMachineState.STOP_FAILED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format(
                            "实例[%s]当前状态[%s]不允许停止，只有运行中或停止失败状态的实例才能停止",
                            instanceId, currentState.getDescription()));
        }

        List<LogstashMachine> instances = List.of(instance);
        logstashDeployService.stopInstances(instances);
    }

    @Override
    @Transactional
    public void forceStopLogstashInstance(Long instanceId) {
        LogstashMachine instance = validateInstanceExists(instanceId);

        // 强制停止不需要状态检查，任何状态都可以强制停止
        List<LogstashMachine> instances = List.of(instance);
        logstashDeployService.forceStopInstances(instances);
    }

    @Override
    @Transactional
    public void reinitializeLogstashInstance(Long instanceId) {
        LogstashMachine instance = validateInstanceExists(instanceId);
        LogstashProcess process = validateProcessExists(instance.getLogstashProcessId());

        // 检查实例状态是否允许重新初始化
        LogstashMachineState currentState = LogstashMachineState.valueOf(instance.getState());
        if (currentState != LogstashMachineState.INITIALIZE_FAILED
                && currentState != LogstashMachineState.NOT_STARTED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format(
                            "实例[%s]当前状态[%s]不允许重新初始化，只有初始化失败或未启动状态的实例才能重新初始化",
                            instanceId, currentState.getDescription()));
        }

        List<LogstashMachine> instances = List.of(instance);
        logstashDeployService.initializeInstances(instances, process);
    }

    @Override
    public LogstashMachineDetailDTO getLogstashMachineDetail(Long logstashMachineId) {
        LogstashMachine logstashMachine = validateInstanceExists(logstashMachineId);
        LogstashProcess process = validateProcessExists(logstashMachine.getLogstashProcessId());
        MachineInfo machineInfo = validateAndGetMachine(logstashMachine.getMachineId());

        return logstashMachineConverter.toDetailDTO(logstashMachine, process, machineInfo);
    }

    /**
     * 验证LogstashMachine实例是否存在
     *
     * @param instanceId LogstashMachine实例ID
     * @return LogstashMachine实例
     */
    private LogstashMachine validateInstanceExists(Long instanceId) {
        if (instanceId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "LogstashMachine实例ID不能为空");
        }

        LogstashMachine instance = logstashMachineMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(
                    ErrorCode.LOGSTASH_MACHINE_NOT_FOUND, "LogstashMachine实例不存在: ID=" + instanceId);
        }

        return instance;
    }

    /** 进程配置更新记录 */
    private record ProcessConfigUpdate(
            String configContent, String jvmOptions, String logstashYml) {}
}
