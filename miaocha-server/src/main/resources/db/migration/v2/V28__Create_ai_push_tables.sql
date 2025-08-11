
-- 1) 前端连接注册表：每个浏览器“标签页通道”的在线状态
CREATE TABLE IF NOT EXISTS channel_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    channel_key     VARCHAR(256) NOT NULL COMMENT '通道键 user:client:page',
    user_id         VARCHAR(64)  NULL COMMENT '用户ID',
    client_id       VARCHAR(64)  NULL COMMENT '前端 clientId',
    page_id         VARCHAR(64)  NULL COMMENT '前端 pageId',
    conversation_id VARCHAR(128) NULL COMMENT '模型会话ID',
    node_id         VARCHAR(64)  NOT NULL COMMENT '承载该连接的应用节点ID',
    ws_conn_id      VARCHAR(128) NOT NULL COMMENT 'WebSocket 会话ID',
    status          VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/OFFLINE',
    last_seen_at    TIMESTAMP(3) NULL DEFAULT NULL COMMENT '最后心跳时间',
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    UNIQUE KEY uk_channel_key (channel_key),
    KEY idx_status (status),
    KEY idx_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推送：前端连接注册表';



-- 2) 每通道的自增序号（保证前端展示顺序）
CREATE TABLE IF NOT EXISTS channel_sequence (
    channel_key VARCHAR(256) NOT NULL COMMENT '通道键',
    seq         BIGINT       NOT NULL DEFAULT 0 COMMENT '当前序号',
    updated_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (channel_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推送：通道序号表';


-- 3) 待推送/已推送动作（Best-effort，无重试/死信）
CREATE TABLE IF NOT EXISTS action_outbox (
     id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
     action_id       VARCHAR(64)  NOT NULL COMMENT '动作ID(UUID)',
     channel_key     VARCHAR(256) NULL     COMMENT '目标通道',
     conversation_id VARCHAR(128) NULL     COMMENT '会话ID',
     action_type     VARCHAR(64)  NOT NULL COMMENT '动作类型',
     tool_name       VARCHAR(128) NULL     COMMENT '来源工具名',
     payload_json    JSON         NULL     COMMENT '动作参数（JSON）',
     `sequence`        BIGINT       NULL     COMMENT '消息序号(每通道自增)',
     target_node_id  VARCHAR(64)  NULL     COMMENT '目标节点ID',
     status          VARCHAR(16)  NOT NULL COMMENT 'PENDING/SENT/ACK/DROPPED',
     error_message   VARCHAR(256) NULL     COMMENT '错误信息',
     created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
     sent_at         TIMESTAMP(3) NULL     COMMENT '发送时间',
     ack_at          TIMESTAMP(3) NULL     COMMENT '确认时间',

     KEY idx_target_status_created (target_node_id, status, created_at),
     KEY idx_channel_status_created (channel_key, status, created_at),
     KEY idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推送：动作待发送表';
