package com.hina.log.application.logstash.enums;

import lombok.Getter;

/** 步骤状态枚举 */
@Getter
public enum StepStatus {
    PENDING("待执行"),
    RUNNING("执行中"),
    COMPLETED("已完成"),
    FAILED("失败"),
    SKIPPED("已跳过");

    private final String description;

    StepStatus(String description) {
        this.description = description;
    }
}
