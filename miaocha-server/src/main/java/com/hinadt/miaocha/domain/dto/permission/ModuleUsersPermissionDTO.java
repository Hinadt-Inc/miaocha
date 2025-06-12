package com.hinadt.miaocha.domain.dto.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 模块用户权限聚合DTO */
@Data
@Schema(description = "模块用户权限聚合信息")
public class ModuleUsersPermissionDTO {

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "该模块下拥有权限的用户列表")
    private List<UserPermissionInfoDTO> users;

    /** 用户权限信息DTO */
    @Data
    @Schema(description = "用户权限信息")
    public static class UserPermissionInfoDTO {

        @Schema(description = "权限ID")
        private Long permissionId;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "用户邮箱")
        private String email;

        @Schema(description = "用户角色")
        private String role;

        @Schema(description = "权限创建时间")
        private String createTime;

        @Schema(description = "权限更新时间")
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
}
