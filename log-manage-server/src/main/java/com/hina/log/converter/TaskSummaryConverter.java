package com.hina.log.converter;

import com.hina.log.dto.TaskSummaryDTO;
import com.hina.log.entity.LogstashTask;
import com.hina.log.entity.LogstashTaskMachineStep;
import com.hina.log.logstash.enums.StepStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务摘要数据转换器
 */
@Component
public class TaskSummaryConverter {

    /**
     * 将LogstashTask实体转换为TaskSummaryDTO
     *
     * @param task  任务实体
     * @param steps 任务相关的步骤
     * @return 任务摘要DTO
     */
    public TaskSummaryDTO convert(LogstashTask task, List<LogstashTaskMachineStep> steps) {
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

        return summary;
    }
}