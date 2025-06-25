package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 日志检索请求DTO */
@Data
@Schema(description = "日志检索请求对象")
public class LogSearchDTO {
    @Schema(description = "数据源ID", example = "1", required = true)
    @NotNull(message = "数据源ID不能为空") private Long datasourceId;

    @Schema(description = "模块名称", example = "nginx", required = true)
    @NotBlank(message = "模块名称不能为空")
    private String module;

    @Schema(
            description = "结构化关键字查询条件列表，支持配置驱动的字段和搜索方法",
            example =
                    "[{\"fieldName\": \"message\", \"searchValue\": \"error\"}, "
                            + "{\"fieldName\": \"level\", \"searchValue\": \"ERROR\"}]")
    private List<KeywordConditionDTO> keywordConditions;

    @Schema(
            description = "WHERE 条件SQL列表，每个条件直接拼接到SQL语句中，多个条件之间使用AND连接",
            example = "['level = \'ERROR\'', 'service_name = \'user-service\'']")
    private List<String> whereSqls;

    @Schema(description = "开始时间", example = "2023-06-01 10:00:00.000")
    private String startTime;

    @Schema(description = "结束时间", example = "2023-06-01 11:00:00.000")
    private String endTime;

    @Schema(
            description = "预定义时间范围",
            example = "last_15m",
            allowableValues = {
                "last_5m",
                "last_15m",
                "last_30m",
                "last_1h",
                "last_8h",
                "last_24h",
                "today",
                "yesterday",
                "last_week"
            })
    private String timeRange;

    @Schema(
            description = "时间分组单位（用于统计图表）",
            example = "minute",
            allowableValues = {"millisecond", "second", "minute", "hour", "day", "auto"})
    private String timeGrouping = "auto";

    @Schema(
            description = "目标桶数量（用于智能时间颗粒度计算），当timeGrouping为auto时，系统会计算出接近该目标桶数的最优时间颗粒度",
            example = "50")
    private Integer targetBuckets;

    @Schema(description = "分页大小", example = "50")
    @Max(value = 5000, message = "分页大小不能超过 5000 条")
    @Min(value = 1, message = "分页大小不能小于1")
    private Integer pageSize = 50;

    @Schema(description = "分页偏移量", example = "0")
    @Min(value = 0, message = "分页偏移量不能小于0")
    private Integer offset = 0;

    @Schema(description = "查询字段列表，为空则查询全部", example = "['log_time', 'level', 'message']")
    private List<String> fields;
}
