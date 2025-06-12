package com.hinadt.miaocha.domain.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 执行模块Doris SQL请求DTO */
@Data
@Schema(description = "执行模块Doris SQL请求")
public class ModuleExecuteDorisSqlDTO {

    @NotBlank(message = "SQL语句不能为空")
    @Schema(
            description = "Doris SQL语句",
            required = true,
            example = "CREATE TABLE IF NOT EXISTS nginx_logs (id INT, log_content TEXT)")
    private String sql;
}
