package com.hinadt.miaocha.common.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * 缓存键工具类
 *
 * <p>提供缓存键生成相关的工具方法和常量定义
 */
public class CacheKeyUtils {

    /** 搜索条件缓存键前缀 */
    public static final String SEARCH_CONDITION_PREFIX = "search_condition";

    /** 缓存键分隔符 */
    public static final String CACHE_KEY_SEPARATOR = "_";

    /**
     * 生成搜索条件缓存键
     *
     * <p>格式：search_condition_随机nanoId
     *
     * @return 生成的缓存键
     */
    public static String generateSearchConditionKey() {
        String nanoId = generateNanoId();
        return SEARCH_CONDITION_PREFIX + CACHE_KEY_SEPARATOR + nanoId;
    }

    /**
     * 生成随机nanoId
     *
     * @return 随机生成的nanoId
     */
    public static String generateNanoId() {
        return NanoIdUtils.randomNanoId();
    }

    private CacheKeyUtils() {
        // 工具类，禁止实例化
    }
}
