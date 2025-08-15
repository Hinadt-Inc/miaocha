package com.hinadt.miaocha.domain.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** AI 会话响应 */
@Data
@Schema(description = "AI会话响应")
public class AISessionResponseDTO {

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "AI回复内容")
    private String content;
}
