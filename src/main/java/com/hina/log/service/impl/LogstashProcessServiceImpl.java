package com.hina.log.service.impl;

import com.hina.log.converter.LogstashProcessConverter;
import com.hina.log.converter.MachineConverter;
import com.hina.log.dto.LogstashProcessCreateDTO;
import com.hina.log.dto.LogstashProcessDTO;
import com.hina.log.dto.MachineDTO;
import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashMachine;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.enums.LogstashProcessState;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.DatasourceMapper;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.mapper.MachineMapper;
import com.hina.log.service.LogstashProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Logstash进程管理服务实现类
 */
@Service
public class LogstashProcessServiceImpl implements LogstashProcessService {
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessServiceImpl.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final DatasourceMapper datasourceMapper;
    private final com.hina.log.service.logstash.LogstashProcessService logstashDeployService;
    private final LogstashProcessConverter logstashProcessConverter;
    private final MachineConverter machineConverter;

    public LogstashProcessServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            DatasourceMapper datasourceMapper,
            @Qualifier("logstashDeployServiceImpl") com.hina.log.service.logstash.LogstashProcessService logstashDeployService,
            LogstashProcessConverter logstashProcessConverter, MachineConverter machineConverter) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.datasourceMapper = datasourceMapper;
        this.logstashDeployService = logstashDeployService;
        this.logstashProcessConverter = logstashProcessConverter;
        this.machineConverter = machineConverter;
    }

    @Override
    @Transactional
    public LogstashProcessDTO createLogstashProcess(LogstashProcessCreateDTO dto) {
        // 参数校验
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "参数不能为空");
        }

        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程名称不能为空");
        }

        if (!StringUtils.hasText(dto.getModule())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        if (dto.getDatasourceId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据源ID不能为空");
        }

        if (dto.getMachineIds() == null || dto.getMachineIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要选择一台机器");
        }

        // 检查进程名称是否已存在
        if (logstashProcessMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NAME_EXISTS);
        }

        // 检查数据源是否存在
        if (datasourceMapper.selectById(dto.getDatasourceId()) == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "指定的数据源不存在");
        }

        // 创建进程记录
        LogstashProcess process = logstashProcessConverter.toEntity(dto);
        process.setState(LogstashProcessState.NOT_STARTED.name());
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());
        logstashProcessMapper.insert(process);

        // 创建进程与机器的关联关系
        for (Long machineId : dto.getMachineIds()) {
            if (machineMapper.selectById(machineId) == null) {
                throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
            }

            LogstashMachine logstashMachine = new LogstashMachine();
            logstashMachine.setLogstashProcessId(process.getId());
            logstashMachine.setMachineId(machineId);
            logstashMachineMapper.insert(logstashMachine);
        }

        return getLogstashProcess(process.getId());
    }

    @Override
    @Transactional
    public LogstashProcessDTO updateLogstashProcess(Long id, LogstashProcessCreateDTO dto) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "参数不能为空");
        }

        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程名称不能为空");
        }

        if (!StringUtils.hasText(dto.getModule())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        if (dto.getDatasourceId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据源ID不能为空");
        }

        if (dto.getMachineIds() == null || dto.getMachineIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要选择一台机器");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查进程是否正在运行，运行中的进程不能修改
        if (LogstashProcessState.RUNNING.name().equals(process.getState()) ||
                LogstashProcessState.STARTING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "运行中的进程不能修改，请先停止进程");
        }

        // 检查进程名称是否已被其他进程使用
        LogstashProcess existingProcess = logstashProcessMapper.selectByName(dto.getName());
        if (existingProcess != null && !existingProcess.getId().equals(id)) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NAME_EXISTS);
        }

        // 检查数据源是否存在
        if (datasourceMapper.selectById(dto.getDatasourceId()) == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "指定的数据源不存在");
        }

        // 更新进程记录
        process = logstashProcessConverter.updateEntity(process, dto);
        process.setId(id);
        process.setUpdateTime(LocalDateTime.now());
        logstashProcessMapper.update(process);

        // 更新进程与机器的关联关系
        logstashMachineMapper.deleteByLogstashProcessId(id);
        for (Long machineId : dto.getMachineIds()) {
            if (machineMapper.selectById(machineId) == null) {
                throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
            }

            LogstashMachine logstashMachine = new LogstashMachine();
            logstashMachine.setLogstashProcessId(id);
            logstashMachine.setMachineId(machineId);
            logstashMachineMapper.insert(logstashMachine);
        }

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public void deleteLogstashProcess(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查进程是否正在运行，运行中的进程不能删除
        if (LogstashProcessState.RUNNING.name().equals(process.getState()) ||
                LogstashProcessState.STARTING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "运行中的进程不能删除，请先停止进程");
        }

        // 删除与进程相关的所有任务和步骤
        // 1. 获取最近的任务，并删除相关步骤信息
        Optional<TaskDetailDTO> latestTask = logstashDeployService.getLatestProcessTaskDetail(id);
        if (latestTask.isPresent()) {
            logger.info("删除进程关联的任务: {}", latestTask.get().getTaskId());
            deleteProcessTask(latestTask.get().getTaskId());
        }

        // 2. 删除与进程相关的所有机器关联
        logstashMachineMapper.deleteByLogstashProcessId(id);
        logger.info("已删除进程与机器的关联关系: {}", id);

        // 3. 删除进程记录
        logstashProcessMapper.deleteById(id);
        logger.info("已删除Logstash进程: {}", id);
    }

    /**
     * 删除进程关联的任务及步骤
     */
    private void deleteProcessTask(String taskId) {
        try {
            // 先删除任务步骤
            logstashDeployService.deleteTaskSteps(taskId);
            // 再删除任务
            logstashDeployService.deleteTask(taskId);
        } catch (Exception e) {
            logger.error("删除任务失败: {}", taskId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除任务数据失败: " + e.getMessage());
        }
    }

    @Override
    public LogstashProcessDTO getLogstashProcess(Long id) {
        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        return enhanceDTO(logstashProcessConverter.toDto(process));
    }

    @Override
    public List<LogstashProcessDTO> getAllLogstashProcesses() {
        return logstashProcessMapper.selectAll().stream()
                .map(logstashProcessConverter::toDto)
                .map(this::enhanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LogstashProcessDTO startLogstashProcess(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查进程状态
        if (LogstashProcessState.RUNNING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程已经在运行中");
        }

        if (LogstashProcessState.STARTING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程正在启动中");
        }

        // 检查配置是否完整
        if (!StringUtils.hasText(process.getConfigJson())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        // 获取进程关联的所有机器
        List<Machine> machines = getMachinesForProcess(id);
        if (machines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 异步部署并启动Logstash
        CompletableFuture<Boolean> future = logstashDeployService.deployAndStartAsync(process, machines);

        // 添加完成处理器，确保状态被正确更新
        future.thenAccept(success -> {
            if (!success) {
                // 如果部署失败，确保更新状态为失败状态
                logstashProcessMapper.updateState(id, LogstashProcessState.FAILED.name());
                logger.error("Logstash进程 [{}] 部署和启动失败", id);
            } else {
                logger.info("Logstash进程 [{}] 部署和启动成功", id);
                // 成功状态已在验证进程步骤中更新为RUNNING
            }
        });

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessDTO stopLogstashProcess(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查进程状态
        if (LogstashProcessState.NOT_STARTED.name().equals(process.getState()) ||
                LogstashProcessState.FAILED.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未运行，无需停止");
        }

        if (LogstashProcessState.STOPPING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程正在停止中，请稍后");
        }

        // 获取进程关联的所有机器
        List<Machine> machines = getMachinesForProcess(id);
        if (machines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 更新进程状态为停止中
        logstashProcessMapper.updateState(id, LogstashProcessState.STOPPING.name());

        // 异步停止Logstash
        CompletableFuture<Boolean> future = logstashDeployService.stopAsync(machines);

        // 使用thenAccept处理结果
        future.thenAccept(success -> {
            // 清除所有相关的进程PID
            List<LogstashMachine> logstashMachines = logstashMachineMapper.selectByLogstashProcessId(id);
            for (LogstashMachine logstashMachine : logstashMachines) {
                if (logstashMachine.getProcessPid() != null && !logstashMachine.getProcessPid().isEmpty()) {
                    // 清除PID
                    logstashMachineMapper.updateProcessPid(id, logstashMachine.getMachineId(), null);
                    logger.info("Cleared Logstash process PID for process {} on machine {}",
                            id, logstashMachine.getMachineId());
                }
            }

            // 更新进程最终状态
            updateProcessStateAfterStop(id, success);
        });

        return getLogstashProcess(id);
    }

    /**
     * 在停止操作完成后同步更新进程状态
     * 使用单独的事务更新最终状态
     */
    @Transactional
    public void updateProcessStateAfterStop(Long processId, boolean success) {
        String finalState = success ? LogstashProcessState.NOT_STARTED.name() : LogstashProcessState.FAILED.name();

        // 加锁更新，确保数据一致性
        logstashProcessMapper.updateState(processId, finalState);
        logger.info("Process {} state updated to {} after stop operation", processId, finalState);
    }

    /**
     * 查询进程任务状态
     *
     * @param id 进程ID
     * @return 任务状态信息
     */
    @Override
    public String getTaskStatus(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        return logstashDeployService.getProcessTaskStatus(id);
    }

    /**
     * 重试失败的任务
     *
     * @param id 进程ID
     * @return 重试操作结果
     */
    @Override
    @Transactional
    public boolean retryTask(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查进程状态，只有失败状态的进程可以重试
        if (!LogstashProcessState.FAILED.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "只有失败状态的进程可以重试");
        }

        // 重置进程状态为NOT_STARTED
        logstashProcessMapper.updateState(id, LogstashProcessState.NOT_STARTED.name());
        logger.info("重置进程 [{}] 状态为未启动，准备重新尝试启动", id);

        // 获取进程关联的所有机器
        List<Machine> machines = getMachinesForProcess(id);
        if (machines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 重新启动任务（与startLogstashProcess相同的逻辑，但不进行状态检查）
        CompletableFuture<Boolean> future = logstashDeployService.deployAndStartAsync(process, machines);

        // 添加完成处理器，确保状态被正确更新
        future.thenAccept(success -> {
            if (!success) {
                // 如果部署失败，确保更新状态为失败状态
                logstashProcessMapper.updateState(id, LogstashProcessState.FAILED.name());
                logger.error("Logstash进程 [{}] 重试部署和启动失败", id);
            } else {
                logger.info("Logstash进程 [{}] 重试部署和启动成功", id);
                // 成功状态已在验证进程步骤中更新为RUNNING
            }
        });

        return true;
    }

    /**
     * 查询进程任务执行状态 - 返回详细信息
     *
     * @param id 进程ID
     * @return 任务详情DTO
     */
    @Override
    public TaskDetailDTO getTaskDetailStatus(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 获取最新任务详情
        Optional<TaskDetailDTO> taskDetailOpt = logstashDeployService.getLatestProcessTaskDetail(id);
        if (!taskDetailOpt.isPresent()) {
            // 如果没有任务记录，返回空状态
            TaskDetailDTO emptyDetail = new TaskDetailDTO();
            emptyDetail.setBusinessId(id);
            emptyDetail.setName("未找到与进程关联的任务");
            emptyDetail.setStatus("无任务");
            emptyDetail.setTotalSteps(0);
            emptyDetail.setSuccessCount(0);
            emptyDetail.setFailedCount(0);
            emptyDetail.setSkippedCount(0);
            // 进度为0
            emptyDetail.setProgressPercentage(0);
            emptyDetail.setMachineProgressPercentages(Map.of());
            return emptyDetail;
        }

        TaskDetailDTO taskDetail = taskDetailOpt.get();

        // 计算总体进度百分比
        taskDetail.getProgressPercentage();

        // 计算每台机器的进度百分比
        taskDetail.getMachineProgressPercentages();

        return taskDetail;
    }

    /**
     * 增强DTO对象，添加额外信息
     */
    private LogstashProcessDTO enhanceDTO(LogstashProcessDTO dto) {
        if (dto == null) {
            return null;
        }

        // 设置状态描述
        try {
            LogstashProcessState state = LogstashProcessState.valueOf(dto.getState());
            dto.setStateDescription(state.getDescription());
        } catch (IllegalArgumentException e) {
            dto.setStateDescription("未知状态");
        }

        // 获取数据源名称
        try {
            dto.setDatasourceName(datasourceMapper.selectById(dto.getDatasourceId()).getName());
        } catch (Exception e) {
            dto.setDatasourceName("未知数据源");
        }

        // 获取机器列表
        List<MachineDTO> machines = new ArrayList<>();
        List<LogstashMachine> relations = logstashMachineMapper.selectByLogstashProcessId(dto.getId());
        for (LogstashMachine relation : relations) {
            Machine machine = machineMapper.selectById(relation.getMachineId());
            if (machine != null) {
                machines.add(machineConverter.toDto(machine));
            }
        }
        dto.setMachines(machines);

        return dto;
    }

    private List<Machine> getMachinesForProcess(Long processId) {
        List<LogstashMachine> relations = logstashMachineMapper.selectByLogstashProcessId(processId);
        List<Machine> machines = new ArrayList<>();
        for (LogstashMachine relation : relations) {
            Machine machine = machineMapper.selectById(relation.getMachineId());
            if (machine != null) {
                machines.add(machine);
            }
        }
        return machines;
    }
}