package com.hina.log.converter;

import com.hina.log.dto.TaskStepsGroupDTO;
import com.hina.log.entity.LogstashTask;
import com.hina.log.entity.LogstashTaskMachineStep;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.enums.StepStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务步骤分组数据转换器
 */
@Component
public class TaskStepsGroupConverter {

    /**
     * 将LogstashTask和相关步骤转换为按步骤分组的DTO
     *
     * @param task       任务实体
     * @param allSteps   任务相关的所有步骤
     * @param machineMap 机器ID到机器实体的映射
     * @return 任务步骤分组DTO
     */
    public TaskStepsGroupDTO convert(LogstashTask task, List<LogstashTaskMachineStep> allSteps,
            Map<Long, Machine> machineMap) {
        TaskStepsGroupDTO result = new TaskStepsGroupDTO();
        result.setTaskId(task.getId());
        result.setTaskName(task.getName());
        result.setTaskStatus(task.getStatus());

        // 按步骤ID分组
        Map<String, List<LogstashTaskMachineStep>> stepGroups = allSteps.stream()
                .collect(Collectors.groupingBy(LogstashTaskMachineStep::getStepId));

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