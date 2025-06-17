package com.hinadt.miaocha.application.logstash;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 日志尾部跟踪服务接口 提供实时跟踪Logstash进程日志的功能 */
public interface LogTailService {

    /**
     * 开始跟踪指定Logstash实例的日志
     *
     * @param logstashMachineId Logstash实例ID
     * @param tailLines 从末尾开始读取的行数
     * @return SSE发射器
     */
    SseEmitter startTailing(Long logstashMachineId, Integer tailLines);

    /**
     * 停止指定的日志跟踪任务
     *
     * @param logstashMachineId Logstash实例ID
     */
    void stopTailing(Long logstashMachineId);

    /** 停止所有日志跟踪任务 */
    void stopAllTailing();
}
