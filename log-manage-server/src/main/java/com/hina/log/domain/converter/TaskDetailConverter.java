package com.hina.log.domain.converter;

import com.hina.log.domain.dto.logstash.TaskDetailDTO;
import com.hina.log.domain.entity.LogstashTask;
import com.hina.log.domain.entity.LogstashTaskMachineStep;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 任务详情实体与DTO转换器
 */
@Component
public class TaskDetailConverter {

    /**
     * 将任务实体转换为任务详情DTO
     *
     * @param task 任务实体
     * @return 任务详情DTO
     */
    public TaskDetailDTO convertToTaskDetail(LogstashTask task) {
        if (task == null) {
            return null;
        }

        TaskDetailDTO dto = new TaskDetailDTO();
        dto.setTaskId(task.getId());
        dto.setBusinessId(task.getProcessId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setOperationType(task.getOperationType());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setErrorMessage(task.getErrorMessage());

        // 计算持续时间
        if (task.getStartTime() != null && task.getEndTime() != null) {
            dto.setDuration(Duration.between(task.getStartTime(), task.getEndTime()).toMillis());
        }

        return dto;
    }

    /**
     * 将任务步骤实体转换为步骤DTO
     *
     * @param step 任务步骤实体
     * @return 任务步骤DTO
     */
    public TaskDetailDTO.MachineStepDTO convertToStepDTO(LogstashTaskMachineStep step) {
        if (step == null) {
            return null;
        }

        TaskDetailDTO.MachineStepDTO dto = new TaskDetailDTO.MachineStepDTO();
        dto.setStepId(step.getStepId());
        dto.setStepName(step.getStepName());
        dto.setStatus(step.getStatus());
        dto.setStartTime(step.getStartTime());
        dto.setEndTime(step.getEndTime());
        dto.setErrorMessage(step.getErrorMessage());

        // 计算持续时间
        if (step.getStartTime() != null && step.getEndTime() != null) {
            dto.setDuration(Duration.between(step.getStartTime(), step.getEndTime()).toMillis());
        }

        return dto;
    }
}
