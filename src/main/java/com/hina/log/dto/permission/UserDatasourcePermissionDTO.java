package com.hina.log.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据源权限DTO
 */
@Data
@Schema(description = "用户数据源权限对象")
public class UserDatasourcePermissionDTO {
    @Schema(description = "权限ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "表名", example = "user_logs")
    private String tableName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}