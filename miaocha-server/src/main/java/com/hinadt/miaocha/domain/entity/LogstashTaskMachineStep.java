package com.hinadt.miaocha.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** Logstash任务在机器上的执行步骤实体类 */
@Data
@Schema(description = "Logstash任务在机器上的执行步骤实体")
public class LogstashTaskMachineStep {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "机器ID", example = "1")
    private Long machineId;

    @Schema(description = "步骤ID", example = "CREATE_REMOTE_DIR")
    private String stepId;

    @Schema(description = "步骤名称", example = "创建远程目录")
    private String stepName;

    @Schema(description = "步骤状态", example = "COMPLETED")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
