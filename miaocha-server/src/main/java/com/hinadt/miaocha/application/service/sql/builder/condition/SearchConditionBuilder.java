package com.hinadt.miaocha.application.service.sql.builder.condition;

import com.hinadt.miaocha.domain.dto.LogSearchDTO;

/** 搜索条件构建器接口 */
public interface SearchConditionBuilder {
    /**
     * 判断当前构建器是否支持该搜索条件
     *
     * @param dto 日志搜索DTO
     * @return 是否支持
     */
    boolean supports(LogSearchDTO dto);

    /**
     * 构建搜索条件SQL片段
     *
     * @param dto 日志搜索DTO
     * @return SQL条件片段（不包含前置AND）
     */
    String buildCondition(LogSearchDTO dto);
}
