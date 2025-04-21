package com.hina.log.enums;

import lombok.Getter;

/**
 * 任务操作类型枚举
 */
@Getter
public enum TaskOperationType {
    START("启动"),
    STOP("停止");

    private final String description;

    TaskOperationType(String description) {
        this.description = description;
    }
}