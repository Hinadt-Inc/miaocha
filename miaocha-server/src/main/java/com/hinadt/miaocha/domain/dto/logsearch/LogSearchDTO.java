package com.hinadt.miaocha.domain.dto.logsearch;

import com.hinadt.miaocha.domain.validator.ValidSortField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/** 日志检索请求DTO */
@Data
@Schema(description = "日志检索请求对象")
public class LogSearchDTO {
    @Schema(description = "模块名称", example = "nginx", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模块名称不能为空")
    private String module;

    @Schema(
            description =
                    "搜索关键字列表，每个关键字支持多种格式：\n"
                            + "1. 单个关键字: error\n"
                            + "2. 多个关键字OR关系: 'error' || 'timeout'\n"
                            + "3. 多个关键字AND关系: 'error' && 'timeout'\n"
                            + "4. 复杂表达式: ('error' || 'warning') && ('timeout' || 'failure')\n"
                            + "关键字将自动应用到模块配置中的所有关键字字段上",
            example = "['error', '('error' || 'warning') && 'timeout'']")
    private List<String> keywords;

    @Schema(
            description = "WHERE 条件SQL列表，每个条件直接拼接到SQL语句中，多个条件之间使用AND连接",
            example = "['level = 'ERROR'', 'service_name = 'user-service'']")
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

    @Valid
    @Schema(description = "排序字段列表，支持多个字段排序")
    private List<SortField> sortFields;

    @Getter
    @Setter
    @Schema(description = "排序字段配置")
    public static class SortField {
        @NotNull(message = "排序字段名不能为空") @Size(max = 128, message = "排序字段名长度不能超过128个字符")
        @ValidSortField
        @Schema(
                description = "排序字段名，只允许普通字段名，不支持复杂字段引用（如 a.b、a['c'] 等）",
                example = "log_time",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 128)
        private String fieldName;

        @NotNull(message = "排序方向不能为空") @Pattern(regexp = "^(ASC|DESC)$", message = "排序方向必须是 ASC 或 DESC")
        @Schema(
                description = "排序方向",
                example = "DESC",
                allowableValues = {"ASC", "DESC"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                defaultValue = "DESC")
        private String direction = "DESC";
    }
}
