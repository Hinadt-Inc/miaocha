package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.LogstashTask;
import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** 任务详情实体与DTO转换器 */
@Component
public class TaskDetailConverter {

    /**
     * 将任务实体转换为任务详情DTO
     *
     * @param task 任务实体
     * @return 任务详情DTO
     */
    public TaskDetailDTO convertToTaskDetail(LogstashTask task) {
        return convertToTaskDetail(task, null, null);
    }

    /**
     * 将任务实体转换为任务详情DTO (包含机器和进程信息)
     *
     * @param task 任务实体
     * @param machineInfo 机器信息（可为null）
     * @param logstashProcess Logstash进程信息（可为null）
     * @return 任务详情DTO
     */
    public TaskDetailDTO convertToTaskDetail(
            LogstashTask task, MachineInfo machineInfo, LogstashProcess logstashProcess) {
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
        dto.setCreateTime(task.getCreateTime());
        dto.setErrorMessage(task.getErrorMessage());

        // 设置机器信息
        if (machineInfo != null) {
            dto.setMachineId(machineInfo.getId());
            dto.setMachineName(machineInfo.getName());
            dto.setMachineIp(machineInfo.getIp());
        } else if (task.getMachineId() != null) {
            // 如果没有传入机器信息但任务有机器ID，至少设置机器ID
            dto.setMachineId(task.getMachineId());
        }

        // 设置进程名称
        if (logstashProcess != null) {
            dto.setProcessName(logstashProcess.getName());
        }

        // 计算持续时间
        if (task.getStartTime() != null && task.getEndTime() != null) {
            dto.setDuration(Duration.between(task.getStartTime(), task.getEndTime()).toSeconds());
        }

        return dto;
    }

    /**
     * 将任务步骤实体转换为步骤DTO
     *
     * @param step 任务步骤实体
     * @return 任务步骤DTO
     */
    public TaskDetailDTO.InstanceStepDTO convertToStepDTO(LogstashTaskMachineStep step) {
        if (step == null) {
            return null;
        }

        TaskDetailDTO.InstanceStepDTO dto = new TaskDetailDTO.InstanceStepDTO();
        dto.setStepId(step.getStepId());
        dto.setStepName(step.getStepName());
        dto.setStatus(step.getStatus());
        dto.setStartTime(step.getStartTime());
        dto.setEndTime(step.getEndTime());
        dto.setErrorMessage(step.getErrorMessage());

        // 计算持续时间
        if (step.getStartTime() != null && step.getEndTime() != null) {
            dto.setDuration(Duration.between(step.getStartTime(), step.getEndTime()).toSeconds());
        }

        return dto;
    }
}
