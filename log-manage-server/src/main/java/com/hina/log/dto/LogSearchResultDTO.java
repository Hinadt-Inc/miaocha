package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 日志检索结果DTO
 */
@Data
@Schema(description = "日志检索结果对象")
public class LogSearchResultDTO {
    @Schema(description = "是否成功", example = "true")
    private Boolean success = true;

    @Schema(description = "错误信息，仅当success=false时有值", example = "关键字表达式语法错误: 括号不匹配")
    private String errorMessage;

    @Schema(description = "查询耗时（毫秒）", example = "123")
    private Long executionTimeMs;

    @Schema(description = "列名列表")
    private List<String> columns;

    @Schema(description = "日志数据明细列表")
    private List<Map<String, Object>> rows;

    @Schema(description = "日志总数")
    private Integer totalCount;

    @Schema(description = "时间分布粒度", example = "MINUTE", allowableValues = { "SECOND", "MINUTE", "HOUR", "DAY",
            "MONTH" })
    private String timeGranularity;

    @Schema(description = "日志时间分布数据，用于生成时间分布图")
    private List<LogDistributionData> distributionData;

    @Data
    @Schema(description = "日志时间分布数据")
    public static class LogDistributionData {
        @Schema(description = "时间点", example = "2023-01-01 10:15:00")
        private String timePoint;

        @Schema(description = "日志数量", example = "123")
        private Long count;
    }
}