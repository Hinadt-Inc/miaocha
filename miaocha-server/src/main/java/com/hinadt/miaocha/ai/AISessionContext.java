package com.hinadt.miaocha.ai;

import org.slf4j.MDC;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 会话上下文 基于 MDC 存储 channelKey 和 conversationId 适用于单线程（如 Spring MVC 同步链路）环境。 */
public class AISessionContext {

    private static final String CHANNEL_KEY = "channelKey";
    private static final String CONVERSATION_ID = "conversationId";

    private static final ThreadLocal<SseEmitter> EMITTER = new ThreadLocal<>();

    private AISessionContext() {}

    /** 一次性设置会话上下文 */
    public static void set(String channelKey, String conversationId) {
        if (channelKey != null) {
            MDC.put(CHANNEL_KEY, channelKey);
        } else {
            MDC.remove(CHANNEL_KEY);
        }
        if (conversationId != null) {
            MDC.put(CONVERSATION_ID, conversationId);
        } else {
            MDC.remove(CONVERSATION_ID);
        }
    }

    public static void setEmitter(SseEmitter emitter) {
        if (emitter != null) {
            EMITTER.set(emitter);
        } else {
            EMITTER.remove();
        }
    }

    public static String getChannelKey() {
        return MDC.get(CHANNEL_KEY);
    }

    public static String getConversationId() {
        return MDC.get(CONVERSATION_ID);
    }

    public static void clear() {
        MDC.remove(CHANNEL_KEY);
        MDC.remove(CONVERSATION_ID);
        EMITTER.remove();
    }

    public static SseEmitter getEmitter() {
        return EMITTER.get();
    }
}
