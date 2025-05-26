package com.hina.log.domain.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 用户模块权限DTO */
@Data
@Schema(description = "用户模块权限信息")
public class UserModulePermissionDTO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}
