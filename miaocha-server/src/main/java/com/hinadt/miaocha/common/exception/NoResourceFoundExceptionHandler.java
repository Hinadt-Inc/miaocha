package com.hinadt.miaocha.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 专门处理NoResourceFoundException的异常处理器 - 提供SPA路由支持 */
@Slf4j
@RestControllerAdvice
@Order(1) // 高优先级，在GlobalExceptionHandler之前处理
public class NoResourceFoundExceptionHandler {

    // 配置化的API路径前缀
    private static final List<String> API_PATH_PREFIXES =
            List.of("/api/", "/swagger-ui/", "/v3/", "/actuator/");

    /** 资源未找到异常处理 - SPA路由支持 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(
            NoResourceFoundException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.debug("资源未找到，尝试SPA路由处理: {}", requestUri);

        if (isApiRequest(requestUri)) {
            return createApiNotFoundResponse();
        }

        return createSpaRouteResponse();
    }

    /** 判断是否为API请求 */
    private boolean isApiRequest(String requestUri) {
        return requestUri != null && API_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }

    /** 创建API 404响应 */
    private ResponseEntity<String> createApiNotFoundResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        return new ResponseEntity<>(
                "{\"code\":404,\"message\":\"请求的API接口不存在，请检查URL路径是否正确\",\"data\":null}",
                headers,
                HttpStatus.NOT_FOUND);
    }

    /** 创建SPA路由响应 */
    private ResponseEntity<String> createSpaRouteResponse() {
        String indexContent = loadIndexHtml();
        if (indexContent != null) {
            return createHtmlResponse(indexContent, HttpStatus.OK);
        }

        String friendlyMessage = loadTemplate("templates/404-friendly.html");
        return createHtmlResponse(friendlyMessage, HttpStatus.NOT_FOUND);
    }

    /** 创建HTML响应 */
    private ResponseEntity<String> createHtmlResponse(String content, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8));
        return new ResponseEntity<>(content, headers, status);
    }

    /** 加载index.html */
    private String loadIndexHtml() {
        try {
            ClassPathResource indexResource = new ClassPathResource("static/index.html");
            if (indexResource.exists()) {
                return new String(
                        indexResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Failed to load index.html: {}", e.getMessage());
        }
        return null;
    }

    /** 加载模板文件 */
    private String loadTemplate(String templatePath) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("加载模板文件失败: {}", templatePath, e);
        }
        return "<!DOCTYPE html><html><body><h1>页面未找到</h1><p><a"
                + " href=\"/\">返回首页</a></p></body></html>";
    }
}
