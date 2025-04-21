package com.hina.log.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Logstash进程任务实体类
 */
@Data
@Schema(description = "Logstash进程任务实体")
public class LogstashProcess {
    @Schema(description = "进程ID", example = "1")
    private Long id;

    @Schema(description = "进程名称", example = "Nginx日志收集")
    private String name;

    @Schema(description = "机器名称", example = "1")
    private Long machineName;

    @Schema(description = "模块名称", example = "nginx")
    private String module;

    @Schema(description = "Logstash配置文件JSON")
    private String configJson;

    @Schema(description = "与Logstash配置对应的Doris日志表SQL")
    private String dorisSql;

    @Schema(description = "关联的数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "进程状态", example = "NOT_STARTED")
    private String state;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}