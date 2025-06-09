package com.hinadt.miaocha.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 自定义访问拒绝处理器 - 处理权限不足的请求返回JSON响应 */
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        log.error("权限拒绝: {}", accessDeniedException.getMessage());

        ApiResponse<Void> apiResponse;

        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            // 用户已认证但没有足够权限
            apiResponse = ApiResponse.error(ErrorCode.NO_ADMIN_PERMISSION);
        } else {
            // 其他权限问题
            apiResponse = ApiResponse.error(ErrorCode.PERMISSION_DENIED);
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
