package com.hinadt.miaocha.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 自定义认证入口点 - 处理未认证的请求返回JSON响应 */
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        log.warn("认证异常: {}", authException.getMessage());

        ApiResponse<Void> apiResponse;

        // 检查是否有JWT相关的异常
        Exception jwtException =
                (Exception) request.getAttribute(JwtAuthenticationFilter.JWT_EXCEPTION_ATTRIBUTE);

        if (jwtException instanceof ExpiredJwtException) {
            // JWT token过期
            apiResponse = ApiResponse.error(ErrorCode.TOKEN_EXPIRED);
        } else if (jwtException != null && "User is disabled".equals(jwtException.getMessage())) {
            // 用户被禁用
            apiResponse = ApiResponse.error(ErrorCode.USER_FORBIDDEN);
        } else if (jwtException != null) {
            // 其他JWT相关异常（格式错误、签名错误等）
            apiResponse = ApiResponse.error(ErrorCode.INVALID_TOKEN);
        } else {
            // 没有提供认证信息或认证信息不足
            apiResponse = ApiResponse.error(ErrorCode.UNAUTHORIZED);
        }

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
