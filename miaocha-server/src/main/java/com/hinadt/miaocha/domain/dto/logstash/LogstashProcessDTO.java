package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** Logstash进程信息DTO */
@Data
@Schema(description = "Logstash进程信息对象")
public class LogstashProcessDTO {
    @Schema(description = "进程ID", example = "1")
    private Long id;

    @Schema(description = "进程名称", example = "Nginx日志收集")
    private String name;

    @Schema(description = "模块名称", example = "nginx")
    private String module;

    @Schema(description = "Logstash配置文件内容")
    private String configContent;

    @Schema(description = "与Logstash配置对应的Doris日志表SQL")
    private String dorisSql;

    @Schema(description = "关联的数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "关联的数据源名称", example = "Doris日志库")
    private String datasourceName;

    @Schema(description = "Doris表名", example = "log_table_test_env")
    private String tableName;

    @Schema(description = "JVM配置选项模板")
    private String jvmOptions;

    @Schema(description = "Logstash系统配置模板")
    private String logstashYml;

    @Schema(description = "部署的机器列表")
    private List<LogstashMachineDTO> machines;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
