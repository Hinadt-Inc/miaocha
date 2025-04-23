package com.hina.log.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户权限表结构DTO
 */
@Data
@Schema(description = "用户权限表结构对象")
public class UserPermissionTableStructureDTO {

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "数据源名称", example = "生产日志数据库")
    private String datasourceName;

    @Schema(description = "数据库名", example = "log_db")
    private String databaseName;

    @Schema(description = "表列表")
    private List<TableInfoDTO> tables;

    @Data
    @Schema(description = "表信息对象")
    public static class TableInfoDTO {
        @Schema(description = "表名", example = "user_logs")
        private String tableName;

        @Schema(description = "权限ID", example = "1")
        private Long permissionId;
    }
}