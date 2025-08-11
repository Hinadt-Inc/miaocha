package com.hinadt.miaocha.ai.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.dto.user.UserDTO;
import com.hinadt.miaocha.domain.entity.ChannelRegistry;
import com.hinadt.miaocha.domain.enums.WebSocketChannelStatus;
import com.hinadt.miaocha.infrastructure.NodeIdProvider;
import com.hinadt.miaocha.infrastructure.mapper.ActionOutboxMapper;
import com.hinadt.miaocha.infrastructure.mapper.ChannelRegistryMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;

/**
 * WebSocket 推送处理器
 *
 * <p>支持消息类型： - REGISTER: 绑定通道（clientId + pageId [ + userId + conversationId]） { "type": "REGISTER",
 * "pageId": "xyz", "userId": "u1", }
 *
 * <p>- PING: 心跳，刷新 lastSeenAt { "type": "PING" }
 *
 * <p>- ACK: 前端处理完一个 action 进行确认 { "type": "ACK", "actionId": "uuid", }
 *
 * <p>发送给前端的消息（由 DeliveryTask 调用 sendToChannelKey 发出）：
 *
 * <p>{ "type": "ACTION", "actionId": "...", "sequence": 123, "toolName":
 * "sendSearchLogDetailsAction", "conversationId": "...", "payload": { ... 任意JSON } }
 */
@Component
@Slf4j
public class PushWebSocketHandler implements WebSocketHandler {

    public static final String MESSAGE_TYPE = "type";
    public static final String MESSAGE_REGISTER = "REGISTER";
    public static final String MESSAGE_PING = "PING";
    public static final String MESSAGE_ACK = "ACK";
    public static final String MESSAGE_PONG = "PONG";

    private final ObjectMapper objectMapper;
    private final ChannelRegistryMapper channelRegistryMapper;
    private final ActionOutboxMapper actionOutboxMapper;
    private final NodeIdProvider nodeIdProvider;

    /** channelKey -> session */
    private final Map<String, WebSocketSession> sessionByChannel = new ConcurrentHashMap<>();

    /** sessionId -> channelKey */
    private final Map<String, String> channelBySessionId = new ConcurrentHashMap<>();

    /** sessionId -> wsConnId (for audit) */
    private final Map<String, String> wsConnIdBySessionId = new ConcurrentHashMap<>();

    public PushWebSocketHandler(
            ObjectMapper objectMapper,
            ChannelRegistryMapper channelRegistryMapper,
            ActionOutboxMapper actionOutboxMapper,
            NodeIdProvider nodeIdProvider) {
        this.objectMapper = objectMapper;
        this.channelRegistryMapper = channelRegistryMapper;
        this.actionOutboxMapper = actionOutboxMapper;
        this.nodeIdProvider = nodeIdProvider;
    }

    // ========== WebSocket Handler lifecycle ==========

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 暂不绑定通道，等待前端发送 REGISTER
        String wsConnId = shortId();
        wsConnIdBySessionId.put(session.getId(), wsConnId);
        log.debug("[WS] connected: sessionId={}, wsConnId={}", session.getId(), wsConnId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (!(message instanceof TextMessage text)) {
            log.warn("[WS] non-text message ignored: sessionId={}", session.getId());
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(text.getPayload());
        } catch (Exception e) {
            log.warn("[WS] invalid json from sessionId={}, err={}", session.getId(), e.toString());
            return;
        }
        String type = getText(root, MESSAGE_TYPE);
        if (!StringUtils.hasText(type)) {
            log.warn("[WS] missing type field, sessionId={}", session.getId());
            return;
        }

        switch (type) {
            case MESSAGE_REGISTER -> handleRegister(session, root);
            case MESSAGE_PING -> handlePing(session);
            case MESSAGE_ACK -> handleAck(session, root);
            default -> log.warn("[WS] unknown type={}, sessionId={}", type, session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn(
                "[WS] transport error, sessionId={}, err={}",
                session.getId(),
                exception.toString());
        // 连接层面的错误，等 close 再做清理
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
            throws IOException {
        String sessionId = session.getId();
        String channelKey = channelBySessionId.remove(sessionId);
        wsConnIdBySessionId.remove(sessionId);
        if (channelKey != null) {
            sessionByChannel.remove(channelKey).close();
            try {
                channelRegistryMapper.updateStatusOffline(channelKey);
            } catch (Exception e) {
                log.warn("[WS] mark OFFLINE failed for {}, err={}", channelKey, e.toString());
            }
            log.info("[WS] closed: channelKey={}, status={}", channelKey, closeStatus);
        } else {
            log.info("[WS] closed: unbound sessionId={}, status={}", sessionId, closeStatus);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ========== Message handlers ==========

    private void handleRegister(WebSocketSession session, JsonNode root) {
        String clientId = getText(root, "clientId");
        String pageId = getText(root, "pageId");

        String userId = null;
        if (session.getPrincipal() instanceof UserDTO user) {
            userId = String.valueOf(user.getId());
        }

        // conversationId 由服务端生成并返回给前端
        String conversationId = generateConversationId();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(pageId)) {
            sendError(session, MESSAGE_REGISTER, "clientId/pageId required");
            return;
        }

        // channelKey 由 userId/clientId/pageId/conversationId 通过哈希计算得到，长度固定
        String channelKey = buildChannelKey(userId, clientId, pageId);
        String wsConnId = wsConnIdBySessionId.get(session.getId());

        // 绑定内存映射
        sessionByChannel.put(channelKey, session);
        channelBySessionId.put(session.getId(), channelKey);

        // 写/更新注册表 ONLINE
        ChannelRegistry registry = new ChannelRegistry();
        registry.setChannelKey(channelKey);
        registry.setUserId(userId);
        registry.setClientId(clientId);
        registry.setPageId(pageId);
        registry.setConversationId(conversationId);
        registry.setNodeId(nodeIdProvider.getNodeId());
        registry.setWsConnId(wsConnId);
        registry.setStatus(WebSocketChannelStatus.ONLINE.name());
        registry.setLastSeenAt(LocalDateTime.now());

        try {
            channelRegistryMapper.insertOrUpdate(registry);
        } catch (Exception e) {
            log.warn(
                    "[WS] insertOrUpdate registry failed, channelKey={}, err={}",
                    channelKey,
                    e.toString());
        }

        // 回执
        sendJson(
                session,
                objectMapper
                        .createObjectNode()
                        .put(MESSAGE_TYPE, "REGISTERED")
                        .put("channelKey", channelKey)
                        .put("conversationId", conversationId)
                        .put("nodeId", nodeIdProvider.getNodeId())
                        .put("wsConnId", wsConnId));

        log.info(
                "[WS] REGISTER ok: channelKey={}, wsConnId={}, userId={}, clientId={}, pageId={},"
                        + " conversationId={}",
                channelKey,
                wsConnId,
                userId,
                clientId,
                pageId,
                conversationId);
    }

    private void handlePing(WebSocketSession session) {
        String channelKey = channelBySessionId.get(session.getId());
        if (channelKey == null) return;
        try {
            channelRegistryMapper.updateLastSeenAt(channelKey);
        } catch (Exception e) {
            log.debug("[WS] update lastSeenAt failed for {}, err={}", channelKey, e.toString());
        }
        // 可选：回 Pong
        sendJson(session, objectMapper.createObjectNode().put(MESSAGE_TYPE, MESSAGE_PONG));
    }

    private void handleAck(WebSocketSession session, JsonNode root) {
        String actionId = getText(root, "actionId");
        if (!StringUtils.hasText(actionId)) {
            sendError(session, MESSAGE_ACK, "actionId required");
            return;
        }
        try {
            actionOutboxMapper.updateStatusAck(actionId, LocalDateTime.now());
            log.debug("[WS] ACK ok: actionId={}", actionId);
        } catch (Exception e) {
            log.warn("[WS] ACK failed: actionId={}, err={}", actionId, e.toString());
        }
    }

    // ========== Public API for DeliveryTask ==========

    /**
     * 由 DeliveryTask 调用：按 channelKey 发送一条 <ACTION> 消息给前端。
     *
     * <p>该方法会自动包一层 ACTION 协议外壳： { "type": "ACTION", "actionId": "...", "sequence": 123, "toolName":
     * "...", "conversationId": "...", "payload": { ... 任意JSON ... } }
     *
     * @return true: 已发送；false: 无该通道或发送异常
     */
    public boolean sendAction(
            String channelKey,
            String actionId,
            long sequence,
            String toolName,
            String conversationId,
            JsonNode payload) {
        WebSocketSession session = sessionByChannel.get(channelKey);
        if (session == null || !session.isOpen()) return false;
        try {
            JsonNode message =
                    objectMapper
                            .createObjectNode()
                            .put("type", "ACTION")
                            .put("actionId", actionId)
                            .put("sequence", sequence)
                            .put("toolName", toolName)
                            .put("conversationId", conversationId)
                            .set("payload", payload == null ? objectMapper.nullNode() : payload);
            session.sendMessage(new TextMessage(message.toString()));
            return true;
        } catch (IOException e) {
            log.warn("[WS] send ACTION failed for {}, err={}", channelKey, e.toString());
            return false;
        }
    }

    // ========== utils ==========

    private static String buildChannelKey(String userId, String clientId, String pageId) {
        String u = StringUtils.hasText(userId) ? userId : "-";
        String input = u + "|" + clientId + "|" + pageId;
        // 使用 SHA-256 计算哈希，并截取前32位十六进制，保持长度固定
        return sha256Hex(input).substring(0, 32);
    }

    private void sendError(WebSocketSession session, String forType, String msg) {
        sendJson(
                session,
                objectMapper
                        .createObjectNode()
                        .put(MESSAGE_TYPE, "ERROR")
                        .put("for", forType)
                        .put("message", msg));
    }

    private void sendJson(WebSocketSession session, JsonNode obj) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(obj.toString()));
            }
        } catch (IOException e) {
            log.debug("[WS] sendJson failed", e);
        }
    }

    private static String getText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String generateConversationId() {
        return UUID.randomUUID().toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }
}
