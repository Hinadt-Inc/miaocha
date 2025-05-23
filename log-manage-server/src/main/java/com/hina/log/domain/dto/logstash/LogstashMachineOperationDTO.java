package com.hina.log.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Logstash机器操作DTO
 */
@Data
@Schema(description = "Logstash机器操作对象")
public class LogstashMachineOperationDTO {
    @Schema(description = "进程ID", example = "1")
    @NotNull(message = "进程ID不能为空")
    private Long processId;

    @Schema(description = "机器ID", example = "1")
    @NotNull(message = "机器ID不能为空")
    private Long machineId;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置选项")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置")
    private String logstashYml;
}
