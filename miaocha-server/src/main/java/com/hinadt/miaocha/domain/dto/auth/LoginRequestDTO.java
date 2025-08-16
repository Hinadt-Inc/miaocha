package com.hinadt.miaocha.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求DTO */
@Data
@Schema(description = "登录请求对象")
public class LoginRequestDTO {

    @Schema(description = "用户邮箱", example = "user@example.com", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "用户密码", example = "password123", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "认证提供者ID，为空时使用系统默认认证", example = "ldap")
    private String providerId;
}
