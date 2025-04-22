package com.hina.log.controller;

import com.hina.log.dto.*;
import com.hina.log.service.LogstashProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Logstash进程管理控制器
 * 所有操作都需要管理员权限
 */
@RestController
@RequestMapping("/api/logstash/processes")
@Tag(name = "Logstash进程管理", description = "提供Logstash进程的增删改查和启停等功能，需要管理员权限")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LogstashProcessController {

    private final LogstashProcessService logstashProcessService;

    /**
     * 创建Logstash进程
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程
     */
    @PostMapping
    @Operation(summary = "创建Logstash进程", description = "创建一个新的Logstash进程")
    public ApiResponse<LogstashProcessDTO> createLogstashProcess(
            @Parameter(description = "Logstash进程创建信息", required = true) @Valid @RequestBody LogstashProcessCreateDTO dto) {
        return ApiResponse.success(logstashProcessService.createLogstashProcess(dto));
    }

    /**
     * 更新Logstash进程
     *
     * @param id  Logstash进程数据库ID
     * @param dto Logstash进程更新DTO
     * @return 更新后的Logstash进程
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新Logstash进程", description = "根据ID更新已有Logstash进程的配置信息")
    public ApiResponse<LogstashProcessDTO> updateLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Logstash进程更新信息", required = true) @Valid @RequestBody LogstashProcessCreateDTO dto) {
        return ApiResponse.success(logstashProcessService.updateLogstashProcess(id, dto));
    }

    /**
     * 删除Logstash进程
     *
     * @param id Logstash进程数据库ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Logstash进程", description = "根据ID删除Logstash进程")
    public ApiResponse<Void> deleteLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        logstashProcessService.deleteLogstashProcess(id);
        return ApiResponse.success();
    }

    /**
     * 获取Logstash进程详情
     *
     * @param id Logstash进程数据库ID
     * @return Logstash进程详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取Logstash进程详情", description = "根据ID获取Logstash进程的详细信息")
    public ApiResponse<LogstashProcessDTO> getLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.getLogstashProcess(id));
    }

    /**
     * 获取所有Logstash进程
     *
     * @return Logstash进程列表
     */
    @GetMapping
    @Operation(summary = "获取所有Logstash进程", description = "获取系统中所有的Logstash进程列表")
    public ApiResponse<List<LogstashProcessDTO>> getAllLogstashProcesses() {
        return ApiResponse.success(logstashProcessService.getAllLogstashProcesses());
    }

    /**
     * 启动Logstash进程
     *
     * @param id Logstash进程数据库ID
     * @return 启动后的Logstash进程
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "启动Logstash进程", description = "启动指定的Logstash进程")
    public ApiResponse<LogstashProcessDTO> startLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.startLogstashProcess(id));
    }

    /**
     * 停止Logstash进程
     *
     * @param id Logstash进程数据库ID
     * @return 停止后的Logstash进程
     */
    @PostMapping("/{id}/stop")
    @Operation(summary = "停止Logstash进程", description = "停止指定的Logstash进程")
    public ApiResponse<LogstashProcessDTO> stopLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.stopLogstashProcess(id));
    }

    @GetMapping("/{id}/task-status")
    @Operation(summary = "查询最近的一次Logstash任务执行状态", description = "根据ID查询Logstash任务的执行状态")
    public ApiResponse<TaskDetailDTO> getTaskStatus(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.getTaskDetailStatus(id));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重试失败的Logstash任务操作", description = "根据ID重试失败的Logstash任务")
    public ApiResponse<LogstashProcessDTO> retryTask(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.retryLogstashProcessOps(id));
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "查询Logstash进程关联的所有任务", description = "根据进程ID查询所有相关的任务摘要信息")
    public ApiResponse<List<TaskSummaryDTO>> getProcessTasks(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.getProcessTaskSummaries(id));
    }

    @GetMapping("/tasks/{taskId}/steps")
    @Operation(summary = "查询任务的步骤详情", description = "根据任务ID查询步骤执行详情，按步骤分组")
    public ApiResponse<TaskStepsGroupDTO> getTaskSteps(
            @Parameter(description = "任务ID", required = true) @PathVariable("taskId") String taskId) {
        return ApiResponse.success(logstashProcessService.getTaskStepsGrouped(taskId));
    }

}