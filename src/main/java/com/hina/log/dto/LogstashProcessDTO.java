package com.hina.log.dto;

import com.hina.log.entity.Machine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logstash进程信息DTO
 */
@Data
@Schema(description = "Logstash进程信息对象")
public class LogstashProcessDTO {
    @Schema(description = "进程ID", example = "1")
    private Long id;

    @Schema(description = "进程名称", example = "Nginx日志收集")
    private String name;

    @Schema(description = "模块名称", example = "nginx")
    private String module;

    @Schema(description = "Logstash配置文件JSON")
    private String configJson;

    @Schema(description = "与Logstash配置对应的Doris日志表SQL")
    private String dorisSql;

    @Schema(description = "关联的数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "关联的数据源名称", example = "Doris日志库")
    private String datasourceName;

    @Schema(description = "进程状态", example = "NOT_STARTED")
    private String state;

    @Schema(description = "进程状态描述", example = "未启动")
    private String stateDescription;

    @Schema(description = "部署的机器列表")
    private List<Machine> machines;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}