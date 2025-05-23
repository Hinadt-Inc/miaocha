package com.hina.log.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求DTO
 */
@Data
@Schema(description = "刷新令牌请求对象")
public class RefreshTokenRequestDTO {

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}