package com.hinadt.miaocha.common.exception;

import com.hinadt.miaocha.domain.dto.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理 */
@Slf4j
@RestControllerAdvice
@Order(2) // 较低优先级，在NoResourceFoundExceptionHandler之后处理
public class GlobalExceptionHandler {

    /** 创建带有UTF-8编码的ResponseEntity */
    private <T> ResponseEntity<T> createResponseEntityWithUtf8(T body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        return new ResponseEntity<>(body, headers, status);
    }

    /** 业务异常处理 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        return createResponseEntityWithUtf8(response, HttpStatus.BAD_REQUEST);
    }

    /** 关键字语法异常处理 */
    @ExceptionHandler(KeywordSyntaxException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeywordSyntaxException(
            KeywordSyntaxException e) {
        log.warn("关键字语法异常: {}, 表达式: {}", e.getMessage(), e.getExpression());
        ApiResponse<Void> response =
                ApiResponse.error(
                        ErrorCode.KEY_WORD_QUERY_SYNTAX_ERROR.getCode(),
                        "关键字表达式语法错误: " + e.getMessage());
        return createResponseEntityWithUtf8(response, HttpStatus.BAD_REQUEST);
    }

    /** 查询字段不存在异常处理 */
    @ExceptionHandler(QueryFieldNotExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleQueryFieldNotExistsException(
            QueryFieldNotExistsException e) {
        ApiResponse<Void> response =
                ApiResponse.error(
                        ErrorCode.MODULE_QUERY_FIELD_NOT_EXISTS.getCode(), e.getMessage());
        return createResponseEntityWithUtf8(response, HttpStatus.BAD_REQUEST);
    }

    /** 参数校验异常处理 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            message =
                    ((MethodArgumentNotValidException) e)
                            .getBindingResult().getFieldErrors().stream()
                                    .map(FieldError::getDefaultMessage)
                                    .collect(Collectors.joining(", "));
        } else {
            message =
                    ((BindException) e)
                            .getBindingResult().getFieldErrors().stream()
                                    .map(FieldError::getDefaultMessage)
                                    .collect(Collectors.joining(", "));
        }
        log.error("参数校验异常: {}", message);
        ApiResponse<Void> response =
                ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
        return createResponseEntityWithUtf8(response, HttpStatus.BAD_REQUEST);
    }

    /** 权限不足异常处理 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足异常: {}", e.getMessage());
        ApiResponse<Void> response =
                ApiResponse.error(ErrorCode.PERMISSION_DENIED.getCode(), "没有操作权限");
        return createResponseEntityWithUtf8(response, HttpStatus.FORBIDDEN);
    }

    /** 日志管理系统异常处理 */
    @ExceptionHandler(MiaoChaException.class)
    public ResponseEntity<ApiResponse<Void>> handleLogManageException(MiaoChaException e) {
        log.error("日志管理异常: ", e);

        ApiResponse<Void> response;
        // 根据异常类型处理
        if (e instanceof SshOperationException) {
            response = ApiResponse.error(ErrorCode.SSH_OPERATION_FAILED.getCode(), e.getMessage());
        } else if (e instanceof LogstashException) {
            response =
                    ApiResponse.error(ErrorCode.LOGSTASH_DEPLOY_FAILED.getCode(), e.getMessage());
        } else if (e instanceof TaskExecutionException) {
            response = ApiResponse.error(ErrorCode.TASK_EXECUTION_FAILED.getCode(), e.getMessage());
        } else {
            response = ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage());
        }

        return createResponseEntityWithUtf8(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** SSH异常处理 */
    @ExceptionHandler(SshException.class)
    public ResponseEntity<ApiResponse<Void>> handleSshException(SshException e) {
        log.error("SSH异常: ", e);
        ApiResponse<Void> response =
                ApiResponse.error(ErrorCode.SSH_COMMAND_FAILED.getCode(), e.getMessage());
        return createResponseEntityWithUtf8(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** 通用异常处理 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);
        ApiResponse<Void> response =
                ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请联系管理员, " + e.getMessage());
        return createResponseEntityWithUtf8(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
