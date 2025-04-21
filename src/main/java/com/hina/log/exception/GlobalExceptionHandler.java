package com.hina.log.exception;

import com.hina.log.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 关键字语法异常处理
     */
    @ExceptionHandler(KeywordSyntaxException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleKeywordSyntaxException(KeywordSyntaxException e) {
        log.warn("关键字语法异常: {}, 表达式: {}", e.getMessage(), e.getExpression());
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), "关键字表达式语法错误: " + e.getMessage());
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else {
            message = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        log.error("参数校验异常: {}", message);
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 权限不足异常处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足异常: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.PERMISSION_DENIED.getCode(), "没有操作权限");
    }

    /**
     * 日志管理系统异常处理
     */
    @ExceptionHandler(LogManageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleLogManageException(LogManageException e) {
        log.error("日志管理异常: ", e);

        // 根据异常类型处理
        if (e instanceof SshOperationException) {
            return ApiResponse.error(ErrorCode.SSH_OPERATION_FAILED.getCode(), e.getMessage());
        } else if (e instanceof LogstashException) {
            return ApiResponse.error(ErrorCode.LOGSTASH_DEPLOY_FAILED.getCode(), e.getMessage());
        } else if (e instanceof TaskExecutionException) {
            return ApiResponse.error(ErrorCode.TASK_EXECUTION_FAILED.getCode(), e.getMessage());
        }

        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage());
    }

    /**
     * SSH异常处理
     */
    @ExceptionHandler(SshException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleSshException(SshException e) {
        log.error("SSH异常: ", e);

        if (e instanceof SshDependencyException) {
            return ApiResponse.error(ErrorCode.SSH_DEPENDENCY_MISSING.getCode(), e.getMessage());
        }

        return ApiResponse.error(ErrorCode.SSH_COMMAND_FAILED.getCode(), e.getMessage());
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请联系管理员");
    }
}