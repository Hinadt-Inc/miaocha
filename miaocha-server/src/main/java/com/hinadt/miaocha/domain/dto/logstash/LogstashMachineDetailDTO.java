package com.hinadt.miaocha.domain.dto.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** Logstash机器进程详情DTO 用于返回单个LogstashMachine在特定机器上的完整详情信息 */
@Data
@Schema(description = "Logstash机器进程详情信息")
public class LogstashMachineDetailDTO {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "Logstash进程ID")
    private Long logstashProcessId;

    @Schema(description = "Logstash进程名称")
    private String logstashProcessName;

    @Schema(description = "Logstash进程模块")
    private String logstashProcessModule;

    @Schema(description = "Logstash进程描述")
    private String logstashProcessDescription;

    @Schema(description = "机器ID")
    private Long machineId;

    @Schema(description = "机器名称")
    private String machineName;

    @Schema(description = "机器IP")
    private String machineIp;

    @Schema(description = "机器端口")
    private Integer machinePort;

    @Schema(description = "机器用户名")
    private String machineUsername;

    @Schema(description = "目标机器上的进程PID")
    private String processPid;

    @Schema(description = "进程在机器上的状态")
    private LogstashMachineState state;

    @Schema(description = "进程在机器上的状态描述")
    private String stateDescription;

    @Schema(description = "机器特定的Logstash主配置文件内容")
    private String configContent;

    @Schema(description = "机器特定的JVM配置选项")
    private String jvmOptions;

    @Schema(description = "机器特定的Logstash系统配置")
    private String logstashYml;

    @Schema(description = "自定义的Logstash安装包路径")
    private String customPackagePath;

    @Schema(description = "部署目录路径")
    private String deployPath;

    @Schema(description = "关联创建时间")
    private LocalDateTime createTime;

    @Schema(description = "关联更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "Logstash进程创建时间")
    private LocalDateTime processCreateTime;

    @Schema(description = "Logstash进程更新时间")
    private LocalDateTime processUpdateTime;
}
