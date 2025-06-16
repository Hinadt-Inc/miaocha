package com.hinadt.miaocha.application.logstash.task;

import com.hinadt.miaocha.application.logstash.enums.StepStatus;
import com.hinadt.miaocha.application.logstash.enums.TaskOperationType;
import com.hinadt.miaocha.application.logstash.enums.TaskStatus;
import com.hinadt.miaocha.domain.converter.TaskDetailConverter;
import com.hinadt.miaocha.domain.converter.TaskMachineStepConverter;
import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO;
import com.hinadt.miaocha.domain.dto.logstash.TaskStepsGroupDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashTask;
import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.LogstashTaskMachineStepMapper;
import com.hinadt.miaocha.domain.mapper.LogstashTaskMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 任务服务实现类 - 重构支持多实例，基于logstashMachineId */
@Service
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final LogstashTaskMapper taskMapper;
    private final LogstashTaskMachineStepMapper stepMapper;
    private final MachineMapper machineMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final Executor taskExecutor;
    private final TaskDetailConverter taskDetailConverter;
    private final TaskMachineStepConverter taskMachineStepConverter;

    public TaskServiceImpl(
            LogstashTaskMapper taskMapper,
            LogstashTaskMachineStepMapper stepMapper,
            MachineMapper machineMapper,
            LogstashMachineMapper logstashMachineMapper,
            @Qualifier("logstashTaskExecutor") Executor taskExecutor,
            TaskDetailConverter taskDetailConverter,
            TaskMachineStepConverter taskMachineStepConverter) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.machineMapper = machineMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.taskExecutor = taskExecutor;
        this.taskDetailConverter = taskDetailConverter;
        this.taskMachineStepConverter = taskMachineStepConverter;
    }

    @Override
    @Transactional
    public String createGlobalTask(
            Long processId,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds) {
        logger.info("创建全局任务，进程ID: {}", processId);

        // 创建任务
        String taskId = UUID.randomUUID().toString();
        LogstashTask task = new LogstashTask();
        task.setId(taskId);
        task.setProcessId(processId);
        task.setName(name);
        task.setDescription(description);
        task.setStatus(TaskStatus.PENDING.name());
        task.setOperationType(operationType != null ? operationType.name() : null);
        taskMapper.insert(task);

        logger.info("已创建全局任务: {}, 进程ID: {}", taskId, processId);
        return taskId;
    }

    @Override
    @Transactional
    public String createInstanceTask(
            Long logstashMachineId,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds) {
        logger.info("创建实例任务，实例ID: {}", logstashMachineId);

        // 获取LogstashMachine信息
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            logger.error("创建实例任务失败: 找不到LogstashMachine实例ID: {}", logstashMachineId);
            return null;
        }

        // 获取机器信息
        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            logger.error("创建实例任务失败: 找不到机器ID: {}", logstashMachine.getMachineId());
            return null;
        }

        // 创建任务
        String taskId = UUID.randomUUID().toString();
        LogstashTask task = new LogstashTask();
        task.setId(taskId);
        task.setProcessId(logstashMachine.getLogstashProcessId());
        task.setMachineId(logstashMachine.getMachineId());
        task.setLogstashMachineId(logstashMachineId); // 新增字段
        task.setName(name);
        task.setDescription(description);
        task.setStatus(TaskStatus.PENDING.name());
        task.setOperationType(operationType != null ? operationType.name() : null);
        taskMapper.insert(task);

        // 创建步骤记录
        List<LogstashTaskMachineStep> steps = new ArrayList<>();
        for (String stepId : stepIds) {
            LogstashTaskMachineStep step = new LogstashTaskMachineStep();
            step.setTaskId(taskId);
            step.setMachineId(logstashMachine.getMachineId());
            step.setLogstashMachineId(logstashMachineId); // 新增字段
            step.setStepId(stepId);
            step.setStepName(getStepName(stepId));
            step.setStatus(StepStatus.PENDING.name());
            steps.add(step);
        }
        stepMapper.batchInsert(steps);

        logger.info(
                "已创建实例任务: {}, 实例ID: {}, 机器: {}", taskId, logstashMachineId, machineInfo.getName());
        return taskId;
    }

    @Override
    @Transactional
    public Map<Long, String> createInstanceTasks(
            List<LogstashMachine> logstashMachines,
            String name,
            String description,
            TaskOperationType operationType,
            List<String> stepIds) {
        logger.info("批量创建实例任务，实例数量: {}", logstashMachines.size());

        Map<Long, String> instanceTaskMap = new HashMap<>();

        // 为每个实例创建单独的任务
        for (LogstashMachine logstashMachine : logstashMachines) {
            String taskName = name + " - 实例[" + logstashMachine.getId() + "]";
            String taskId =
                    createInstanceTask(
                            logstashMachine.getId(), taskName, description, operationType, stepIds);

            if (taskId != null) {
                instanceTaskMap.put(logstashMachine.getId(), taskId);
            }
        }

        logger.info("已完成批量创建实例任务，创建任务数: {}", instanceTaskMap.size());
        return instanceTaskMap;
    }

    @Override
    public Optional<TaskDetailDTO> getTaskDetail(String taskId) {
        Optional<LogstashTask> taskOpt = taskMapper.findById(taskId);
        if (!taskOpt.isPresent()) {
            return Optional.empty();
        }

        // 获取任务信息
        LogstashTask task = taskOpt.get();
        TaskDetailDTO dto = taskDetailConverter.convertToTaskDetail(task);

        // 获取所有步骤信息
        List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskId);

        // 按机器分组步骤
        Map<Long, List<LogstashTaskMachineStep>> machineStepsMap =
                steps.stream()
                        .collect(Collectors.groupingBy(LogstashTaskMachineStep::getMachineId));

        // 使用转换器将基于机器ID的步骤转换为基于机器名称的步骤
        Map<String, List<TaskDetailDTO.MachineStepDTO>> nameBasedStepMap =
                taskMachineStepConverter.convertToNameBasedMap(machineStepsMap);

        dto.setMachineSteps(nameBasedStepMap);

        // 计算统计信息
        int[] counts = countStepStatus(steps);
        dto.setTotalSteps(steps.size());
        dto.setSuccessCount(counts[0]);
        dto.setFailedCount(counts[1]);
        dto.setSkippedCount(counts[2]);

        return Optional.of(dto);
    }

    @Override
    public List<String> getAllProcessTaskIds(Long processId) {
        List<LogstashTask> tasks = taskMapper.findByProcessId(processId);
        return tasks.stream().map(LogstashTask::getId).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllInstanceTaskIds(Long logstashMachineId) {
        logger.debug("获取LogstashMachine实例[{}]的所有任务ID", logstashMachineId);
        return taskMapper.findTaskIdsByLogstashMachineId(logstashMachineId);
    }

    @Override
    public List<String> getAllInstanceTaskIdsByProcessAndMachine(Long processId, Long machineId) {
        logger.debug("获取进程[{}]在机器[{}]上所有实例的任务ID", processId, machineId);

        // 获取该进程在指定机器上的所有LogstashMachine实例
        List<LogstashMachine> allInstances =
                logstashMachineMapper.selectByLogstashProcessId(processId);
        List<LogstashMachine> logstashMachines =
                allInstances.stream()
                        .filter(instance -> machineId.equals(instance.getMachineId()))
                        .toList();

        // 收集所有实例的任务ID
        List<String> allTaskIds = new ArrayList<>();
        for (LogstashMachine logstashMachine : logstashMachines) {
            List<String> instanceTaskIds =
                    taskMapper.findTaskIdsByLogstashMachineId(logstashMachine.getId());
            allTaskIds.addAll(instanceTaskIds);
        }

        return allTaskIds;
    }

    @Override
    public Optional<TaskDetailDTO> getLatestInstanceTaskDetailByProcessAndMachine(
            Long processId, Long machineId) {
        logger.debug("获取进程[{}]在机器[{}]上最新的实例任务详情", processId, machineId);

        // 获取所有相关任务ID
        List<String> taskIds = getAllInstanceTaskIdsByProcessAndMachine(processId, machineId);

        if (taskIds.isEmpty()) {
            return Optional.empty();
        }

        // 获取所有任务详情并找到最新的
        Optional<TaskDetailDTO> latestTask =
                taskIds.stream()
                        .map(taskId -> getTaskDetail(taskId).orElse(null))
                        .filter(Objects::nonNull)
                        .max(
                                Comparator.comparing(
                                        task ->
                                                task.getCreateTime() != null
                                                        ? task.getCreateTime()
                                                        : LocalDateTime.MIN));

        return latestTask;
    }

    @Override
    @Transactional
    public void updateTaskStatus(String taskId, TaskStatus status) {
        taskMapper.updateStatus(taskId, status.name());

        LogstashTask task = taskMapper.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }

        if (status == TaskStatus.RUNNING && task.getStartTime() == null) {
            taskMapper.updateStartTime(taskId, LocalDateTime.now());
        }

        if ((status == TaskStatus.COMPLETED
                        || status == TaskStatus.FAILED
                        || status == TaskStatus.CANCELLED)
                && task.getEndTime() == null) {
            taskMapper.updateEndTime(taskId, LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void updateStepStatus(
            String taskId, Long logstashMachineId, String stepId, StepStatus status) {
        // 获取LogstashMachine信息以获取machineId
        LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
        if (logstashMachine == null) {
            logger.error("更新步骤状态失败: 找不到LogstashMachine实例ID: {}", logstashMachineId);
            return;
        }

        stepMapper.updateStatusByLogstashMachineId(
                taskId, logstashMachineId, stepId, status.name());

        if (status == StepStatus.RUNNING) {
            stepMapper.updateStartTimeByLogstashMachineId(
                    taskId, logstashMachineId, stepId, LocalDateTime.now());
        }

        if (status == StepStatus.COMPLETED
                || status == StepStatus.FAILED
                || status == StepStatus.SKIPPED) {
            stepMapper.updateEndTimeByLogstashMachineId(
                    taskId, logstashMachineId, stepId, LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void updateStepStatus(
            String taskId,
            Long logstashMachineId,
            String stepId,
            StepStatus status,
            String errorMessage) {
        // 更新状态
        updateStepStatus(taskId, logstashMachineId, stepId, status);

        // 如果提供了错误信息或状态是失败状态，更新错误信息
        if (errorMessage != null || status == StepStatus.FAILED) {
            stepMapper.updateErrorMessageByLogstashMachineId(
                    taskId, logstashMachineId, stepId, errorMessage);
        }
    }

    @Override
    public void executeAsync(String taskId, Runnable action, Runnable callback) {
        taskExecutor.execute(
                () -> {
                    try {
                        // 更新任务状态为执行中
                        updateTaskStatus(taskId, TaskStatus.RUNNING);

                        // 执行任务
                        action.run();

                        // 检查任务状态 - 如果执行过程中任务被标记为失败，不要将其更新为已完成
                        Optional<LogstashTask> taskAfterExecution = taskMapper.findById(taskId);
                        if (taskAfterExecution.isPresent()
                                && !TaskStatus.FAILED
                                        .name()
                                        .equals(taskAfterExecution.get().getStatus())) {
                            // 只有当任务未被标记为失败时才更新为已完成
                            updateTaskStatus(taskId, TaskStatus.COMPLETED);
                        }
                    } catch (Exception e) {
                        // 更新任务状态为失败
                        logger.error("任务执行失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
                        LogstashTask task = taskMapper.findById(taskId).orElse(null);
                        if (task != null) {
                            String errorMessage = e.getMessage();
                            if (errorMessage == null || errorMessage.isEmpty()) {
                                errorMessage = "未知错误";
                            }
                            taskMapper.updateErrorMessage(taskId, errorMessage);
                        }
                        updateTaskStatus(taskId, TaskStatus.FAILED);
                    } finally {
                        // 执行回调
                        if (callback != null) {
                            try {
                                callback.run();
                            } catch (Exception e) {
                                logger.error(
                                        "任务回调执行失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
                            }
                        }
                    }
                });
    }

    @Override
    public Map<String, Map<String, Integer>> getTaskInstanceStepStatusStats(String taskId) {
        List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskId);

        // 按LogstashMachine实例ID分组统计
        Map<String, Map<String, Integer>> instanceStats = new HashMap<>();

        Map<Long, List<LogstashTaskMachineStep>> instanceStepsMap =
                steps.stream()
                        .filter(step -> step.getLogstashMachineId() != null)
                        .collect(
                                Collectors.groupingBy(
                                        LogstashTaskMachineStep::getLogstashMachineId));

        for (Map.Entry<Long, List<LogstashTaskMachineStep>> entry : instanceStepsMap.entrySet()) {
            Long logstashMachineId = entry.getKey();
            List<LogstashTaskMachineStep> instanceSteps = entry.getValue();

            Map<String, Integer> statusStats = new HashMap<>();
            statusStats.put("PENDING", 0);
            statusStats.put("RUNNING", 0);
            statusStats.put("COMPLETED", 0);
            statusStats.put("FAILED", 0);
            statusStats.put("SKIPPED", 0);

            for (LogstashTaskMachineStep step : instanceSteps) {
                String status = step.getStatus();
                statusStats.put(status, statusStats.getOrDefault(status, 0) + 1);
            }

            instanceStats.put("instance-" + logstashMachineId, statusStats);
        }

        return instanceStats;
    }

    @Override
    public List<TaskStepsGroupDTO> getTaskStepsGroupStats(String taskId) {
        Optional<LogstashTask> taskOpt = taskMapper.findById(taskId);
        if (!taskOpt.isPresent()) {
            return Collections.emptyList();
        }

        LogstashTask task = taskOpt.get();
        List<LogstashTaskMachineStep> allSteps = stepMapper.findByTaskId(taskId);

        // 按步骤ID分组
        Map<String, List<LogstashTaskMachineStep>> stepGroups =
                allSteps.stream()
                        .collect(Collectors.groupingBy(LogstashTaskMachineStep::getStepId));

        // 获取所有相关的LogstashMachine信息
        Set<Long> logstashMachineIds =
                allSteps.stream()
                        .map(LogstashTaskMachineStep::getLogstashMachineId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, LogstashMachine> logstashMachineMap = new HashMap<>();
        if (!logstashMachineIds.isEmpty()) {
            List<LogstashMachine> logstashMachines =
                    logstashMachineMapper.selectByIds(new ArrayList<>(logstashMachineIds));
            logstashMachineMap =
                    logstashMachines.stream()
                            .collect(Collectors.toMap(LogstashMachine::getId, m -> m));
        }

        // 获取所有机器信息
        Set<Long> machineIds =
                allSteps.stream()
                        .map(LogstashTaskMachineStep::getMachineId)
                        .collect(Collectors.toSet());

        Map<Long, MachineInfo> machineMap = new HashMap<>();
        if (!machineIds.isEmpty()) {
            List<MachineInfo> machineInfos = machineMapper.selectByIds(new ArrayList<>(machineIds));
            machineMap =
                    machineInfos.stream().collect(Collectors.toMap(MachineInfo::getId, m -> m));
        }

        // 构建步骤组列表
        List<TaskStepsGroupDTO> stepGroupList = new ArrayList<>();
        for (Map.Entry<String, List<LogstashTaskMachineStep>> entry : stepGroups.entrySet()) {
            String stepId = entry.getKey();
            List<LogstashTaskMachineStep> stepList = entry.getValue();

            TaskStepsGroupDTO stepGroup = new TaskStepsGroupDTO();
            stepGroup.setTaskId(task.getId());
            stepGroup.setTaskName(task.getName());
            stepGroup.setTaskStatus(task.getStatus());
            stepGroup.setStepId(stepId);
            stepGroup.setStepName(getStepName(stepId));

            // 构建实例步骤列表
            List<TaskStepsGroupDTO.InstanceStepDTO> instanceSteps = new ArrayList<>();
            for (LogstashTaskMachineStep step : stepList) {
                TaskStepsGroupDTO.InstanceStepDTO instanceStep =
                        new TaskStepsGroupDTO.InstanceStepDTO();
                instanceStep.setLogstashMachineId(step.getLogstashMachineId());
                instanceStep.setMachineId(step.getMachineId());

                // 设置机器名称
                MachineInfo machineInfo = machineMap.get(step.getMachineId());
                instanceStep.setMachineName(machineInfo != null ? machineInfo.getName() : "未知机器");

                // 设置实例信息
                LogstashMachine logstashMachine =
                        logstashMachineMap.get(step.getLogstashMachineId());
                if (logstashMachine != null) {
                    instanceStep.setInstanceName("实例-" + logstashMachine.getId());
                    instanceStep.setDeployPath(logstashMachine.getDeployPath());
                }

                instanceStep.setStatus(step.getStatus());
                instanceStep.setStartTime(step.getStartTime());
                instanceStep.setEndTime(step.getEndTime());
                instanceStep.setErrorMessage(step.getErrorMessage());

                instanceSteps.add(instanceStep);
            }

            stepGroup.setInstanceSteps(instanceSteps);
            stepGroupList.add(stepGroup);
        }

        return stepGroupList;
    }

    @Override
    @Transactional
    public void resetStepStatuses(String taskId, StepStatus newStatus) {
        if (taskId == null || newStatus == null) {
            return;
        }
        stepMapper.resetStepStatuses(taskId, newStatus.name());
        logger.info("重置任务所有步骤状态，任务ID: {}, 新状态: {}", taskId, newStatus.name());
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        try {
            // 先删除步骤再删除任务
            stepMapper.deleteByTaskId(taskId);
            taskMapper.deleteById(taskId);
            logger.info("删除任务: {}", taskId);
        } catch (Exception e) {
            logger.error("删除任务失败: {}", taskId, e);
            throw new RuntimeException("删除任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteInstanceTasks(Long logstashMachineId) {
        try {
            List<String> taskIds = getAllInstanceTaskIds(logstashMachineId);
            for (String taskId : taskIds) {
                deleteTask(taskId);
            }
            logger.info("删除实例相关的所有任务，实例ID: {}, 删除任务数: {}", logstashMachineId, taskIds.size());
        } catch (Exception e) {
            logger.error("删除实例任务失败，实例ID: {}", logstashMachineId, e);
            throw new RuntimeException("删除实例任务失败: " + e.getMessage(), e);
        }
    }

    /** 统计步骤状态 */
    private int[] countStepStatus(List<LogstashTaskMachineStep> steps) {
        int[] counts = new int[3]; // [成功, 失败, 跳过]

        for (LogstashTaskMachineStep step : steps) {
            if (StepStatus.COMPLETED.name().equals(step.getStatus())) {
                counts[0]++;
            } else if (StepStatus.FAILED.name().equals(step.getStatus())) {
                counts[1]++;
            } else if (StepStatus.SKIPPED.name().equals(step.getStatus())) {
                counts[2]++;
            }
        }

        return counts;
    }

    /** 根据步骤ID获取步骤名称 */
    private String getStepName(String stepId) {
        // 这里可以扩展为从配置或枚举中获取步骤名称
        switch (stepId) {
            case "createDirectory":
                return "创建目录";
            case "uploadPackage":
                return "上传安装包";
            case "extractPackage":
                return "解压安装包";
            case "createConfig":
                return "创建配置";
            case "modifyConfig":
                return "修改配置";
            case "startProcess":
                return "启动进程";
            case "verifyProcess":
                return "验证进程";
            case "stopProcess":
                return "停止进程";
            case "deleteProcessDirectory":
                return "删除目录";
            case "refreshConfig":
                return "刷新配置";
            default:
                return stepId;
        }
    }
}
