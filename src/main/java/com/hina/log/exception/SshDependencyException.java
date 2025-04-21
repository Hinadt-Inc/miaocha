package com.hina.log.exception;

/**
 * SSH依赖异常，当系统缺少SSH相关依赖命令时抛出
 */
public class SshDependencyException extends SshException {

    public SshDependencyException(String message) {
        super(message);
    }

    public SshDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshDependencyException(Throwable cause) {
        super(cause);
    }
}