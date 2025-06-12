package com.hinadt.miaocha.domain.dto.permission;

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

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "创建人邮箱")
    private String createUser;

    @Schema(description = "创建人昵称")
    private String createUserName;

    @Schema(description = "修改人邮箱")
    private String updateUser;

    @Schema(description = "修改人昵称")
    private String updateUserName;
}
