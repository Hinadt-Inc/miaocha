package com.hinadt.miaocha.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** SQL查询请求DTO */
@Data
@Schema(description = "SQL查询请求对象")
@SuperBuilder
@NoArgsConstructor
public class SqlQueryDTO {
    @Schema(description = "数据源ID", example = "1", required = true)
    @NotNull(message = "数据源ID不能为空") private Long datasourceId;

    @Schema(description = "SQL查询语句", example = "SELECT * FROM logs LIMIT 10", required = true)
    @NotBlank(message = "SQL查询语句不能为空")
    private String sql;

    @Schema(description = "是否导出结果文件", example = "false")
    private Boolean exportResult = false;

    @Schema(
            description = "导出文件格式（仅当exportResult=true时有效）",
            example = "xlsx",
            allowableValues = {"csv", "xlsx"})
    private String exportFormat = "xlsx";
}
