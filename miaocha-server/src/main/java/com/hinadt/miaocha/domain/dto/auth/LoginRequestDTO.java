package com.hinadt.miaocha.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求DTO */
@Data
@Schema(description = "登录请求对象")
public class LoginRequestDTO {

    @Schema(description = "用户邮箱或用户名", example = "user@example.com", required = true)
    @NotBlank(message = "用户名不能为空")
    private String loginIdentifier;

    @Schema(description = "用户密码", example = "password123", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "提供者ID", example = "ldap", required = false)
    private String providerId;

    // 保持向后兼容性
    @Schema(
            description = "用户邮箱（已废弃，请使用loginIdentifier）",
            example = "user@example.com",
            required = false)
    @Deprecated
    public String getEmail() {
        return loginIdentifier;
    }

    @Deprecated
    public void setEmail(String email) {
        this.loginIdentifier = email;
    }
}
