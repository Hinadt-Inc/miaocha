package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.LogTailService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.logstash.LogTailRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 日志尾部跟踪接口 提供实时查看Logstash进程日志的功能 */
@Slf4j
@RestController
@RequestMapping("/api/logstash/log-tail")
@Tag(name = "日志尾部跟踪", description = "实时查看Logstash进程日志")
public class LogTailEndpoint {

    private final LogTailService logTailService;

    public LogTailEndpoint(LogTailService logTailService) {
        this.logTailService = logTailService;
    }

    /**
     * 手动检查用户认证状态 Q：为什么需要手动检查认证？ A：SSE 的接口如果依赖Spring Security的自动认证，会出现 AuthorizationException
     * 异常，怀疑应该是SSE 的 SseEmitter 在异步环境下丢失了 SpringSecurity 的认证信息，在停止日志跟踪接口时日志中总会出现一连串的异常，具
     * 体原因不明，后续分析解决 TODO 分析SSE认证问题
     */
    private void checkAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 创建日志跟踪任务
     *
     * @deprecated 此接口已废弃。直接使用 /stream/{logstashMachineId} 接口即可，该接口会自动创建任务
     * @param request 跟踪请求参数
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(
            summary = "创建日志跟踪任务（已废弃）",
            description =
                    "为指定Logstash实例创建日志跟踪任务。已废弃：直接使用 /stream/{logstashMachineId} 接口即可，该接口会自动创建任务")
    @Deprecated
    public ApiResponse<Void> createTailing(@Valid @RequestBody LogTailRequestDTO request) {
        checkAuthentication();

        log.info(
                "创建日志跟踪任务: logstashMachineId={}, tailLines={}",
                request.getLogstashMachineId(),
                request.getTailLines());

        logTailService.createTailing(request.getLogstashMachineId(), request.getTailLines());
        return ApiResponse.success();
    }

    /**
     * 获取日志跟踪SSE流
     *
     * @param logstashMachineId Logstash实例ID
     * @param token JWT令牌（可选，用于支持EventSource API）
     * @return SSE数据流
     */
    @GetMapping(value = "/stream/{logstashMachineId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "获取日志流",
            description =
                    "获取指定Logstash实例的实时日志SSE流。如果日志跟踪任务不存在，会自动创建任务（默认读取最后500行）。支持通过Authorization头或token查询参数传递JWT令牌")
    public SseEmitter getLogStream(
            @Parameter(description = "Logstash实例ID", required = true) @PathVariable
                    Long logstashMachineId,
            @Parameter(description = "JWT令牌（用于支持EventSource API）", required = false)
                    @RequestParam(required = false)
                    String token) {
        checkAuthentication();

        log.info("获取日志流: logstashMachineId={}", logstashMachineId);
        return logTailService.getLogStream(logstashMachineId);
    }

    /**
     * 停止指定的日志跟踪
     *
     * @param logstashMachineId Logstash实例ID
     * @return 操作结果
     */
    @DeleteMapping("/stop/{logstashMachineId}")
    @Operation(summary = "停止日志跟踪", description = "停止指定Logstash实例的日志跟踪")
    public ApiResponse<Void> stopTailing(
            @Parameter(description = "Logstash实例ID", required = true) @PathVariable
                    Long logstashMachineId) {
        checkAuthentication();

        log.info("停止日志跟踪: logstashMachineId={}", logstashMachineId);

        logTailService.stopTailing(logstashMachineId);
        return ApiResponse.success();
    }

    /**
     * 停止所有日志跟踪
     *
     * @return 操作结果
     */
    @DeleteMapping("/stop-all")
    @Operation(summary = "停止所有日志跟踪", description = "停止当前所有活跃的日志跟踪任务")
    public ApiResponse<Void> stopAllTailing() {
        checkAuthentication();

        log.info("停止所有日志跟踪");

        logTailService.stopAllTailing();
        return ApiResponse.success();
    }
}
