package com.hina.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Doris SQL执行DTO */
@Data
@Schema(description = "Doris SQL执行请求对象")
public class DorisSqlExecuteDTO {

    @Schema(description = "Doris SQL语句", example = "CREATE TABLE my_table (...)", required = true)
    @NotBlank(message = "SQL语句不能为空")
    private String sql;
}
