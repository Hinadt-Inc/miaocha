package com.hinadt.miaocha.domain.dto.logstash;

import java.time.LocalDateTime;
import java.util.List;

/** 任务步骤分组DTO，用于展示按步骤分组的任务执行情况 - 重构支持多实例 */
public class TaskStepsGroupDTO {

    private String taskId;
    private String taskName;
    private String taskStatus;
    private String stepId;
    private String stepName;
    private List<InstanceStepDTO> instanceSteps;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public List<InstanceStepDTO> getInstanceSteps() {
        return instanceSteps;
    }

    public void setInstanceSteps(List<InstanceStepDTO> instanceSteps) {
        this.instanceSteps = instanceSteps;
    }

    /** 实例步骤DTO，表示特定步骤在特定实例上的执行情况 */
    public static class InstanceStepDTO {
        private Long logstashMachineId;
        private Long machineId;
        private String machineName;
        private String instanceName;
        private String deployPath;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;

        public Long getLogstashMachineId() {
            return logstashMachineId;
        }

        public void setLogstashMachineId(Long logstashMachineId) {
            this.logstashMachineId = logstashMachineId;
        }

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }

        public String getMachineName() {
            return machineName;
        }

        public void setMachineName(String machineName) {
            this.machineName = machineName;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }

        public String getDeployPath() {
            return deployPath;
        }

        public void setDeployPath(String deployPath) {
            this.deployPath = deployPath;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /** 步骤组，包含步骤ID、名称和在各机器上的执行情况 - 保留向后兼容性 */
    @Deprecated
    public static class StepGroup {
        private String stepId;
        private String stepName;
        private int completedCount;
        private int failedCount;
        private int pendingCount;
        private int runningCount;
        private int skippedCount;
        private int totalCount;
        private List<MachineStep> machineSteps;

        public String getStepId() {
            return stepId;
        }

        public void setStepId(String stepId) {
            this.stepId = stepId;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public int getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(int completedCount) {
            this.completedCount = completedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        public int getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(int pendingCount) {
            this.pendingCount = pendingCount;
        }

        public int getRunningCount() {
            return runningCount;
        }

        public void setRunningCount(int runningCount) {
            this.runningCount = runningCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void setSkippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public List<MachineStep> getMachineSteps() {
            return machineSteps;
        }

        public void setMachineSteps(List<MachineStep> machineSteps) {
            this.machineSteps = machineSteps;
        }
    }

    /** 机器步骤，表示特定步骤在特定机器上的执行情况 - 保留向后兼容性 */
    @Deprecated
    public static class MachineStep {
        private Long machineId;
        private String machineName;
        private String machineIp;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }

        public String getMachineName() {
            return machineName;
        }

        public void setMachineName(String machineName) {
            this.machineName = machineName;
        }

        public String getMachineIp() {
            return machineIp;
        }

        public void setMachineIp(String machineIp) {
            this.machineIp = machineIp;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
