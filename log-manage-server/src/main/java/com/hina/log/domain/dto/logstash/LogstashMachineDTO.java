package com.hina.log.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Logstash机器关联DTO
 */
@Data
@Schema(description = "Logstash机器关联信息对象")
public class LogstashMachineDTO {
    @Schema(description = "关联ID", example = "1")
    private Long id;

    @Schema(description = "Logstash进程ID", example = "1")
    private Long logstashProcessId;

    @Schema(description = "机器ID", example = "1")
    private Long machineId;

    @Schema(description = "机器名称", example = "测试服务器")
    private String machineName;

    @Schema(description = "机器IP", example = "192.168.1.100")
    private String machineIp;

    @Schema(description = "目标机器上的进程PID", example = "12345")
    private String processPid;

    @Schema(description = "进程在机器上的状态", example = "NOT_STARTED")
    private String state;

    @Schema(description = "进程在机器上的状态描述", example = "未启动")
    private String stateDescription;

    @Schema(description = "机器特定的Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置选项")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置")
    private String logstashYml;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
