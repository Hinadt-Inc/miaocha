package com.hina.log.domain.dto.logstash;

import com.hina.log.application.logstash.enums.LogstashMachineState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Logstash进程响应DTO")
public class LogstashProcessResponseDTO {

    @Schema(description = "Logstash进程ID")
    private Long id;

    @Schema(description = "Logstash进程名称")
    private String name;

    @Schema(description = "Logstash进程模块")
    private String module;

    @Schema(description = "Logstash进程描述")
    private String description;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置文件内容")
    private String jvmOptions;

    @Schema(description = "Logstash YML配置文件内容")
    private String logstashYml;

    @Schema(description = "自定义的Logstash安装包路径")
    private String customPackagePath;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "关联的Logstash机器状态列表")
    private List<LogstashMachineStatusInfoDTO> machineStatuses;

    @Data
    @Schema(description = "Logstash机器状态信息DTO")
    public static class LogstashMachineStatusInfoDTO {
        @Schema(description = "机器ID")
        private Long machineId;

        @Schema(description = "机器名称")
        private String machineName;

        @Schema(description = "机器IP")
        private String machineIp;

        @Schema(description = "Logstash在该机器上的状态")
        private LogstashMachineState state;

        @Schema(description = "Logstash在该机器上的状态描述")
        private String stateDescription;
    }
}
