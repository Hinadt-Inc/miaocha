package com.hinadt.miaocha.common.exception;

/** SSH操作运行时异常，用于包装checked的SshException */
public class SshOperationException extends MiaoChaException {

    public SshOperationException(String message) {
        super(message);
    }

    public SshOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshOperationException(SshException cause) {
        super(cause.getMessage(), cause);
    }
}
