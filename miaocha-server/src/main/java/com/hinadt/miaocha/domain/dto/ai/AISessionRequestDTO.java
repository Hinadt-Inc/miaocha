package com.hinadt.miaocha.domain.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI 会话请求 */
@Data
@Schema(description = "AI会话请求")
public class AISessionRequestDTO {

    @Schema(description = "会话ID，不传则创建新会话")
    private String conversationId;

    @Schema(description = "用户输入的消息", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "message不能为空")
    private String message;
}
