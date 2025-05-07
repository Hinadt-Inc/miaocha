package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建Logstash进程DTO
 */
@Data
@Schema(description = "创建Logstash进程对象")
public class LogstashProcessCreateDTO {
    @Schema(description = "进程名称", example = "Nginx日志收集")
    @NotBlank(message = "进程名称不能为空")
    private String name;

    @Schema(description = "模块名称", example = "nginx")
    @NotBlank(message = "模块名称不能为空")
    private String module;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "与Logstash配置对应的Doris日志表SQL")
    private String dorisSql;

    @Schema(description = "关联的数据源ID", example = "1")
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "Doris表名, 选择性手动指定, 不指定的话会从配置中解析", example = "log_table_test_env")
    private String tableName;

    @Schema(description = "部署的机器ID列表")
    @NotEmpty(message = "部署机器不能为空")
    private List<Long> machineIds;
}