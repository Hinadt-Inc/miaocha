package com.hinadt.miaocha.ai.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.entity.ActionOutbox;
import com.hinadt.miaocha.domain.enums.AIActionStatus;
import com.hinadt.miaocha.infrastructure.NodeIdProvider;
import com.hinadt.miaocha.infrastructure.mapper.ActionOutboxMapper;
import com.hinadt.miaocha.infrastructure.mapper.ChannelRegistryMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI推送动作发布器
 *
 * <p>负责将AI工具的执行结果发布到消息队列，供前端消费。 提供简洁的API接口，隐藏底层实现细节。
 */
@Component
@Slf4j
public class ActionPushService {

    private final ActionOutboxMapper actionOutboxMapper;
    private final ChannelRegistryMapper channelRegistryMapper;
    private final NodeIdProvider nodeIdProvider;
    private final ObjectMapper objectMapper;
    private final ChannelSequenceManager sequenceManager;

    public ActionPushService(
            ActionOutboxMapper actionOutboxMapper,
            ChannelRegistryMapper channelRegistryMapper,
            NodeIdProvider nodeIdProvider,
            ObjectMapper objectMapper,
            ChannelSequenceManager sequenceManager) {
        this.actionOutboxMapper = actionOutboxMapper;
        this.channelRegistryMapper = channelRegistryMapper;
        this.nodeIdProvider = nodeIdProvider;
        this.objectMapper = objectMapper;
        this.sequenceManager = sequenceManager;
    }

    /**
     * 发布动作到指定通道和会话
     *
     * @param channelKey 通道键
     * @param conversationId 会话ID
     * @param toolName 工具名称
     * @param payload 载荷数据
     * @return 动作ID
     */
    public String publishToChannel(
            String channelKey, String conversationId, String toolName, Object payload) {
        // 执行所有验证
        validateParameters(channelKey, conversationId, toolName);
        validateChannelExists(channelKey);

        // 生成动作ID
        String actionId = UUID.randomUUID().toString();

        // 生成序号
        Long sequence = sequenceManager.nextSequence(channelKey);

        // 序列化载荷
        String payloadJson = serializePayload(payload);

        // 创建动作记录
        ActionOutbox action =
                createActionOutbox(
                        actionId, channelKey, conversationId, toolName, payloadJson, sequence);

        // 保存到数据库
        actionOutboxMapper.insert(action);

        log.debug(
                "[ActionPublisher] 发布动作成功: actionId={}, channelKey={}, toolName={}, sequence={}",
                actionId,
                channelKey,
                toolName,
                sequence);

        return actionId;
    }

    /**
     * 验证参数
     *
     * @param channelKey 通道键
     * @param conversationId 会话ID
     * @param toolName 工具名称
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateParameters(String channelKey, String conversationId, String toolName) {
        if (!StringUtils.hasText(channelKey)) {
            throw new IllegalArgumentException("channelKey不能为空");
        }
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId不能为空");
        }
        if (!StringUtils.hasText(toolName)) {
            throw new IllegalArgumentException("toolName不能为空");
        }
    }

    /**
     * 验证通道是否存在
     *
     * @param channelKey 通道键
     * @throws IllegalArgumentException 如果通道不存在
     */
    private void validateChannelExists(String channelKey) {
        var channelRegistry = channelRegistryMapper.selectByChannelKey(channelKey);
        if (channelRegistry == null) {
            throw new IllegalArgumentException("通道不存在: " + channelKey);
        }
    }

    /**
     * 序列化载荷
     *
     * @param payload 载荷对象
     * @return 序列化后的JSON字符串
     * @throws RuntimeException 如果序列化失败
     */
    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("[ActionPublisher] 序列化载荷失败: error={}", e.getMessage());
            throw new RuntimeException("序列化载荷失败", e);
        }
    }

    /**
     * 创建动作记录
     *
     * @param actionId 动作ID
     * @param channelKey 通道键
     * @param conversationId 会话ID
     * @param toolName 工具名称
     * @param payloadJson 载荷JSON
     * @param sequence 序号
     * @return 动作记录
     */
    private ActionOutbox createActionOutbox(
            String actionId,
            String channelKey,
            String conversationId,
            String toolName,
            String payloadJson,
            Long sequence) {
        ActionOutbox action = new ActionOutbox();
        action.setActionId(actionId);
        action.setChannelKey(channelKey);
        action.setConversationId(conversationId);
        action.setToolName(toolName);
        action.setPayloadJson(payloadJson);
        action.setSequence(sequence);
        action.setTargetNodeId(nodeIdProvider.getNodeId());
        action.setStatus(AIActionStatus.PENDING.name());
        action.setCreatedAt(LocalDateTime.now());
        return action;
    }
}
