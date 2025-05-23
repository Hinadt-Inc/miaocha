package com.hina.log.domain.dto.logstash;

import java.time.LocalDateTime;

/**
 * 任务摘要DTO，用于展示任务列表
 */
public class TaskSummaryDTO {

    private String taskId;
    private Long processId;
    private String name;
    private String description;
    private String status;
    private String operationType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalSteps;
    private int completedSteps;
    private int failedSteps;
    private int pendingSteps;
    private int runningSteps;
    private int skippedSteps;
    private int progressPercentage;
    private String errorMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
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

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public int getPendingSteps() {
        return pendingSteps;
    }

    public void setPendingSteps(int pendingSteps) {
        this.pendingSteps = pendingSteps;
    }

    public int getRunningSteps() {
        return runningSteps;
    }

    public void setRunningSteps(int runningSteps) {
        this.runningSteps = runningSteps;
    }

    public int getSkippedSteps() {
        return skippedSteps;
    }

    public void setSkippedSteps(int skippedSteps) {
        this.skippedSteps = skippedSteps;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}