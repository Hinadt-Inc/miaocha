package com.hina.log.exception;

/**
 * 任务执行相关异常
 */
public class TaskExecutionException extends LogManageException {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskExecutionException(Throwable cause) {
        super(cause);
    }
}