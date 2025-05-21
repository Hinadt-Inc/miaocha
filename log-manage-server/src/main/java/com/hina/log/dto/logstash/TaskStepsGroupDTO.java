package com.hina.log.dto.logstash;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务步骤分组DTO，用于展示按步骤分组的任务执行情况
 */
public class TaskStepsGroupDTO {

    private String taskId;
    private String taskName;
    private String taskStatus;
    private List<StepGroup> steps;

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

    public List<StepGroup> getSteps() {
        return steps;
    }

    public void setSteps(List<StepGroup> steps) {
        this.steps = steps;
    }

    /**
     * 步骤组，包含步骤ID、名称和在各机器上的执行情况
     */
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

    /**
     * 机器步骤，表示特定步骤在特定机器上的执行情况
     */
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