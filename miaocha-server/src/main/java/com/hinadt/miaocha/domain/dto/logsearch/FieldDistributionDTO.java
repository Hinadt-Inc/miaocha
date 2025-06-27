package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 字段数据分布DTO 用于表示日志字段的数据分布统计信息 */
@Data
@Schema(description = "字段数据分布统计对象")
public class FieldDistributionDTO {

    @Schema(description = "字段名称", example = "level")
    private String fieldName;

    @Schema(description = "字段值分布列表，按数量降序排序")
    private List<ValueDistribution> valueDistributions;

    /** 字段值分布数据 */
    @Data
    @Schema(description = "字段值分布数据")
    public static class ValueDistribution {
        @Schema(description = "字段值", example = "ERROR")
        private Object value;

        @Schema(description = "出现次数", example = "42")
        private Integer count;

        @Schema(description = "在采样数据中的占比百分比", example = "14.5")
        private Double percentage;
    }
}
