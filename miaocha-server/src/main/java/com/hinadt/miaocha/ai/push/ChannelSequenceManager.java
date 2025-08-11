package com.hinadt.miaocha.ai.push;

import com.hinadt.miaocha.infrastructure.mapper.ChannelSequenceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通道序号管理器
 *
 * <p>基于数据库的通道序号管理，确保消息的有序性和持久化。 使用数据库原子操作保证并发安全。
 */
@Component
@Slf4j
public class ChannelSequenceManager {

    private final ChannelSequenceMapper channelSequenceMapper;

    public ChannelSequenceManager(ChannelSequenceMapper channelSequenceMapper) {
        this.channelSequenceMapper = channelSequenceMapper;
    }

    /**
     * 获取指定通道的下一个序号
     *
     * @param channelKey 通道键
     * @return 下一个序号
     */
    public Long nextSequence(String channelKey) {
        if (!StringUtils.hasText(channelKey)) {
            throw new IllegalArgumentException("channelKey不能为空");
        }

        try {
            // 先递增序号
            channelSequenceMapper.getAndIncrementSequence(channelKey);
            // 再获取递增后的序号
            Long sequence = channelSequenceMapper.getSequenceAfterIncrement(channelKey);
            log.debug("[ChannelSequence] 生成序号: channelKey={}, sequence={}", channelKey, sequence);
            return sequence;
        } catch (Exception e) {
            log.error(
                    "[ChannelSequence] 生成序号失败: channelKey={}, error={}",
                    channelKey,
                    e.getMessage());
            throw new RuntimeException("生成通道序号失败", e);
        }
    }
}
