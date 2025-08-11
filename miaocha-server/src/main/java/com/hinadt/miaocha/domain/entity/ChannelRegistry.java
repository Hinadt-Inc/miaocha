package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.domain.enums.WebSocketChannelStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** AI推送：前端连接注册表实体 */
@Data
public class ChannelRegistry {
    private Long id;
    private String channelKey;
    private String userId;
    private String clientId;
    private String pageId;
    private String conversationId;
    private String nodeId;
    private String wsConnId;

    /** {@link WebSocketChannelStatus} */
    private String status; // ONLINE / OFFLINE

    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
}
