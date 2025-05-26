package com.hina.log.common.exception;

/** Logstash相关操作异常 */
public class LogstashException extends LogManageException {

    public LogstashException(String message) {
        super(message);
    }

    public LogstashException(String message, Throwable cause) {
        super(message, cause);
    }

    public LogstashException(Throwable cause) {
        super(cause);
    }
}
