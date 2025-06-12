package com.hinadt.miaocha.domain.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新模块信息请求DTO */
@Data
@Schema(description = "更新模块信息请求")
public class ModuleInfoUpdateDTO {

    @NotNull(message = "模块ID不能为空") @Schema(description = "模块ID", example = "1", required = true)
    private Long id;

    @NotBlank(message = "模块名称不能为空")
    @Schema(description = "模块名称", example = "nginx", required = true)
    private String name;

    @NotNull(message = "数据源ID不能为空") @Schema(description = "关联的数据源ID", example = "1", required = true)
    private Long datasourceId;

    @NotBlank(message = "Doris表名不能为空")
    @Schema(description = "Doris表名", example = "log_table_test_env", required = true)
    private String tableName;
}
