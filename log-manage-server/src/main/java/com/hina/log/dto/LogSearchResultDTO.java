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

    @Schema(description = "日志时间分布数据，用于生成时间分布图")
    private List<LogDistributionData> distributionData;

    /**
     * 为了保持与修改前的API兼容
     */
    @Schema(hidden = true)
    public List<Map<String, Object>> getRecords() {
        return rows;
    }

    /**
     * 为了保持与修改前的API兼容
     */
    @Schema(hidden = true)
    public void setRecords(List<Map<String, Object>> records) {
        this.rows = records;
    }

    /**
     * 为了保持与修改前的API兼容
     */
    @Schema(hidden = true)
    public List<Map<String, Object>> getDistribution() {
        return rows;
    }

    /**
     * 为了保持与修改前的API兼容
     */
    @Schema(hidden = true)
    public void setDistribution(List<Map<String, Object>> distribution) {
        // 不做实际设置，仅作为兼容方法
    }

    @Data
    @Schema(description = "日志时间分布数据")
    public static class LogDistributionData {
        @Schema(description = "时间点", example = "2023-01-01 10:15:00")
        private String timePoint;

        @Schema(description = "日志数量", example = "123")
        private Long count;
    }
}