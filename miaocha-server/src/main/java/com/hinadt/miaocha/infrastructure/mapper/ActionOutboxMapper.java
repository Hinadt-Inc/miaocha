package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.ActionOutbox;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActionOutboxMapper {

    int insert(ActionOutbox record);

    /** 拉取当前节点待发送消息（加行级锁） */
    List<ActionOutbox> selectPendingByNode(
            @Param("nodeId") String nodeId, @Param("limit") int limit);

    int updateStatusSent(@Param("actionId") String actionId, @Param("sentAt") LocalDateTime sentAt);

    int updateStatusDropped(
            @Param("actionId") String actionId, @Param("errorMessage") String errorMessage);

    int updateStatusAck(@Param("actionId") String actionId, @Param("ackAt") LocalDateTime ackAt);
}
