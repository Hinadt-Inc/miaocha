package com.hinadt.miaocha.application.logstash;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Log tail service interface - provides real-time Logstash process log tracking */
public interface LogTailService {

    /**
     * Get and create SSE log stream for specified Logstash instance Creates a self-contained stream
     * with automatic cleanup on connection close
     *
     * @param logstashMachineId Logstash instance ID
     * @param tailLines Number of lines to read from end
     * @return SSE emitter
     */
    SseEmitter getAndCreateLogStream(Long logstashMachineId, Integer tailLines);
}
