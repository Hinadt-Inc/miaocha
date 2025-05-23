package com.hina.log.application.logstash.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {
    PENDING("待执行"),
    RUNNING("执行中"),
    COMPLETED("已完成"),
    FAILED("失败"),
    CANCELLED("已取消");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }
}