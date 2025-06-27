package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 日志时间分布查询结果DTO */
@Data
@Schema(description = "日志时间分布查询结果对象")
public class LogHistogramResultDTO extends LogSearchResultDTO {

    @Schema(description = "日志时间分布数据，用于生成时间分布图")
    private List<LogDistributionData> distributionData;

    @Schema(
            description =
                    "时间点分组单位，支持毫秒级和22种标准时间间隔：毫秒级(系统自动计算)、秒级(1,2,5,10,15,30)、分钟级(1,2,5,10,15,30)、小时级(1,2,3,6,12)、天级(1,2,3,7,14,30)，具体间隔值参见timeInterval字段",
            example = "minute",
            allowableValues = {"millisecond", "second", "minute", "hour", "day"})
    private String timeUnit;

    @Schema(
            description =
                    "时间间隔数值，与timeUnit组合表示每个桶的间隔。支持的标准间隔值：毫秒级(系统自动计算如100ms)，秒级(1,2,5,10,15,30)，分钟级(1,2,5,10,15,30)，小时级(1,2,3,6,12)，天级(1,2,3,7,14,30)",
            example = "5")
    private Integer timeInterval;

    @Schema(description = "预估桶数量，基于时间范围和颗粒度计算的预期桶数量，优化后目标范围45-60", example = "48")
    private Integer estimatedBuckets;

    @Schema(description = "实际桶数量，实际返回的数据桶数量，应该与预估桶数量接近", example = "45")
    private Integer actualBuckets;

    @Schema(
            description =
                    "颗粒度计算方法：AUTO_CALCULATED(基于Kibana算法自动计算最优间隔)、USER_SPECIFIED(用户指定固定时间单位)、FALLBACK(降级策略，当计算失败时使用默认1分钟间隔)",
            example = "AUTO_CALCULATED",
            allowableValues = {"AUTO_CALCULATED", "USER_SPECIFIED", "FALLBACK"})
    private String calculationMethod;

    /** 日志时间分布数据 */
    @Data
    @Schema(description = "日志时间分布数据")
    public static class LogDistributionData {
        @Schema(description = "时间点，支持毫秒精度格式", example = "2023-01-01 10:15:00.123")
        private String timePoint;

        @Schema(description = "日志数量", example = "123")
        private Long count;
    }
}
