package com.hina.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 日志字段分布查询结果DTO */
@Data
@Schema(description = "日志字段分布查询结果对象")
public class LogFieldDistributionResultDTO {
    @Schema(description = "是否成功", example = "true")
    private Boolean success = true;

    @Schema(description = "错误信息，仅当success=false时有值", example = "关键字表达式语法错误: 括号不匹配")
    private String errorMessage;

    @Schema(description = "查询耗时（毫秒）", example = "123")
    private Long executionTimeMs;

    @Schema(description = "字段数据分布统计信息，用于展示各字段的Top5值及占比")
    private List<FieldDistributionDTO> fieldDistributions;
}
