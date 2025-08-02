package com.hinadt.miaocha.domain.entity.enums;

import com.hinadt.miaocha.domain.dto.logsearch.LogSearchCacheDTO;
import lombok.Getter;

/** 系统缓存组枚举 定义不同类型的缓存组及其对应的数据类型 */
@Getter
public enum CacheGroup {

    /** 日志检索条件缓存组 用于缓存用户的日志检索条件配置 */
    LOG_SEARCH_CONDITION("日志检索条件缓存组", LogSearchCacheDTO.class);

    /** 缓存组名称 */
    private final String name;

    /** 缓存数据类型 */
    private final Class<?> dataType;

    CacheGroup(String name, Class<?> dataType) {
        this.name = name;
        this.dataType = dataType;
    }
}
