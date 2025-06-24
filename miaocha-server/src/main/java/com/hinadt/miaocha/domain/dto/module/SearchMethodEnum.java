package com.hinadt.miaocha.domain.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;

/** 搜索方法枚举 定义关键词检索字段支持的搜索方法 */
@Schema(description = "搜索方法枚举")
public enum SearchMethodEnum {
    @Schema(description = "模糊匹配，使用SQL的LIKE操作符，支持通配符匹配")
    LIKE("模糊匹配"),

    @Schema(description = "全词匹配，搜索内容必须包含所有指定的关键词")
    MATCH_ALL("全词匹配"),

    @Schema(description = "任意词匹配，搜索内容包含任意一个指定的关键词即可")
    MATCH_ANY("任意词匹配"),

    @Schema(description = "短语匹配，搜索内容必须包含完整的短语")
    MATCH_PHRASE("短语匹配");

    private final String description;

    SearchMethodEnum(String description) {
        this.description = description;
    }

    @Schema(description = "搜索方法的中文描述")
    public String getDescription() {
        return description;
    }
}
