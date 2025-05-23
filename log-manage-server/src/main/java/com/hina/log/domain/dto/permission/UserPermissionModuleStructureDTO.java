package com.hina.log.domain.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户权限模块结构DTO
 */
@Data
@Schema(description = "用户权限模块结构信息")
public class UserPermissionModuleStructureDTO {
    
    @Schema(description = "数据源ID")
    private Long datasourceId;
    
    @Schema(description = "数据源名称")
    private String datasourceName;
    
    @Schema(description = "数据库名称")
    private String databaseName;
    
    @Schema(description = "模块列表")
    private List<ModuleInfoDTO> modules;
    
    /**
     * 模块信息DTO
     */
    @Data
    @Schema(description = "模块信息")
    public static class ModuleInfoDTO {
        
        @Schema(description = "模块名称")
        private String moduleName;
        
        @Schema(description = "权限ID")
        private Long permissionId;
    }
}
