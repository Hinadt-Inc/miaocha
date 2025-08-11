package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.ChannelRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChannelRegistryMapper {

    ChannelRegistry selectByChannelKey(@Param("channelKey") String channelKey);

    /** INSERT 存在时 UPDATE（以 channel_key 唯一） */
    int insertOrUpdate(ChannelRegistry registry);

    int updateStatusOffline(@Param("channelKey") String channelKey);

    int updateStatusOnline(
            @Param("channelKey") String channelKey,
            @Param("nodeId") String nodeId,
            @Param("wsConnId") String wsConnId);

    int updateLastSeenAt(@Param("channelKey") String channelKey);
}
