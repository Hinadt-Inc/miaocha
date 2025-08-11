package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.domain.enums.AIActionStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** AI推送：动作待发送表实体 */
@Data
public class ActionOutbox {
    private Long id;
    private String actionId;
    private String channelKey;
    private String conversationId;
    private String toolName;

    /** 建表用 JSON 类型；这里用 String 承接，Service 里用 ObjectMapper 序列化/反序列化 */
    private String payloadJson;

    private Long sequence;
    private String targetNodeId;

    /** {@link AIActionStatus} */
    private String status; // PENDING / SENT / ACK / DROPPED

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime ackAt;
}
