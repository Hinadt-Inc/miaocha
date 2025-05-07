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
     * 更新Logstash进程配置
     *
     * @param id        Logstash进程数据库ID
     * @param configDTO 包含configJson的DTO
     * @return 更新后的Logstash进程
     */
    @PutMapping("/{id}/config")
    @Operation(summary = "更新Logstash配置", description = "更新Logstash进程的配置信息并自动刷新到目标机器")
    public ApiResponse<LogstashProcessDTO> updateLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Logstash配置信息", required = true) @Valid @RequestBody LogstashConfigUpdateDTO configDTO) {
        // 如果手动指定了表名，则使用手动指定的表名，否则会自动从配置中提取
        return ApiResponse.success(logstashProcessService.updateLogstashConfig(id, configDTO.getConfigContent(), configDTO.getTableName()));
    }

    /**
     * 手动刷新Logstash配置
     *
     * @param id Logstash进程数据库ID
     * @return 操作结果
     */
    @PostMapping("/{id}/config/refresh")
    @Operation(summary = "刷新Logstash配置", description = "手动将数据库中的配置刷新到目标机器（不更改配置内容）")
    public ApiResponse<LogstashProcessDTO> refreshLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.refreshLogstashConfig(id));
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

    /**
     * 执行Doris SQL语句
     *
     * @param id  Logstash进程ID
     * @param dto 包含SQL语句的DTO
     * @return 更新后的Logstash进程
     */
    @PostMapping("/{id}/doris-sql")
    @Operation(summary = "执行Doris SQL语句", description = "执行Doris建表SQL并保存到进程中，仅能在未启动状态执行且只能执行一次")
    public ApiResponse<LogstashProcessDTO> executeDorisSql(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Doris SQL执行请求", required = true) @Valid @RequestBody DorisSqlExecuteDTO dto) {
        return ApiResponse.success(logstashProcessService.executeDorisSql(id, dto.getSql()));
    }

}