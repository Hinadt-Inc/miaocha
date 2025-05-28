package com.hina.log.domain.dto;

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

    @Schema(description = "时间点分组单位timeUnit", example = "second,minute,hour,day")
    private String timeUnit;

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
