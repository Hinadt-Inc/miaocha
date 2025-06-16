package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 任务详情DTO */
@Data
@Schema(description = "任务详情DTO")
public class TaskDetailDTO {
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "业务ID - LogStash进程数据库ID")
    private Long businessId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "耗时(毫秒)")
    private Long duration;

    @Schema(description = "步骤总数")
    private Integer totalSteps;

    @Schema(description = "成功步骤数")
    private Integer successCount;

    @Schema(description = "失败步骤数")
    private Integer failedCount;

    @Schema(description = "跳过步骤数")
    private Integer skippedCount;

    @Schema(description = "任务进度百分比 (0-100)")
    private Integer progressPercentage;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "实例步骤详情")
    private Map<String, List<InstanceStepDTO>> instanceSteps;

    @Schema(description = "每个实例的进度百分比 (实例名称 -> 百分比值)")
    private Map<String, Integer> instanceProgressPercentages;

    /**
     * 计算并获取任务进度百分比
     *
     * @return 任务进度百分比 (0-100)
     */
    public Integer getProgressPercentage() {
        if (totalSteps == null || totalSteps == 0) {
            return 0;
        }

        // 计算完成的步骤数（成功 + 失败 + 跳过）
        int completedSteps =
                (successCount != null ? successCount : 0)
                        + (failedCount != null ? failedCount : 0)
                        + (skippedCount != null ? skippedCount : 0);

        // 计算百分比并四舍五入
        return Math.min(100, Math.round((float) completedSteps * 100 / totalSteps));
    }

    /** 实例步骤DTO */
    @Data
    @Schema(description = "Logstash实例步骤DTO")
    public static class InstanceStepDTO {
        @Schema(description = "步骤ID")
        private String stepId;

        @Schema(description = "步骤名称")
        private String stepName;

        @Schema(description = "步骤状态")
        private String status;

        @Schema(description = "开始时间")
        private LocalDateTime startTime;

        @Schema(description = "结束时间")
        private LocalDateTime endTime;

        @Schema(description = "耗时(毫秒)")
        private Long duration;

        @Schema(description = "错误信息")
        private String errorMessage;
    }

    /**
     * 计算获取每个实例的步骤进度百分比
     *
     * @return 每个实例的进度百分比 (实例ID -> 百分比值)
     */
    @Schema(description = "每个实例的进度百分比")
    public Map<String, Integer> getInstanceProgressPercentages() {
        if (instanceSteps == null || instanceSteps.isEmpty()) {
            this.instanceProgressPercentages = Map.of();
            return this.instanceProgressPercentages;
        }

        this.instanceProgressPercentages =
                instanceSteps.entrySet().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> {
                                            List<InstanceStepDTO> steps = entry.getValue();
                                            if (steps == null || steps.isEmpty()) {
                                                return 0;
                                            }

                                            long totalStepsForInstance = steps.size();
                                            long completedSteps =
                                                    steps.stream()
                                                            .filter(
                                                                    step ->
                                                                            "COMPLETED"
                                                                                            .equals(
                                                                                                    step
                                                                                                            .getStatus())
                                                                                    || "FAILED"
                                                                                            .equals(
                                                                                                    step
                                                                                                            .getStatus())
                                                                                    || "SKIPPED"
                                                                                            .equals(
                                                                                                    step
                                                                                                            .getStatus()))
                                                            .count();

                                            return Math.min(
                                                    100,
                                                    (int)
                                                            Math.round(
                                                                    (double) completedSteps
                                                                            * 100
                                                                            / totalStepsForInstance));
                                        }));

        return this.instanceProgressPercentages;
    }
}
