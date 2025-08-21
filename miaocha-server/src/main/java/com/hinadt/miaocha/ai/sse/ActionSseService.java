package com.hinadt.miaocha.ai.sse;

import com.hinadt.miaocha.domain.dto.ai.AISessionActionDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Sends tool action invocations to frontend via SSE. */
@Component
@Slf4j
public class ActionSseService {

    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    /** Register an emitter for the given conversation. */
    public void register(String conversationId, SseEmitter emitter) {
        sessions.put(conversationId, emitter);
        emitter.onCompletion(() -> sessions.remove(conversationId));
        emitter.onTimeout(() -> sessions.remove(conversationId));
    }

    /**
     * Push action information through SSE to the connected frontend.
     *
     * @param conversationId id of the conversation associated with the action
     * @param toolName name of the invoked tool
     * @param payload tool parameters
     */
    public void sendAction(String conversationId, String toolName, Object payload) {
        SseEmitter emitter = sessions.get(conversationId);
        if (emitter == null) {
            log.warn("No SSE emitter available for action: {}", toolName);
            return;
        }
        AISessionActionDTO dto = new AISessionActionDTO();
        dto.setConversationId(conversationId);
        dto.setToolName(toolName);
        dto.setPayload(payload);
        try {
            emitter.send(SseEmitter.event().name("action").data(dto));
        } catch (Exception e) {
            log.error("Failed to send action via SSE: {}", e.getMessage(), e);
        }
    }
}
