package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 日志检索请求DTO
 */
@Data
@Schema(description = "日志检索请求对象")
public class LogSearchDTO {
    @Schema(description = "数据源ID", example = "1", required = true)
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "模块名称", example = "nginx", required = true)
    @NotBlank(message = "模块名称不能为空")
    private String module;

    @Schema(description = "搜索关键字列表，每个关键字支持多种格式：\n"
            + "1. 单个关键字: error\n"
            + "2. 多个关键字OR关系: 'error' || 'timeout'\n"
            + "3. 多个关键字AND关系: 'error' && 'timeout'\n"
            + "4. 复杂表达式: ('error' || 'warning') && ('timeout' || 'failure')", example = "['error', '('error' || 'warning') && 'timeout'']")
    private List<String> keywords;

    @Schema(description = "WHERE 条件SQL列表，每个条件直接拼接到SQL语句中，多个条件之间使用AND连接", example = "['level = \'ERROR\'', 'service_name = \'user-service\'']")
    private List<String> whereSqls;

    @Schema(description = "开始时间", example = "2023-06-01 10:00:00")
    private String startTime;

    @Schema(description = "结束时间", example = "2023-06-01 11:00:00")
    private String endTime;

    @Schema(description = "预定义时间范围", example = "last_15m", allowableValues = {"last_5m", "last_15m", "last_30m",
            "last_1h", "last_8h", "last_24h", "today", "yesterday", "last_week"})
    private String timeRange;

    @Schema(description = "时间分组单位（用于统计图表）", example = "minute", allowableValues = {"second", "minute", "hour",
            "day",
            "auto"})
    private String timeGrouping = "auto";

    @Schema(description = "分页大小", example = "50")
    private Integer pageSize = 50;

    @Schema(description = "分页偏移量", example = "0")
    private Integer offset = 0;

    @Schema(description = "查询字段列表，为空则查询全部", example = "['log_time', 'level', 'message']")
    private List<String> fields;
}