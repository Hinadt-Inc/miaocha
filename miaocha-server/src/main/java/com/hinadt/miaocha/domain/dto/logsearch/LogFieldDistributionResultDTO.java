package com.hinadt.miaocha.domain.dto.logsearch;

import com.hinadt.miaocha.application.service.sql.builder.FieldDistributionSqlBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 日志字段分布查询结果DTO */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日志字段分布查询结果对象")
public class LogFieldDistributionResultDTO extends LogSearchResultDTO {

    @Schema(description = "字段数据分布统计信息，用于展示各字段的Top5值及占比")
    private List<FieldDistributionDTO> fieldDistributions;

    @Schema(description = "采样配置：为了提升查询性能，统计基于最新的N条数据", example = "5000")
    private Integer sampleSize = FieldDistributionSqlBuilder.SAMPLE_SIZE;

    @Schema(description = "实际采样条数：实际参与统计的数据条数", example = "5000")
    private Integer actualSampleCount;

    @Schema(description = "是否为采样统计：true表示基于采样数据统计，false表示全量数据统计", example = "true")
    private Boolean isSampledStatistics = true;
}
