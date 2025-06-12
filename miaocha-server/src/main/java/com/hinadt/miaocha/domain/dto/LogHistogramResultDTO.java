package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 日志时间分布查询结果DTO */
@Data
@Schema(description = "日志时间分布查询结果对象")
public class LogHistogramResultDTO {
    @Schema(description = "是否成功", example = "true")
    private Boolean success = true;

    @Schema(description = "错误信息，仅当success=false时有值", example = "关键字表达式语法错误: 括号不匹配")
    private String errorMessage;

    @Schema(description = "查询耗时（毫秒）", example = "123")
    private Long executionTimeMs;

    @Schema(description = "日志时间分布数据，用于生成时间分布图")
    private List<LogDistributionData> distributionData;

    @Schema(
            description = "时间点分组单位",
            example = "minute",
            allowableValues = {"second", "minute", "hour", "day"})
    private String timeUnit;

    @Schema(description = "时间间隔数值，表示每个桶的间隔，如5分钟、2小时等", example = "5")
    private Integer timeInterval;

    @Schema(description = "预估桶数量，基于时间范围和颗粒度计算的预期桶数量", example = "48")
    private Integer estimatedBuckets;

    @Schema(description = "实际桶数量，实际返回的数据桶数量", example = "45")
    private Integer actualBuckets;

    @Schema(
            description = "颗粒度计算方法",
            example = "AUTO_CALCULATED",
            allowableValues = {"AUTO_CALCULATED", "USER_SPECIFIED", "FALLBACK"})
    private String calculationMethod;

    /** 日志时间分布数据 */
    @Data
    @Schema(description = "日志时间分布数据")
    public static class LogDistributionData {
        @Schema(description = "时间点", example = "2023-01-01 10:15:00")
        private String timePoint;

        @Schema(description = "日志数量", example = "123")
        private Long count;
    }
}
