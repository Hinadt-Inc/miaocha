package com.hinadt.miaocha.ai.sse;

import com.hinadt.miaocha.ai.AISessionContext;
import com.hinadt.miaocha.domain.dto.ai.AISessionActionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Sends tool action invocations to frontend via SSE. */
@Component
@Slf4j
public class ActionSseService {

    /**
     * Push action information through SSE to the connected frontend.
     *
     * @param toolName name of the invoked tool
     * @param payload tool parameters
     */
    public void sendAction(String toolName, Object payload) {
        SseEmitter emitter = AISessionContext.getEmitter();
        if (emitter == null) {
            log.warn("No SSE emitter available for action: {}", toolName);
            return;
        }
        AISessionActionDTO dto = new AISessionActionDTO();
        dto.setChannelKey(AISessionContext.getChannelKey());
        dto.setConversationId(AISessionContext.getConversationId());
        dto.setToolName(toolName);
        dto.setPayload(payload);
        try {
            emitter.send(SseEmitter.event().name("action").data(dto));
        } catch (Exception e) {
            log.error("Failed to send action via SSE: {}", e.getMessage(), e);
        }
    }
}
