package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 日志明细查询结果DTO */
@Data
@Schema(description = "日志明细查询结果对象")
public class LogDetailResultDTO extends LogSearchResultDTO {

    @Schema(description = "列名列表")
    private List<String> columns;

    @Schema(description = "日志数据明细列表")
    private List<Map<String, Object>> rows;

    @Schema(description = "日志总数")
    private Integer totalCount;
}
