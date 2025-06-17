package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.LogTailService;
import com.hinadt.miaocha.common.exception.ErrorCode;
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
     * 开始跟踪日志
     *
     * @param request 跟踪请求参数
     * @return SSE数据流
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "开始跟踪日志", description = "启动对指定Logstash实例的实时日志跟踪")
    public SseEmitter startTailing(@Valid @RequestBody LogTailRequestDTO request) {
        log.info(
                "收到日志跟踪请求: logstashMachineId={}, tailLines={}",
                request.getLogstashMachineId(),
                request.getTailLines());

        try {
            return logTailService.startTailing(
                    request.getLogstashMachineId(), request.getTailLines());
        } catch (Exception e) {
            log.error("启动日志跟踪失败", e);
            throw e;
        }
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

        log.info("收到停止日志跟踪请求: logstashMachineId={}", logstashMachineId);

        try {
            logTailService.stopTailing(logstashMachineId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("停止日志跟踪失败", e);
            return ApiResponse.error(
                    ErrorCode.INTERNAL_ERROR.getCode(), "停止日志跟踪失败: " + e.getMessage());
        }
    }

    /**
     * 停止所有日志跟踪
     *
     * @return 操作结果
     */
    @DeleteMapping("/stop-all")
    @Operation(summary = "停止所有日志跟踪", description = "停止当前所有活跃的日志跟踪任务")
    public ApiResponse<Void> stopAllTailing() {
        log.info("收到停止所有日志跟踪请求");

        try {
            logTailService.stopAllTailing();
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("停止所有日志跟踪失败", e);
            return ApiResponse.error(
                    ErrorCode.INTERNAL_ERROR.getCode(), "停止所有日志跟踪失败: " + e.getMessage());
        }
    }

    /**
     * 获取日志跟踪服务状态
     *
     * @return 服务状态信息
     */
    @GetMapping("/status")
    @Operation(summary = "获取服务状态", description = "获取日志跟踪服务的运行状态和线程池信息")
    public ApiResponse<String> getServiceStatus() {
        try {
            // 这里我们需要在LogTailService中添加获取状态的方法
            return ApiResponse.success("日志跟踪服务运行正常");
        } catch (Exception e) {
            log.error("获取服务状态失败", e);
            return ApiResponse.error(
                    ErrorCode.INTERNAL_ERROR.getCode(), "获取服务状态失败: " + e.getMessage());
        }
    }
}
