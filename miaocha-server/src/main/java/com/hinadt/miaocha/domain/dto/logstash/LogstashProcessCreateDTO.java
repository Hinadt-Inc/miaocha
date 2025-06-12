package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建Logstash进程DTO */
@Data
@Schema(description = "创建Logstash进程对象")
public class LogstashProcessCreateDTO {
    @Schema(description = "进程名称", example = "Nginx日志收集")
    @NotBlank(message = "进程名称不能为空")
    private String name;

    @Schema(description = "关联的模块ID", example = "1")
    @NotNull(message = "模块ID不能为空") private Long moduleId;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "JVM配置选项模板")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置模板")
    private String logstashYml;

    @Schema(description = "自定义部署路径，如果不指定则使用系统默认配置", example = "/opt/custom/logstash")
    private String customDeployPath;

    @Schema(description = "部署的机器ID列表")
    @NotEmpty(message = "部署机器不能为空")
    private List<Long> machineIds;
}
