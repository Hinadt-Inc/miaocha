package com.hina.log.domain.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户修改自己密码DTO
 */
@Data
@Schema(description = "用户修改自己密码请求对象")
public class UpdatePasswordDTO {

    @Schema(description = "旧密码", example = "oldPassword123", required = true)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", example = "newPassword123", required = true)
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位之间")
    private String newPassword;
}