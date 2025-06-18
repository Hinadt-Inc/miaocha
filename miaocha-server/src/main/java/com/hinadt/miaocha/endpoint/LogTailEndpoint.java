package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.LogTailService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.logstash.LogTailRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
     * 创建日志跟踪任务
     *
     * @param request 跟踪请求参数
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建日志跟踪任务", description = "为指定Logstash实例创建日志跟踪任务")
    public ApiResponse<Void> createTailing(@Valid @RequestBody LogTailRequestDTO request) {
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
            description = "获取指定Logstash实例的实时日志SSE流。支持通过Authorization头或token查询参数传递JWT令牌")
    public SseEmitter getLogStream(
            @Parameter(description = "Logstash实例ID", required = true) @PathVariable
                    Long logstashMachineId,
            @Parameter(description = "JWT令牌（用于支持EventSource API）", required = false)
                    @RequestParam(required = false)
                    String token) {
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
        log.info("停止所有日志跟踪");

        logTailService.stopAllTailing();
        return ApiResponse.success();
    }
}
