package com.hinadt.miaocha.domain.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 模块信息创建请求DTO */
@Data
@Schema(description = "模块信息创建请求")
@SuperBuilder
@NoArgsConstructor
public class ModuleInfoCreateDTO {

    @NotBlank(message = "模块名称不能为空")
    @Schema(description = "模块名称", example = "Nginx日志模块", required = true)
    private String name;

    @NotNull(message = "数据源ID不能为空") @Schema(description = "数据源ID", example = "1", required = true)
    private Long datasourceId;

    @NotBlank(message = "表名不能为空")
    @Schema(description = "表名", example = "nginx_logs", required = true)
    private String tableName;

    // 注意：dorisSql字段已移除，只能通过executeDorisSql方法设置
}
