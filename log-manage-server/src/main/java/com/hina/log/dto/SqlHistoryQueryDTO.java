package com.hina.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * SQL查询历史分页请求DTO
 */
@Data
@Schema(description = "SQL查询历史分页请求对象")
public class SqlHistoryQueryDTO {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    @Schema(description = "数据源ID，可选，用于筛选特定数据源的查询历史", example = "1")
    private Long datasourceId;

    @Schema(description = "表名，可选，用于筛选包含特定表名的查询记录", example = "users")
    private String tableName;

    @Schema(description = "SQL查询关键字，可选，用于筛选SQL语句中包含特定关键字的记录", example = "SELECT")
    private String queryKeyword;
}