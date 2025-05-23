package com.hina.log.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Logstash和机器的关联实体类
 */
@Data
@Schema(description = "Logstash和机器的关联实体")
public class LogstashMachine {
    @Schema(description = "关联ID", example = "1")
    private Long id;

    @Schema(description = "Logstash进程实体数据库ID", example = "1")
    private Long logstashProcessId;

    @Schema(description = "机器ID", example = "1")
    private Long machineId;

    @Schema(description = "目标机器上的进程PID", example = "12345")
    private String processPid;

    @Schema(description = "进程在机器上的状态", example = "NOT_STARTED")
    private String state;

    @Schema(description = "机器特定的Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置选项")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置")
    private String logstashYml;

    @Schema(description = "执行状态", example = "SUCCESS")
    private String executionStatus;

    @Schema(description = "执行开始时间")
    private LocalDateTime executionStartTime;

    @Schema(description = "执行结束时间")
    private LocalDateTime executionEndTime;

    @Schema(description = "执行耗时(毫秒)")
    private Long executionDuration;

    @Schema(description = "执行错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 计算执行耗时
     */
    public void calculateExecutionDuration() {
        if (executionStartTime != null && executionEndTime != null) {
            this.executionDuration = java.time.Duration.between(executionStartTime, executionEndTime).toMillis();
        }
    }
}