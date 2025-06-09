package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 日志明细查询结果DTO */
@Data
@Schema(description = "日志明细查询结果对象")
public class LogDetailResultDTO {
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
}
