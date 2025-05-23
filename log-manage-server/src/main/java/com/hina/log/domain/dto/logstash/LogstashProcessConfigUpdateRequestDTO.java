package com.hina.log.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Logstash配置更新请求DTO")
public class LogstashProcessConfigUpdateRequestDTO {

    @Schema(description = "要更新配置的机器ID列表。如果为空或不传，则表示全局更新（更新所有关联的机器）。")
    private List<Long> machineIds; // Optional: if null or empty, applies to all machines of the process

    @Schema(description = "新的Logstash配置文件内容 (e.g., input {} filter {} output {}). 如果为空，则不更新此项。")
    private String configContent; // Optional

    @Schema(description = "新的JVM配置文件内容 (jvm.options). 如果为空，则不更新此项。")
    private String jvmOptions; // Optional

    @Schema(description = "新的Logstash YML配置文件内容 (logstash.yml). 如果为空，则不更新此项。")
    private String logstashYml; // Optional
} 