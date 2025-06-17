package com.hinadt.miaocha.domain.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 日志跟踪响应状态枚举 - 对应SSE推送的状态 */
@Getter
@AllArgsConstructor
public enum LogTailResponseStatus {

    /** 已连接，正在传输数据 */
    CONNECTED("已连接"),

    /** 错误状态 */
    ERROR("错误状态"),

    /** 心跳包 */
    HEARTBEAT("心跳包");

    private final String description;
}
