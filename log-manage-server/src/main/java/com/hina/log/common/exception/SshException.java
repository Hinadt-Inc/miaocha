package com.hina.log.common.exception;

/** SSH操作异常 */
public class SshException extends Exception {

    public SshException(String message) {
        super(message);
    }

    public SshException(String message, Throwable cause) {
        super(message, cause);
    }

    public SshException(Throwable cause) {
        super(cause);
    }
}
