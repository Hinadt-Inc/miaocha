package com.hinadt.miaocha.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户信息DTO */
@Data
@Schema(description = "用户信息对象")
public class UserDTO {
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户昵称", example = "管理员")
    private String nickname;

    @Schema(description = "用户邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "用户唯一标识符")
    private String uid;

    @Schema(description = "用户角色", example = "ADMIN")
    private String role;

    @Schema(description = "用户状态：1-启用，0-禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
