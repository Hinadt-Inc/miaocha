package com.hina.log.service.task;

import com.hina.log.converter.TaskDetailConverter;
import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashTask;
import com.hina.log.entity.LogstashTaskMachineStep;
import com.hina.log.entity.Machine;
import com.hina.log.enums.StepStatus;
import com.hina.log.enums.TaskOperationType;
import com.hina.log.enums.TaskStatus;
import com.hina.log.mapper.LogstashTaskMachineStepMapper;
import com.hina.log.mapper.LogstashTaskMapper;
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
    private final Executor taskExecutor;
    private final TaskDetailConverter taskDetailConverter;

    public TaskServiceImpl(
            LogstashTaskMapper taskMapper,
            LogstashTaskMachineStepMapper stepMapper,
            @Qualifier("logstashTaskExecutor") Executor taskExecutor,
            TaskDetailConverter taskDetailConverter) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.taskExecutor = taskExecutor;
        this.taskDetailConverter = taskDetailConverter;
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

        // 转换为DTO结构
        Map<Long, List<TaskDetailDTO.MachineStepDTO>> machineStepDtoMap = new HashMap<>();
        for (Map.Entry<Long, List<LogstashTaskMachineStep>> entry : machineStepsMap.entrySet()) {
            List<TaskDetailDTO.MachineStepDTO> stepDtos = entry.getValue().stream()
                    .map(taskDetailConverter::convertToStepDTO)
                    .collect(Collectors.toList());
            machineStepDtoMap.put(entry.getKey(), stepDtos);
        }

        dto.setMachineSteps(machineStepDtoMap);

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

                logger.error("执行任务失败: {}", taskId, e);
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

        for (Map.Entry<Long, List<TaskDetailDTO.MachineStepDTO>> entry : dto.getMachineSteps().entrySet()) {
            Long machineId = entry.getKey();
            List<TaskDetailDTO.MachineStepDTO> steps = entry.getValue();

            summary.append("  机器ID: ").append(machineId).append("\n");

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
    public Map<Long, Map<String, Integer>> getTaskMachineStepStatusStats(String taskId) {
        List<LogstashTaskMachineStep> steps = stepMapper.findByTaskId(taskId);

        return steps.stream()
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
}