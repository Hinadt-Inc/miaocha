package com.hinadt.miaocha.common.exception;

/** 日志管理系统基础异常类 所有应用内的异常应继承自此类 */
public class MiaoChaException extends RuntimeException {

    public MiaoChaException(String message) {
        super(message);
    }

    public MiaoChaException(String message, Throwable cause) {
        super(message, cause);
    }

    public MiaoChaException(Throwable cause) {
        super(cause);
    }
}
