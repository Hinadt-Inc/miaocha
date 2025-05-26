package com.hina.log.application.logstash.enums;

import lombok.Getter;

/** 任务操作类型枚举 */
@Getter
public enum TaskOperationType {
    INITIALIZE("初始化"),
    START("启动"),
    STOP("停止"),
    RESTART("重启"),
    UPDATE_CONFIG("更新配置"),
    REFRESH_CONFIG("刷新配置");

    private final String description;

    TaskOperationType(String description) {
        this.description = description;
    }
}
