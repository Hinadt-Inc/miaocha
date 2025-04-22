package com.hina.log.logstash.task;

import com.hina.log.converter.TaskDetailConverter;
import com.hina.log.converter.TaskMachineStepConverter;
import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.dto.TaskStepsGroupDTO;
import com.hina.log.dto.TaskSummaryDTO;
import com.hina.log.entity.LogstashTask;
import com.hina.log.entity.LogstashTaskMachineStep;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.enums.TaskOperationType;
import com.hina.log.logstash.enums.TaskStatus;
import com.hina.log.mapper.LogstashTaskMachineStepMapper;
import com.hina.log.mapper.LogstashTaskMapper;
import com.hina.log.mapper.MachineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 */
@Service
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final LogstashTaskMapper taskMapper;
    private final LogstashTaskMachineStepMapper stepMapper;
    private final MachineMapper machineMapper;
    private final Executor taskExecutor;
    private final TaskDetailConverter taskDetailConverter;
    private final TaskMachineStepConverter taskMachineStepConverter;

    public TaskServiceImpl(
            LogstashTaskMapper taskMapper,
            LogstashTaskMachineStepMapper stepMapper,
            MachineMapper machineMapper,
            @Qualifier("logstashTaskExecutor") Executor taskExecutor,
            TaskDetailConverter taskDetailConverter,
            TaskMachineStepConverter taskMachineStepConverter) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.machineMapper = machineMapper;
        this.taskExecutor = taskExecutor;
        this.taskDetailConverter = taskDetailConverter;
        this.taskMachineStepConverter = taskMachineStepConverter;
    }

    @Override
    @Transactional
    public String createTask(Long processId, String name, String description,
            TaskOperationType operationType, List<Machine> machines,
            List<String> stepIds) {
        // 创建任务
        String taskId = UUID.randomUUID().toString();
        LogstashTask task = new LogstashTask();
        task.setId(taskId);
        task.setProcessId(processId);
        task.setName(name);
        task.setDescription(description);
        task.setStatus(TaskStatus.PENDING.name());
        task.setOperationType(operationType.name());
        taskMapper.insert(task);

        // 创建各机器的步骤记录
        List<LogstashTaskMachineStep> steps = new ArrayList<>();
        for (Machine machine : machines) {
            for (String stepId : stepIds) {
                LogstashTaskMachineStep step = new LogstashTaskMachineStep();
                step.setTaskId(taskId);
                step.setMachineId(machine.getId());
                step.setStepId(stepId);
                // 根据stepId获取步骤名称（可扩展为从配置或枚举中获取）
                step.setStepName(stepId);
                step.setStatus(StepStatus.PENDING.name());
                steps.add(step);
            }
        }
        stepMapper.batchInsert(steps);

        logger.info("已创建任务: {}, 进程ID: {}", taskId, processId);
        return taskId;
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
        Map<Long, List<LogstashTaskMachineStep>> machineStepsMap = steps.stream()
                .collect(Collectors.groupingBy(LogstashTaskMachineStep::getMachineId));

        // 使用转换器将基于机器ID的步骤转换为基于机器名称的步骤
        Map<String, List<TaskDetailDTO.MachineStepDTO>> nameBasedStepMap = taskMachineStepConverter
                .convertToNameBasedMap(machineStepsMap);

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
    public Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId) {
        Optional<LogstashTask> taskOpt = taskMapper.findLatestByProcessId(processId);
        if (!taskOpt.isPresent()) {
            return Optional.empty();
        }
        return getTaskDetail(taskOpt.get().getId());
    }

    @Override
    public List<String> getAllProcessTaskIds(Long processId) {
        List<LogstashTask> tasks = taskMapper.findByProcessId(processId);
        return tasks.stream()
                .map(LogstashTask::getId)
                .collect(Collectors.toList());
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

        if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED)
                && task.getEndTime() == null) {
            taskMapper.updateEndTime(taskId, LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void updateStepStatus(String taskId, Long machineId, String stepId, StepStatus status) {
        stepMapper.updateStatus(taskId, machineId, stepId, status.name());

        if (status == StepStatus.RUNNING) {
            stepMapper.updateStartTime(taskId, machineId, stepId, LocalDateTime.now());
        }

        if (status == StepStatus.COMPLETED || status == StepStatus.FAILED || status == StepStatus.SKIPPED) {
            stepMapper.updateEndTime(taskId, machineId, stepId, LocalDateTime.now());
        }
    }

    @Override
    public void updateStepErrorMessage(String taskId, Long machineId, String stepId, String errorMessage) {
        stepMapper.updateErrorMessage(taskId, machineId, stepId, errorMessage);
    }

    @Override
    public void executeAsync(String taskId, Runnable action, Runnable callback) {
        taskExecutor.execute(() -> {
            try {
                // 更新任务状态为执行中
                updateTaskStatus(taskId, TaskStatus.RUNNING);

                // 执行任务
                action.run();

                // 更新任务状态为已完成
                updateTaskStatus(taskId, TaskStatus.COMPLETED);
            } catch (Exception e) {
                // 更新任务状态为失败
                LogstashTask task = taskMapper.findById(taskId).orElse(null);
                if (task != null) {
                    task.setErrorMessage(e.getMessage());
                    taskMapper.updateErrorMessage(taskId, e.getMessage());
                }
                updateTaskStatus(taskId, TaskStatus.FAILED);
            } finally {
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    @Override
    public String getTaskSummary(String taskId) {
        Optional<TaskDetailDTO> taskDetail = getTaskDetail(taskId);
        if (!taskDetail.isPresent()) {
            return "任务不存在";
        }

        TaskDetailDTO dto = taskDetail.get();
        StringBuilder summary = new StringBuilder();

        summary.append("任务ID: ").append(dto.getTaskId()).append("\n");
        summary.append("业务ID: ").append(dto.getBusinessId()).append("\n");
        summary.append("任务名称: ").append(dto.getName()).append("\n");
        summary.append("任务状态: ").append(dto.getStatus()).append("\n");
        summary.append("操作类型: ").append(dto.getOperationType()).append("\n");

        if (dto.getStartTime() != null) {
            summary.append("开始时间: ").append(dto.getStartTime()).append("\n");
        }

        if (dto.getEndTime() != null) {
            summary.append("结束时间: ").append(dto.getEndTime()).append("\n");
            summary.append("耗时: ").append(formatDuration(dto.getDuration())).append("\n");
        }

        summary.append("步骤总数: ").append(dto.getTotalSteps()).append("\n");
        summary.append("成功步骤: ").append(dto.getSuccessCount()).append("\n");
        summary.append("失败步骤: ").append(dto.getFailedCount()).append("\n");
        summary.append("跳过步骤: ").append(dto.getSkippedCount()).append("\n");

        if (dto.getErrorMessage() != null) {
            summary.append("错误信息: ").append(dto.getErrorMessage()).append("\n");
        }

        summary.append("\n各机器步骤执行情况:\n");

        for (Map.Entry<String, List<TaskDetailDTO.MachineStepDTO>> entry : dto.getMachineSteps().entrySet()) {
            String machineName = entry.getKey();
            List<TaskDetailDTO.MachineStepDTO> steps = entry.getValue();

            summary.append("  机器: ").append(machineName).append("\n");

            // 按执行顺序排序步骤
            steps.sort(Comparator.comparing(
                    step -> step.getStartTime() != null ? step.getStartTime() : LocalDateTime.MAX));

            for (TaskDetailDTO.MachineStepDTO step : steps) {
                summary.append("    - ").append(step.getStepName()).append(": ")
                        .append(step.getStatus());

                if (step.getErrorMessage() != null) {
                    summary.append(" (").append(step.getErrorMessage()).append(")");
                }

                summary.append("\n");
            }
        }

        return summary.toString();
    }

    @Override
    public Map<String, Map<String, Integer>> getTaskMachineStepStatusStats(String taskId) {
        List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskId);

        // 先按机器ID分组获取状态统计
        Map<Long, Map<String, Integer>> idBasedStats = steps.stream()
                .collect(Collectors.groupingBy(
                        LogstashTaskMachineStep::getMachineId,
                        Collectors.groupingBy(
                                LogstashTaskMachineStep::getStatus,
                                Collectors.counting())))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        v -> v.getValue().intValue()))));

        // 获取所有涉及的机器ID
        Set<Long> machineIds = idBasedStats.keySet();

        // 批量查询机器信息
        List<Machine> machines = new ArrayList<>();
        if (!machineIds.isEmpty()) {
            machines = machineMapper.selectByIds(new ArrayList<>(machineIds));
        }

        Map<Long, Machine> machineMap = machines.stream()
                .collect(Collectors.toMap(Machine::getId, machine -> machine));

        // 转换为基于名称的映射
        Map<String, Map<String, Integer>> nameBasedStats = new HashMap<>();

        for (Map.Entry<Long, Map<String, Integer>> entry : idBasedStats.entrySet()) {
            Long machineId = entry.getKey();
            Map<String, Integer> statusStats = entry.getValue();

            // 获取机器名称，如果找不到则使用ID作为键
            String machineKey = getMachineKey(machineMap, machineId);

            nameBasedStats.put(machineKey, statusStats);
        }

        return nameBasedStats;
    }

    /**
     * 获取机器的键值（优先使用名称，如果不存在则使用ID字符串）
     */
    private String getMachineKey(Map<Long, Machine> machineMap, Long machineId) {
        Machine machine = machineMap.get(machineId);
        if (machine != null && machine.getName() != null && !machine.getName().isEmpty()) {
            return machine.getName();
        }
        // 如果找不到机器或名称为空，则使用ID作为键
        return "Machine-" + machineId;
    }

    // 辅助方法：计算步骤状态统计 [成功数, 失败数, 跳过数]
    private int[] countStepStatus(List<LogstashTaskMachineStep> steps) {
        int[] counts = new int[3];

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

    // 辅助方法：格式化持续时间
    private String formatDuration(Long millis) {
        if (millis == null) {
            return "未知";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        try {
            // 先删除步骤再删除任务
            taskMapper.deleteById(taskId);
            logger.info("Deleted task: {}", taskId);
        } catch (Exception e) {
            logger.error("Failed to delete task: {}", taskId, e);
            throw new RuntimeException("Failed to delete task: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteTaskSteps(String taskId) {
        try {
            stepMapper.deleteByTaskId(taskId);
            logger.info("Deleted all steps for task: {}", taskId);
        } catch (Exception e) {
            logger.error("Failed to delete task steps: {}", taskId, e);
            throw new RuntimeException("Failed to delete task steps: " + e.getMessage(), e);
        }
    }

    /**
     * 重置进程关联的任务状态
     * 将进程ID关联的所有任务及步骤状态设置为未执行状态，便于重试
     *
     * @param processId 进程ID
     * @return 是否成功重置
     * @deprecated 任务应当被视为历史记录，不应重置状态，新的操作应创建新的任务记录
     */
    @Override
    @Transactional
    @Deprecated
    public boolean resetTasksForBusiness(Long processId) {
        try {
            // 获取该进程最新的任务
            List<LogstashTask> tasks = taskMapper.findByProcessId(processId);
            if (tasks.isEmpty()) {
                logger.info("No tasks found for process ID: {}", processId);
                return true;
            }

            // 获取最新任务ID
            String latestTaskId = tasks.get(0).getId();

            // 重置任务状态为PENDING
            taskMapper.updateStatus(latestTaskId, TaskStatus.PENDING.name());

            // 重置任务步骤状态为PENDING
            stepMapper.resetStepStatuses(latestTaskId, StepStatus.PENDING.name());

            // 清除任务和步骤的错误信息
            taskMapper.updateErrorMessage(latestTaskId, null);
            stepMapper.clearStepErrorMessages(latestTaskId);

            logger.info("Successfully reset task status for process ID: {}, task ID: {}", processId, latestTaskId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to reset tasks for process ID: {}", processId, e);
            return false;
        }
    }

    @Override
    public List<TaskSummaryDTO> getProcessTaskSummaries(Long processId) {
        List<LogstashTask> tasks = taskMapper.findByProcessId(processId);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskSummaryDTO> summaries = new ArrayList<>();
        for (LogstashTask task : tasks) {
            TaskSummaryDTO summary = new TaskSummaryDTO();
            summary.setTaskId(task.getId());
            summary.setProcessId(task.getProcessId());
            summary.setName(task.getName());
            summary.setDescription(task.getDescription());
            summary.setStatus(task.getStatus());
            summary.setOperationType(task.getOperationType());
            summary.setStartTime(task.getStartTime());
            summary.setEndTime(task.getEndTime());
            summary.setErrorMessage(task.getErrorMessage());

            // 获取步骤信息并计算统计数据
            List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(task.getId());

            // 初始化计数器
            int totalSteps = steps.size();
            int completedSteps = 0;
            int failedSteps = 0;
            int pendingSteps = 0;
            int runningSteps = 0;
            int skippedSteps = 0;

            // 统计各状态的步骤数量
            for (LogstashTaskMachineStep step : steps) {
                String status = step.getStatus();
                if (StepStatus.COMPLETED.name().equals(status)) {
                    completedSteps++;
                } else if (StepStatus.FAILED.name().equals(status)) {
                    failedSteps++;
                } else if (StepStatus.PENDING.name().equals(status)) {
                    pendingSteps++;
                } else if (StepStatus.RUNNING.name().equals(status)) {
                    runningSteps++;
                } else if (StepStatus.SKIPPED.name().equals(status)) {
                    skippedSteps++;
                }
            }

            // 设置统计数据
            summary.setTotalSteps(totalSteps);
            summary.setCompletedSteps(completedSteps);
            summary.setFailedSteps(failedSteps);
            summary.setPendingSteps(pendingSteps);
            summary.setRunningSteps(runningSteps);
            summary.setSkippedSteps(skippedSteps);

            // 计算进度百分比
            int progressPercentage = 0;
            if (totalSteps > 0) {
                progressPercentage = (completedSteps + skippedSteps) * 100 / totalSteps;
            }
            summary.setProgressPercentage(progressPercentage);

            summaries.add(summary);
        }

        // 按创建时间倒序排序，最新的任务在前面
        summaries.sort((a, b) -> {
            // 首先按状态排序：运行中的最先，然后是失败的，最后是已完成和其他状态
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
        Optional<LogstashTask> taskOpt = taskMapper.findById(taskId);
        if (!taskOpt.isPresent()) {
            return null;
        }

        LogstashTask task = taskOpt.get();
        List<LogstashTaskMachineStep> allSteps = stepMapper.findByTaskId(taskId);

        TaskStepsGroupDTO result = new TaskStepsGroupDTO();
        result.setTaskId(task.getId());
        result.setTaskName(task.getName());
        result.setTaskStatus(task.getStatus());

        // 按步骤ID分组
        Map<String, List<LogstashTaskMachineStep>> stepGroups = allSteps.stream()
                .collect(Collectors.groupingBy(LogstashTaskMachineStep::getStepId));

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

        // 构建步骤组列表
        List<TaskStepsGroupDTO.StepGroup> stepGroupList = new ArrayList<>();

        for (Map.Entry<String, List<LogstashTaskMachineStep>> entry : stepGroups.entrySet()) {
            String stepId = entry.getKey();
            List<LogstashTaskMachineStep> steps = entry.getValue();

            TaskStepsGroupDTO.StepGroup stepGroup = new TaskStepsGroupDTO.StepGroup();
            stepGroup.setStepId(stepId);
            // 使用第一个步骤的名称作为步骤组名称
            stepGroup.setStepName(steps.get(0).getStepName());

            // 计算步骤统计信息
            int completedCount = 0;
            int failedCount = 0;
            int pendingCount = 0;
            int runningCount = 0;
            int skippedCount = 0;

            for (LogstashTaskMachineStep step : steps) {
                String status = step.getStatus();
                if (StepStatus.COMPLETED.name().equals(status)) {
                    completedCount++;
                } else if (StepStatus.FAILED.name().equals(status)) {
                    failedCount++;
                } else if (StepStatus.PENDING.name().equals(status)) {
                    pendingCount++;
                } else if (StepStatus.RUNNING.name().equals(status)) {
                    runningCount++;
                } else if (StepStatus.SKIPPED.name().equals(status)) {
                    skippedCount++;
                }
            }

            stepGroup.setCompletedCount(completedCount);
            stepGroup.setFailedCount(failedCount);
            stepGroup.setPendingCount(pendingCount);
            stepGroup.setRunningCount(runningCount);
            stepGroup.setSkippedCount(skippedCount);
            stepGroup.setTotalCount(steps.size());

            // 构建步骤在各机器上的执行情况
            List<TaskStepsGroupDTO.MachineStep> machineSteps = new ArrayList<>();
            for (LogstashTaskMachineStep step : steps) {
                TaskStepsGroupDTO.MachineStep machineStep = new TaskStepsGroupDTO.MachineStep();
                machineStep.setMachineId(step.getMachineId());

                // 设置机器信息
                Machine machine = machineMap.get(step.getMachineId());
                if (machine != null) {
                    machineStep.setMachineName(machine.getName());
                    machineStep.setMachineIp(machine.getIp());
                } else {
                    machineStep.setMachineName("未知机器");
                    machineStep.setMachineIp("未知IP");
                }

                machineStep.setStatus(step.getStatus());
                machineStep.setStartTime(step.getStartTime());
                machineStep.setEndTime(step.getEndTime());
                machineStep.setErrorMessage(step.getErrorMessage());

                machineSteps.add(machineStep);
            }

            // 按状态排序机器步骤
            machineSteps.sort(Comparator.comparing(ms -> {
                String status = ms.getStatus();
                if (StepStatus.RUNNING.name().equals(status))
                    return 1;
                if (StepStatus.FAILED.name().equals(status))
                    return 2;
                if (StepStatus.COMPLETED.name().equals(status))
                    return 3;
                if (StepStatus.SKIPPED.name().equals(status))
                    return 4;
                return 5; // PENDING 或其他状态
            }));

            stepGroup.setMachineSteps(machineSteps);
            stepGroupList.add(stepGroup);
        }

        // 按步骤完成情况排序
        stepGroupList.sort((a, b) -> {
            // 优先展示正在运行的步骤
            if (a.getRunningCount() > 0 && b.getRunningCount() == 0) {
                return -1;
            }
            if (a.getRunningCount() == 0 && b.getRunningCount() > 0) {
                return 1;
            }

            // 然后是失败的步骤
            if (a.getFailedCount() > 0 && b.getFailedCount() == 0) {
                return -1;
            }
            if (a.getFailedCount() == 0 && b.getFailedCount() > 0) {
                return 1;
            }

            // 最后按步骤ID排序（可能是按照执行顺序命名的）
            return a.getStepId().compareTo(b.getStepId());
        });

        result.setSteps(stepGroupList);
        return result;
    }
}