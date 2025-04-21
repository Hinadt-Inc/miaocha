package com.hina.log.enums;

import lombok.Getter;

/**
 * Logstash进程状态枚举
 */
@Getter
public enum LogstashProcessState {
    NOT_STARTED("未启动"),
    STARTING("正在启动"),
    RUNNING("运行中"),
    STOPPING("正在停止"),
    FAILED("失败");

    private final String description;

    LogstashProcessState(String description) {
        this.description = description;
    }
}