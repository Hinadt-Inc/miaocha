package com.hinadt.miaocha.config.filter;

import com.hinadt.miaocha.common.util.LogIdContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;
import java.util.StringJoiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * API请求日志过滤器
 *
 * <p>记录所有API请求的路径和方法，并为每个API请求生成和管理 logId，确保整个请求生命周期中的日志追踪。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiRequestLoggingFilter implements Filter {

    private static final String API_PREFIX = "/api/";
    private static final String LOGIN_API = "/api/auth/login";
    private static final int MAX_BODY_SIZE = 100 * 1024; // 100KB
    public static final String APPLICATION_JSON = "application/json";

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();

        // 默认使用原始 request
        HttpServletRequest actualRequest = request;

        boolean isApi = requestURI.startsWith(API_PREFIX);
        boolean isJson = isJsonContentType(request);

        try {
            if (isApi) {
                LogIdContext.setLogId(LogIdContext.generateLogId());

                if (isJson && !requestURI.startsWith(LOGIN_API)) {
                    // 只包装 application/json 请求
                    actualRequest = new CachedRequestWrapper(request);
                    logRequest((CachedRequestWrapper) actualRequest);
                } else {
                    // 非 JSON 也记录基础信息
                    log.info(
                            "API Request: {} {} from {} with query: {}",
                            request.getMethod(),
                            request.getRequestURI(),
                            request.getRemoteAddr(),
                            getQueryString(request));
                }
            }

            filterChain.doFilter(actualRequest, response);

        } finally {
            if (isApi) {
                try {
                    log.info(
                            "API Response: {} {} - Status: {}",
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus());
                } finally {
                    LogIdContext.clear();
                }
            }
        }
    }

    private void logRequest(CachedRequestWrapper request) {
        String queryString = getQueryString(request);
        String requestBody = getRequestBody(request);
        log.info(
                "API Request: {} {} from {} with query: {} and body: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                queryString,
                requestBody);
    }

    private String getQueryString(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) return "";
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            joiner.add(entry.getKey() + "=" + String.join(",", entry.getValue()));
        }
        return joiner.toString();
    }

    private String getRequestBody(CachedRequestWrapper request) {
        byte[] content = request.getCachedBody();
        if (content.length == 0) return "";

        if (content.length > MAX_BODY_SIZE) {
            log.warn("Request body too large to log: {} bytes", content.length);
            return "[Body too large (>100KB)]";
        }

        try {
            return new String(content, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to decode request body", e);
            return "[Error decoding body]";
        }
    }

    private boolean isJsonContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains(APPLICATION_JSON);
    }

    /** 可重复读取 JSON 请求体的包装器，仅支持 application/json。 */
    @Getter
    private static class CachedRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            InputStream inputStream = request.getInputStream();
            this.cachedBody = inputStream.readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 不支持异步 IO
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(
                    new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
    }
}
