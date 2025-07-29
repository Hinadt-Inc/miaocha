package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.entity.SystemCacheConfig;
import com.hinadt.miaocha.domain.entity.enums.CacheGroup;
import java.util.List;
import java.util.Optional;

/** 系统缓存服务接口 提供系统缓存配置的管理功能 */
public interface SystemCacheService {

    /**
     * 保存缓存配置（通过对象） 将对象序列化为JSON后保存 创建用户信息会通过 UserAuditInterceptor 自动设置
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @param data 要缓存的数据对象
     * @param <T> 数据类型
     * @return 保存的缓存配置
     */
    <T> SystemCacheConfig saveCache(CacheGroup cacheGroup, String cacheKey, T data);

    /**
     * 获取缓存内容并反序列化为指定类型
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @param clazz 目标类型
     * @param <T> 数据类型
     * @return 反序列化后的对象，如果不存在则返回空
     */
    <T> Optional<T> getCache(CacheGroup cacheGroup, String cacheKey, Class<T> clazz);

    /**
     * 获取当前用户在指定缓存组的所有缓存配置 用户信息通过 UserContextUtil 自动获取
     *
     * @param cacheGroup 缓存组
     * @return 缓存配置列表
     */
    List<SystemCacheConfig> getUserCaches(CacheGroup cacheGroup);

    /**
     * 删除缓存配置
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @return 是否删除成功
     */
    boolean deleteCache(CacheGroup cacheGroup, String cacheKey);
}
