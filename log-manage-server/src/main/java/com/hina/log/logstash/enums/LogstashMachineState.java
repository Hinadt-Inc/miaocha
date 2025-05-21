package com.hina.log.logstash.enums;

import lombok.Getter;

/**
 * Logstash机器状态枚举
 * 表示Logstash进程在特定机器上的状态
 */
@Getter
public enum LogstashMachineState {
    INITIALIZING("初始化中"), // 创建过程中的初始状态
    NOT_STARTED("未启动"), // 初始化完成，但未启动
    STARTING("正在启动"), // 启动中
    RUNNING("运行中"), // 运行中
    STOPPING("正在停止"), // 停止中
    START_FAILED("启动失败"), // 启动失败
    STOP_FAILED("停止失败"), // 停止失败
    INITIALIZE_FAILED("初始化失败"); // 初始化失败，可以重新初始化

    private final String description;

    LogstashMachineState(String description) {
        this.description = description;
    }
}
