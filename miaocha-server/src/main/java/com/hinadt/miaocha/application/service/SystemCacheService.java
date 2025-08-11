package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.cache.SystemCacheDTO;
import com.hinadt.miaocha.domain.entity.SystemCacheConfig;
import com.hinadt.miaocha.domain.enums.CacheGroup;
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
     * 获取缓存内容并反序列化为指定类型, 注意获取的是缓存内容，不是缓存整行记录
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @param <T> 数据类型
     * @return 反序列化后的对象，如果不存在则返回空
     */
    <T> Optional<T> getCache(CacheGroup cacheGroup, String cacheKey);

    /**
     * 删除缓存配置
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @return 是否删除成功
     */
    boolean deleteCache(CacheGroup cacheGroup, String cacheKey);

    /**
     * 获取当前用户在指定缓存组的所有缓存数据（带类型转换） 用户信息通过 UserContextUtil 自动获取
     *
     * @param cacheGroup 缓存组
     * @param <T> 数据类型
     * @return 缓存数据列表
     */
    <T> List<SystemCacheDTO<T>> getUserCacheData(CacheGroup cacheGroup);

    /**
     * 批量删除缓存配置
     *
     * @param cacheGroup 缓存组
     * @param cacheKeys 缓存键列表
     * @return 删除的数量
     */
    int batchDeleteCache(CacheGroup cacheGroup, List<String> cacheKeys);
}
