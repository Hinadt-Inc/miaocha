package com.hina.log.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hina.log.domain.dto.ApiResponse;
import com.hina.log.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义认证入口点 - 处理未认证的请求返回JSON响应
 */
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {
        log.warn("认证异常: {}", authException.getMessage());

        ApiResponse<Void> apiResponse;

        // 从request attributes获取JWT处理中的异常（如果有）
        Exception exception = (Exception) request.getAttribute("jwtException");

        if (exception instanceof ExpiredJwtException) {
            // 令牌过期
            apiResponse = ApiResponse.error(ErrorCode.TOKEN_EXPIRED);
        } else {
            // 其他认证异常
            apiResponse = ApiResponse.error(ErrorCode.INVALID_TOKEN);
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}