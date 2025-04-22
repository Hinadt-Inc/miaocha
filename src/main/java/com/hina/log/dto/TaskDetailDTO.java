package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务详情DTO
 */
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

    @Schema(description = "机器步骤详情")
    private Map<String, List<MachineStepDTO>> machineSteps;

    @Schema(description = "每台机器的进度百分比 (机器名称 -> 百分比值)")
    private Map<String, Integer> machineProgressPercentages;

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
        int completedSteps = (successCount != null ? successCount : 0) +
                (failedCount != null ? failedCount : 0) +
                (skippedCount != null ? skippedCount : 0);

        // 计算百分比并四舍五入
        return Math.min(100, Math.round((float) completedSteps * 100 / totalSteps));
    }

    /**
     * 机器步骤DTO
     */
    @Data
    @Schema(description = "机器步骤DTO")
    public static class MachineStepDTO {
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
     * 计算获取每台机器的步骤进度百分比
     * 
     * @return 每台机器的进度百分比 (机器ID -> 百分比值)
     */
    @Schema(description = "每台机器的进度百分比")
    public Map<String, Integer> getMachineProgressPercentages() {
        if (machineSteps == null || machineSteps.isEmpty()) {
            this.machineProgressPercentages = Map.of();
            return this.machineProgressPercentages;
        }

        this.machineProgressPercentages = machineSteps.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<MachineStepDTO> steps = entry.getValue();
                            if (steps == null || steps.isEmpty()) {
                                return 0;
                            }

                            long totalStepsForMachine = steps.size();
                            long completedSteps = steps.stream()
                                    .filter(step -> "COMPLETED".equals(step.getStatus()) ||
                                            "FAILED".equals(step.getStatus()) ||
                                            "SKIPPED".equals(step.getStatus()))
                                    .count();

                            return Math.min(100,
                                    (int) Math.round((double) completedSteps * 100 / totalStepsForMachine));
                        }));

        return this.machineProgressPercentages;
    }
}