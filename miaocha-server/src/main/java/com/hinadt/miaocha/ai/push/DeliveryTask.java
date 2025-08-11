package com.hinadt.miaocha.ai.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.entity.ActionOutbox;
import com.hinadt.miaocha.infrastructure.NodeIdProvider;
import com.hinadt.miaocha.infrastructure.mapper.ActionOutboxMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 定时投递 ACTION 到前端（无重试，允许丢失消息） */
@Slf4j
@Component
public class DeliveryTask {

    private final ActionOutboxMapper outboxMapper;
    private final PushWebSocketHandler pushHandler;
    private final NodeIdProvider nodeIdProvider;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager txManager;

    @Value("${ai.push.batch-size:5}")
    private int batchSize;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DeliveryTask(
            ActionOutboxMapper outboxMapper,
            PushWebSocketHandler pushHandler,
            NodeIdProvider nodeIdProvider,
            ObjectMapper objectMapper,
            PlatformTransactionManager txManager) {
        this.outboxMapper = outboxMapper;
        this.pushHandler = pushHandler;
        this.nodeIdProvider = nodeIdProvider;
        this.objectMapper = objectMapper;
        this.txManager = txManager;
    }

    @Scheduled(fixedDelayString = "${ai.push.fixed-delay-ms:500}")
    public void tick() {
        if (!running.compareAndSet(false, true)) return; // 防并发重入
        try {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.executeWithoutResult(
                    it -> {
                        ;
                        runOnce();
                    });
        } catch (Exception e) {
            log.warn("[DeliveryTask] tick error: {}", e.toString());
        } finally {
            running.set(false);
        }
    }

    /** 单次取数并发送（供定时任务或测试调用） */
    public void runOnce() {
        final String nodeId = nodeIdProvider.getNodeId();
        final int limit = Math.max(1, batchSize);
        List<ActionOutbox> pending = outboxMapper.selectPendingByNode(nodeId, limit);
        if (pending == null || pending.isEmpty()) return;

        for (ActionOutbox it : pending) {
            String channelKey = it.getChannelKey();
            JsonNode payload = null;
            try {
                if (it.getPayloadJson() != null) {
                    payload = objectMapper.readTree(it.getPayloadJson());
                } else {
                    payload = objectMapper.nullNode();
                }
            } catch (Exception parseEx) {
                log.warn(
                        "[DeliveryTask] payload parse error, actionId={}, err={}",
                        it.getActionId(),
                        parseEx.toString());
                payload = objectMapper.nullNode();
            }

            boolean sent = false;
            try {
                sent =
                        pushHandler.sendAction(
                                channelKey,
                                it.getActionId(),
                                it.getSequence() == null ? 0L : it.getSequence(),
                                it.getToolName(),
                                it.getConversationId(),
                                payload);
            } catch (Exception sendEx) {
                log.warn(
                        "[DeliveryTask] send error, actionId={}, err={}",
                        it.getActionId(),
                        sendEx.toString());
            }

            if (sent) {
                outboxMapper.updateStatusSent(it.getActionId(), LocalDateTime.now());
            } else {
                outboxMapper.updateStatusDropped(it.getActionId(), "NO_SESSION_OR_SEND_FAIL");
            }
        }
    }
}
