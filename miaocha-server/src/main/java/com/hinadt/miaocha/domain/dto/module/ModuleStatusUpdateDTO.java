package com.hinadt.miaocha.domain.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 模块状态更新请求DTO */
@Data
@Schema(description = "模块状态更新请求")
@SuperBuilder
@NoArgsConstructor
public class ModuleStatusUpdateDTO {

    @NotNull(message = "模块ID不能为空") @Schema(description = "模块ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "模块状态不能为空") @Schema(
            description = "模块状态：1-启用，0-禁用",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
