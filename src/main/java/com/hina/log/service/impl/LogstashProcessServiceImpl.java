package com.hina.log.service.impl;

import com.hina.log.converter.LogstashProcessConverter;
import com.hina.log.converter.MachineConverter;
import com.hina.log.converter.TaskStepsGroupConverter;
import com.hina.log.converter.TaskSummaryConverter;
import com.hina.log.dto.*;
import com.hina.log.entity.*;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.logstash.LogstashProcessDeployService;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.logstash.enums.TaskStatus;
import com.hina.log.mapper.*;
import com.hina.log.service.LogstashProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
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
    private final LogstashProcessDeployService logstashDeployService;
    private final LogstashProcessConverter logstashProcessConverter;
    private final MachineConverter machineConverter;
    private final LogstashTaskMapper taskMapper;
    private final LogstashTaskMachineStepMapper stepMapper;
    private final TaskSummaryConverter taskSummaryConverter;
    private final TaskStepsGroupConverter taskStepsGroupConverter;

    public LogstashProcessServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            DatasourceMapper datasourceMapper,
            @Qualifier("logstashDeployServiceImpl") LogstashProcessDeployService logstashDeployService,
            LogstashProcessConverter logstashProcessConverter,
            MachineConverter machineConverter,
            LogstashTaskMapper taskMapper,
            LogstashTaskMachineStepMapper stepMapper,
            TaskSummaryConverter taskSummaryConverter,
            TaskStepsGroupConverter taskStepsGroupConverter) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.datasourceMapper = datasourceMapper;
        this.logstashDeployService = logstashDeployService;
        this.logstashProcessConverter = logstashProcessConverter;
        this.machineConverter = machineConverter;
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.taskSummaryConverter = taskSummaryConverter;
        this.taskStepsGroupConverter = taskStepsGroupConverter;
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
        // 初始状态为初始化中状态
        process.setState(LogstashProcessState.INITIALIZING.name());
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

        // 获取进程对应的机器
        List<Machine> machines = getMachinesForProcess(process.getId());

        // 执行进程环境初始化
        logstashDeployService.initializeProcessAsync(process, machines);

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

        // 获取进程对应的机器
        List<Machine> machines = getMachinesForProcess(id);
        if (machines.isEmpty()) {
            logger.warn("进程没有关联的机器，进程ID: {}", id);
        } else {
            // 检查进程是否正在运行，运行中的进程不能删除
            if (LogstashProcessState.RUNNING.name().equals(process.getState()) ||
                    LogstashProcessState.STARTING.name().equals(process.getState())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "运行中的进程不能删除，请先停止进程");
            }

            // 删除进程物理目录 - 直接调用，不依赖状态管理
            try {
                CompletableFuture<Boolean> deleteFuture = logstashDeployService.deleteProcessDirectory(id, machines);
                boolean success = deleteFuture.get(); // 等待删除完成
                if (!success) {
                    logger.warn("删除进程物理目录出现部分失败，但会继续删除数据库记录，进程ID: {}", id);
                } else {
                    logger.info("成功删除进程物理目录，进程ID: {}", id);
                }
            } catch (Exception e) {
                logger.error("删除进程物理目录时发生错误: {}", e.getMessage(), e);
                // 继续进行数据库记录删除
            }
        }

        // 删除与进程相关的所有任务和步骤
        try {
            // 获取所有与进程相关的任务
            List<String> taskIds = logstashDeployService.getAllProcessTaskIds(id);
            if (!taskIds.isEmpty()) {
                logger.info("找到{}个与进程关联的任务，进程ID: {}", taskIds.size(), id);

                // 删除每个任务及其步骤
                for (String taskId : taskIds) {
                    logger.info("删除进程关联的任务: {}", taskId);
                    deleteProcessTask(taskId);
                }
            } else {
                logger.info("没有找到与进程关联的任务，进程ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("删除进程相关任务时发生错误: {}", e.getMessage(), e);
            // 继续删除其他数据
        }

        // 删除与进程相关的所有机器关联
        logstashMachineMapper.deleteByLogstashProcessId(id);
        logger.info("已删除进程与机器的关联关系: {}", id);

        // 删除进程记录
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

        if (LogstashProcessState.INITIALIZING.name().equals(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程正在初始化中");
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

        // 异步启动Logstash（不再包含部署/初始化步骤）
        logstashDeployService.startProcessAsync(process, machines);

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
                LogstashProcessState.START_FAILED.name().equals(process.getState()) ||
                LogstashProcessState.STOP_FAILED.name().equals(process.getState())) {
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

        // 异步停止Logstash
        logstashDeployService.stopProcessAsync(id, machines);

        return getLogstashProcess(id);
    }

    /**
     * 重试失败的任务
     *
     * @param id 进程ID
     * @return 重试操作结果
     */
    @Override
    @Transactional
    public LogstashProcessDTO retryLogstashProcessOps(Long id) {
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
        if (!isFailedState(process.getState())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "只有失败状态的进程可以重试");
        }

        // 获取进程关联的所有机器
        List<Machine> machines = getMachinesForProcess(id);
        if (machines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 重置进程状态为未启动
        logstashProcessMapper.updateState(id, LogstashProcessState.NOT_STARTED.name());
        logger.info("重置进程 [{}] 状态为未启动，准备重新尝试操作", id);

        String failedState = process.getState();

        // 根据失败状态来决定是重试初始化还是启动操作
        if (LogstashProcessState.START_FAILED.name().equals(failedState)) {
            // 如果是启动失败，则只重试启动操作
            logstashDeployService.startProcessAsync(process, machines);
        } else {
            // 如果是停止失败，则只重试停止操作
            logstashDeployService.stopProcessAsync(id, machines);
        }

        return getLogstashProcess(id);
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

    /**
     * 检查进程是否处于失败状态
     */
    private boolean isFailedState(String state) {
        return LogstashProcessState.START_FAILED.name().equals(state) ||
                LogstashProcessState.STOP_FAILED.name().equals(state);
    }

    @Override
    public List<TaskSummaryDTO> getProcessTaskSummaries(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 获取该进程关联的所有任务
        List<LogstashTask> tasks = taskMapper.findByProcessId(id);
        if (tasks.isEmpty()) {
            return List.of();
        }

        // 转换为DTO
        List<TaskSummaryDTO> summaries = new ArrayList<>();
        for (LogstashTask task : tasks) {
            // 获取任务对应的所有步骤
            List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(task.getId());

            // 使用转换器生成摘要DTO
            TaskSummaryDTO summary = taskSummaryConverter.convert(task, steps);
            summaries.add(summary);
        }

        // 按状态优先级和时间排序
        summaries.sort((a, b) -> {
            // 首先按状态排序
            int statusCompare = compareStatus(a.getStatus(), b.getStatus());
            if (statusCompare != 0) {
                return statusCompare;
            }

            // 其次按开始时间倒序排序
            if (a.getStartTime() != null && b.getStartTime() != null) {
                return b.getStartTime().compareTo(a.getStartTime());
            } else if (a.getStartTime() != null) {
                return -1;
            } else if (b.getStartTime() != null) {
                return 1;
            }

            return 0;
        });

        return summaries;
    }

    // 辅助方法：比较任务状态的优先级
    private int compareStatus(String status1, String status2) {
        Map<String, Integer> statusPriority = new HashMap<>();
        statusPriority.put(TaskStatus.RUNNING.name(), 1);
        statusPriority.put(TaskStatus.FAILED.name(), 2);
        statusPriority.put(TaskStatus.COMPLETED.name(), 3);
        statusPriority.put(TaskStatus.CANCELLED.name(), 4);
        statusPriority.put(TaskStatus.PENDING.name(), 5);

        Integer priority1 = statusPriority.getOrDefault(status1, 99);
        Integer priority2 = statusPriority.getOrDefault(status2, 99);

        return priority1.compareTo(priority2);
    }

    @Override
    public TaskStepsGroupDTO getTaskStepsGrouped(String taskId) {
        // 参数校验
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务ID不能为空");
        }

        // 获取任务信息
        Optional<LogstashTask> taskOpt = taskMapper.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "未找到指定任务");
        }

        LogstashTask task = taskOpt.get();

        // 获取任务关联的所有步骤
        List<LogstashTaskMachineStep> allSteps = stepMapper.findByTaskId(taskId);
        if (allSteps.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务没有关联的步骤");
        }

        // 获取所有机器信息
        Set<Long> machineIds = allSteps.stream()
                .map(LogstashTaskMachineStep::getMachineId)
                .collect(Collectors.toSet());

        Map<Long, Machine> machineMap = new HashMap<>();
        if (!machineIds.isEmpty()) {
            List<Machine> machines = machineMapper.selectByIds(new ArrayList<>(machineIds));
            machineMap = machines.stream()
                    .collect(Collectors.toMap(Machine::getId, m -> m));
        }

        // 使用转换器生成DTO
        return taskStepsGroupConverter.convert(task, allSteps, machineMap);
    }
}