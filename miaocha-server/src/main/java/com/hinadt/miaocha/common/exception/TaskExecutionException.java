package com.hinadt.miaocha.common.exception;

/** 任务执行相关异常 */
public class TaskExecutionException extends MiaoChaException {

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
