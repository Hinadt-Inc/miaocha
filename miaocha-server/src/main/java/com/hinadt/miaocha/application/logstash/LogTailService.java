package com.hinadt.miaocha.application.logstash;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 日志尾部跟踪服务接口 提供实时跟踪Logstash进程日志的功能 */
public interface LogTailService {

    /**
     * 创建日志跟踪任务（不返回SSE流）
     *
     * @param logstashMachineId Logstash实例ID
     * @param tailLines 从末尾开始读取的行数
     */
    void createTailing(Long logstashMachineId, Integer tailLines);

    /**
     * 获取指定Logstash实例的SSE日志流
     *
     * @param logstashMachineId Logstash实例ID
     * @return SSE发射器
     */
    SseEmitter getLogStream(Long logstashMachineId);

    /**
     * 停止指定的日志跟踪任务（幂等操作）
     *
     * @param logstashMachineId Logstash实例ID
     */
    void stopTailing(Long logstashMachineId);

    /** 停止所有日志跟踪任务 */
    void stopAllTailing();
}
