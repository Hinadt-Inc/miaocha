package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.LogTailService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Log tail endpoint - provides real-time viewing of Logstash process logs */
@Slf4j
@RestController
@RequestMapping("/api/logstash/log-tail")
@Tag(name = "Log Tail", description = "Real-time Logstash process log viewing")
public class LogTailEndpoint {

    private final LogTailService logTailService;

    public LogTailEndpoint(LogTailService logTailService) {
        this.logTailService = logTailService;
    }

    /**
     * Manually check user authentication status Q: Why do we need to manually check authentication?
     * A: If SSE interfaces rely on Spring Security's automatic authentication,
     * AuthorizationException occurs. It's suspected that SseEmitter loses SpringSecurity
     * authentication information in async environments. TODO: Analyze SSE authentication issues
     */
    private void checkAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * Get log stream for Logstash instance
     *
     * @param logstashMachineId Logstash instance ID
     * @param tailLines Number of lines to tail from end (optional, default 500)
     * @param token JWT token (optional, for EventSource API support)
     * @return SSE data stream
     */
    @GetMapping(value = "/stream/{logstashMachineId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Get log stream",
            description =
                    "Get real-time log SSE stream for specified Logstash instance. Creates"
                            + " self-contained stream with automatic cleanup on connection close.")
    public SseEmitter getLogStream(
            @Parameter(description = "Logstash instance ID", required = true) @PathVariable
                    Long logstashMachineId,
            @Parameter(description = "Number of lines to tail from end", required = false)
                    @RequestParam(required = false, defaultValue = "500")
                    Integer tailLines,
            @Parameter(description = "JWT token (for EventSource API support)", required = false)
                    @RequestParam(required = false)
                    String token) {
        checkAuthentication();

        log.info(
                "Get log stream: logstashMachineId={}, tailLines={}", logstashMachineId, tailLines);
        return logTailService.getLogStream(logstashMachineId, tailLines);
    }
}
