package com.hina.log.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应对象")
public class LoginResponseDTO {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "令牌过期时间（毫秒时间戳）", example = "1714583272000")
    private Long expiresAt;

    @Schema(description = "刷新令牌过期时间（毫秒时间戳）", example = "1715101672000")
    private Long refreshExpiresAt;

    @Schema(description = "用户角色", example = "ADMIN")
    private String role;
}