package com.hinadt.miaocha.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** AI推送：通道序号表实体 */
@Data
public class ChannelSequence {
    private String channelKey;
    private Long seq;
    private LocalDateTime updatedAt;
}
