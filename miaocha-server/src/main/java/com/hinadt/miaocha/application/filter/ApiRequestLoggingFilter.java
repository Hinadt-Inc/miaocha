package com.hinadt.miaocha.application.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** API请求日志过滤器 记录所有API请求的路径和方法，方便调试 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiRequestLoggingFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestURI = request.getRequestURI();

        // 只记录API请求
        if (requestURI.startsWith("/api/")) {
            log.info(
                    "API Request: {} {} from {}",
                    request.getMethod(),
                    requestURI,
                    request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);

        // API请求完成后记录状态码
        if (requestURI.startsWith("/api/")) {
            log.info(
                    "API Response: {} {} - Status: {}",
                    request.getMethod(),
                    requestURI,
                    response.getStatus());
        }
    }
}
