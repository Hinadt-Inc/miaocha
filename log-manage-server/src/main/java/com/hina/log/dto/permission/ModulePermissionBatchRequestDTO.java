package com.hina.log.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 模块权限批量请求DTO
 */
@Data
@Schema(description = "模块权限批量请求信息")
public class ModulePermissionBatchRequestDTO {
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @NotEmpty(message = "模块列表不能为空")
    @Schema(description = "模块名称列表")
    private List<String> modules;
}
