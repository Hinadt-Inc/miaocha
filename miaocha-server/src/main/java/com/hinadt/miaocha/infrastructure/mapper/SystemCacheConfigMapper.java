package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.SystemCacheConfig;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 系统缓存配置Mapper接口 */
@Mapper
public interface SystemCacheConfigMapper {

    /**
     * 插入缓存配置
     *
     * @param config 缓存配置
     * @return 影响行数
     */
    int insert(SystemCacheConfig config);

    /**
     * 批量插入缓存配置
     *
     * @param configs 缓存配置列表
     * @return 影响行数
     */
    int batchInsert(@Param("configs") List<SystemCacheConfig> configs);

    /**
     * 根据ID查询缓存配置
     *
     * @param id 主键ID
     * @return 缓存配置
     */
    SystemCacheConfig selectById(Long id);

    /**
     * 根据缓存组和键查询配置
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @return 缓存配置
     */
    SystemCacheConfig selectByGroupAndKey(
            @Param("cacheGroup") String cacheGroup, @Param("cacheKey") String cacheKey);

    /**
     * 根据缓存组查询所有配置
     *
     * @param cacheGroup 缓存组
     * @return 缓存配置列表
     */
    List<SystemCacheConfig> selectByGroup(String cacheGroup);

    /**
     * 根据缓存组和键列表批量查询
     *
     * @param cacheGroup 缓存组
     * @param cacheKeys 缓存键列表
     * @return 缓存配置列表
     */
    List<SystemCacheConfig> selectByGroupAndKeys(
            @Param("cacheGroup") String cacheGroup, @Param("cacheKeys") List<String> cacheKeys);

    /**
     * 查询所有缓存配置
     *
     * @return 缓存配置列表
     */
    List<SystemCacheConfig> selectAll();

    /**
     * 根据缓存组和创建人查询配置（利用组合索引）
     *
     * @param cacheGroup 缓存组
     * @param createUser 创建人
     * @return 缓存配置列表
     */
    List<SystemCacheConfig> selectByGroupAndCreateUser(
            @Param("cacheGroup") String cacheGroup, @Param("createUser") String createUser);

    /**
     * 根据ID删除缓存配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据缓存组和键删除配置
     *
     * @param cacheGroup 缓存组
     * @param cacheKey 缓存键
     * @return 影响行数
     */
    int deleteByGroupAndKey(
            @Param("cacheGroup") String cacheGroup, @Param("cacheKey") String cacheKey);

    /**
     * 根据缓存组删除所有配置
     *
     * @param cacheGroup 缓存组
     * @return 影响行数
     */
    int deleteByGroup(String cacheGroup);

    /**
     * 根据ID列表批量删除
     *
     * @param ids ID列表
     * @return 影响行数
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据缓存组和键列表批量删除
     *
     * @param cacheGroup 缓存组
     * @param cacheKeys 缓存键列表
     * @return 影响行数
     */
    int batchDeleteByGroupAndKeys(
            @Param("cacheGroup") String cacheGroup, @Param("cacheKeys") List<String> cacheKeys);

    /**
     * 统计指定缓存组的配置数量
     *
     * @param cacheGroup 缓存组
     * @return 配置数量
     */
    int countByGroup(String cacheGroup);

    /**
     * 统计总配置数量
     *
     * @return 总配置数量
     */
    int countAll();

    /**
     * 根据ID更新缓存内容
     *
     * @param id 主键ID
     * @param content 新的缓存内容
     * @return 影响行数
     */
    int updateContent(@Param("id") Long id, @Param("content") String content);
}
