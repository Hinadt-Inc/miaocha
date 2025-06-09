package com.hinadt.miaocha.common.exception;

/** 日志管理系统基础异常类 所有应用内的异常应继承自此类 */
public class LogManageException extends RuntimeException {

    public LogManageException(String message) {
        super(message);
    }

    public LogManageException(String message, Throwable cause) {
        super(message, cause);
    }

    public LogManageException(Throwable cause) {
        super(cause);
    }
}
