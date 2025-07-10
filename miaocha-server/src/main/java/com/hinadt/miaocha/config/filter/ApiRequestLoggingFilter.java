package com.hinadt.miaocha.config.filter;

import com.hinadt.miaocha.common.util.LogIdContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * API请求日志过滤器
 *
 * <p>记录所有API请求的路径和方法，并为每个API请求生成和管理 logId， 确保整个请求生命周期中的日志追踪。
 */
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

        try {
            // 只为API请求生成和管理 logId
            if (requestURI.startsWith("/api/")) {
                // 生成新的 logId 并设置到 MDC
                LogIdContext.setLogId(LogIdContext.generateLogId());

                log.info(
                        "API Request: {} {} from {}",
                        request.getMethod(),
                        requestURI,
                        request.getRemoteAddr());
            }

            // 继续执行过滤器链
            filterChain.doFilter(request, response);

        } finally {
            // 在 finally 块中记录响应和清理资源
            if (requestURI.startsWith("/api/")) {
                try {
                    log.info(
                            "API Response: {} {} - Status: {}",
                            request.getMethod(),
                            requestURI,
                            response.getStatus());
                } finally {
                    // 清理当前线程的 logId，避免内存泄漏
                    LogIdContext.clear();
                }
            }
        }
    }
}
