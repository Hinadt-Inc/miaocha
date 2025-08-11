package com.hinadt.miaocha.domain.dto.ai;

import lombok.Data;

/** SSE action message DTO carrying tool invocation details. */
@Data
public class AISessionActionDTO {
    private String channelKey;
    private String conversationId;
    private String toolName;
    private Object payload;
}
