package com.hinadt.miaocha.common.exception;

import com.hinadt.miaocha.endpoint.LogTailEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * SSE exception handler scoped to LogTail endpoint.
 *
 * <p>It treats client-disconnect scenarios as normal (No Content) to avoid noisy global error logs
 * when EventSource is closed by the client.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = LogTailEndpoint.class)
@Order(0) // Highest precedence for LogTail endpoint
public class LogTailSseExceptionHandler {

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncDisconnect(
            AsyncRequestNotUsableException e, HttpServletRequest request) {
        log.info(
                "Log tail SSE disconnected (async unusable): uri={}, msg={}",
                request.getRequestURI(),
                safeMessage(e));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbort(
            ClientAbortException e, HttpServletRequest request) {
        log.info(
                "Log tail SSE client aborted connection: uri={}, msg={}",
                request.getRequestURI(),
                safeMessage(e));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleIOException(IOException e, HttpServletRequest request) {
        String msg = safeMessage(e);
        if (msg.contains("Broken pipe") || msg.contains("Connection reset")) {
            log.info("Log tail SSE I/O disconnect: uri={}, msg={}", request.getRequestURI(), msg);
            return ResponseEntity.noContent().build();
        }

        log.error("I/O error during log tail SSE: uri={}, msg={}", request.getRequestURI(), msg, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    private String safeMessage(Throwable t) {
        return t == null || t.getMessage() == null ? "" : t.getMessage();
    }
}
