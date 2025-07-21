package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新Logstash进程完整元信息DTO - 包含基础信息和配置信息 */
@Data
@Schema(description = "更新Logstash进程完整元信息对象，只更新数据库，不同步到实例")
public class LogstashProcessMetadataUpdateDTO {

    @Schema(description = "进程名称", example = "Nginx日志收集")
    @NotBlank(message = "进程名称不能为空")
    private String name;

    @Schema(description = "关联的模块ID", example = "1")
    @NotNull(message = "模块ID不能为空") private Long moduleId;

    @Schema(description = "Logstash配置文件内容 (e.g., input {} filter {} output {})")
    private String configContent;

    @Schema(description = "JVM配置文件内容 (jvm.options)")
    private String jvmOptions;

    @Schema(description = "Logstash YML配置文件内容 (logstash.yml)")
    private String logstashYml;
}
