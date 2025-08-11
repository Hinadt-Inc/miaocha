package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.ChannelSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 通道序号映射器 */
@Mapper
public interface ChannelSequenceMapper {

    /**
     * 递增通道序号（原子操作）
     *
     * @param channelKey 通道键
     * @return 影响的行数
     */
    Long getAndIncrementSequence(@Param("channelKey") String channelKey);

    /**
     * 获取递增后的序号
     *
     * @param channelKey 通道键
     * @return 序号
     */
    Long getSequenceAfterIncrement(@Param("channelKey") String channelKey);

    /**
     * 获取当前序号（不递增）
     *
     * @param channelKey 通道键
     * @return 当前序号，如果通道不存在则返回0
     */
    Long getCurrentSequence(@Param("channelKey") String channelKey);

    /**
     * 重置通道序号
     *
     * @param channelKey 通道键
     * @return 影响的行数
     */
    int resetSequence(@Param("channelKey") String channelKey);

    /**
     * 删除通道序号记录
     *
     * @param channelKey 通道键
     * @return 影响的行数
     */
    int deleteSequence(@Param("channelKey") String channelKey);

    /**
     * 根据通道键查询序号记录
     *
     * @param channelKey 通道键
     * @return 通道序号实体
     */
    ChannelSequence selectByChannelKey(@Param("channelKey") String channelKey);
}
