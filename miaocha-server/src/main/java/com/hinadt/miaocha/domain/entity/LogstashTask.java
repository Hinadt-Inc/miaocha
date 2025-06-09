package com.hinadt.miaocha.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** Logstash部署任务实体类 */
@Data
@Schema(description = "Logstash部署任务实体")
public class LogstashTask {
    @Schema(description = "任务ID (UUID)")
    private String id;

    @Schema(description = "Logstash进程ID", example = "1")
    private Long processId;

    @Schema(description = "机器ID，如果是针对特定机器的任务", example = "1")
    private Long machineId;

    @Schema(description = "任务名称", example = "部署Nginx日志收集进程")
    private String name;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务状态", example = "RUNNING")
    private String status;

    @Schema(description = "操作类型", example = "START")
    private String operationType;

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
