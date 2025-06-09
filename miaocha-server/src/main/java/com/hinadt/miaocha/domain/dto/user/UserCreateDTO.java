package com.hinadt.miaocha.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 用户创建DTO */
@Data
@Schema(description = "用户创建请求对象")
public class UserCreateDTO {

    @Schema(description = "用户昵称", example = "张三", required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "用户邮箱", example = "user@example.com", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "用户密码", example = "password123", required = true)
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位之间")
    private String password;

    @Schema(
            description = "用户角色",
            example = "ADMIN",
            required = true,
            allowableValues = {"ADMIN", "USER"})
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(ADMIN|USER)$", message = "角色必须是ADMIN或USER")
    private String role;
}
