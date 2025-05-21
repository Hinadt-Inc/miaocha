package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hina.log.dto.logstash.LogstashProcessCreateDTO;
import com.hina.log.dto.logstash.LogstashProcessResponseDTO;
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
@Tag(name = "Logstash进程管理", description = "提供Logstash进程的创建、启停和配置更新等功能，需要管理员权限")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LogstashProcessController {

    private final LogstashProcessService logstashProcessService;

    /**
     * 创建Logstash进程
     * 创建后会自动初始化对应的LogstashMachine实例
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程响应
     */
    @PostMapping
    @Operation(summary = "创建Logstash进程", description = "创建一个新的Logstash进程，包含配置信息和机器列表。如果jvmOptions或logstashYml为空，将在初始化后异步同步这些配置。")
    public ApiResponse<LogstashProcessResponseDTO> createLogstashProcess(
            @Parameter(description = "Logstash进程创建信息", required = true) @Valid @RequestBody LogstashProcessCreateDTO dto) {
        return ApiResponse.success(logstashProcessService.createLogstashProcess(dto));
    }

    /**
     * 获取Logstash进程详情
     *
     * @param id Logstash进程数据库ID
     * @return Logstash进程详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取Logstash进程详情", description = "根据ID获取Logstash进程的详细信息，包括各个机器上的状态")
    public ApiResponse<LogstashProcessResponseDTO> getLogstashProcess(
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
    public ApiResponse<List<LogstashProcessResponseDTO>> getAllLogstashProcesses() {
        return ApiResponse.success(logstashProcessService.getAllLogstashProcesses());
    }

    /**
     * 更新Logstash进程配置
     * 支持同时更新主配置、JVM配置、系统配置中的任意组合
     * 
     * @param id Logstash进程数据库ID
     * @param dto 配置更新请求DTO
     * @return 更新后的Logstash进程
     */
    @PutMapping("/{id}/config")
    @Operation(summary = "更新Logstash配置", description = "更新Logstash进程的配置信息。可以同时更新任意组合的：主配置文件、JVM配置、Logstash系统配置。可以针对全部机器或指定机器。")
    public ApiResponse<LogstashProcessResponseDTO> updateLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Logstash配置更新请求", required = true) @Valid @RequestBody LogstashProcessConfigUpdateRequestDTO dto) {
        return ApiResponse.success(logstashProcessService.updateLogstashConfig(id, dto));
    }

    /**
     * 手动刷新Logstash配置
     * 将数据库中的配置刷新到目标机器（不更改配置内容）
     *
     * @param id Logstash进程数据库ID
     * @param dto 配置刷新请求DTO，可指定要刷新的机器
     * @return 操作结果
     */
    @PostMapping("/{id}/config/refresh")
    @Operation(summary = "刷新Logstash配置", description = "手动将数据库中的配置刷新到目标机器（不更改配置内容）。可以指定要刷新的机器ID，若不指定则刷新所有机器。")
    public ApiResponse<LogstashProcessResponseDTO> refreshLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "配置刷新请求", required = false) @RequestBody(required = false) LogstashProcessConfigUpdateRequestDTO dto) {
        // 如果dto为null，创建一个空的dto，表示刷新所有机器
        if (dto == null) {
            dto = new LogstashProcessConfigUpdateRequestDTO();
        }
        return ApiResponse.success(logstashProcessService.refreshLogstashConfig(id, dto));
    }

    /**
     * 删除Logstash进程
     *
     * @param id Logstash进程数据库ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Logstash进程", description = "根据ID删除Logstash进程及其相关联的所有LogstashMachine")
    public ApiResponse<Void> deleteLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        logstashProcessService.deleteLogstashProcess(id);
        return ApiResponse.success();
    }

    /**
     * 启动Logstash进程 - 全局操作（所有机器）
     *
     * @param id Logstash进程数据库ID
     * @return 启动后的Logstash进程
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "全局启动Logstash进程", description = "启动指定Logstash进程在所有关联机器上的实例")
    public ApiResponse<LogstashProcessResponseDTO> startLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.startLogstashProcess(id));
    }

    /**
     * 停止Logstash进程 - 全局操作（所有机器）
     *
     * @param id Logstash进程数据库ID
     * @return 停止后的Logstash进程
     */
    @PostMapping("/{id}/stop")
    @Operation(summary = "全局停止Logstash进程", description = "停止指定Logstash进程在所有关联机器上的实例")
    public ApiResponse<LogstashProcessResponseDTO> stopLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.stopLogstashProcess(id));
    }

    /**
     * 启动单台机器上的Logstash进程
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 启动后的Logstash进程信息
     */
    @PostMapping("/{id}/machines/{machineId}/start")
    @Operation(summary = "启动单台机器上的Logstash进程", description = "启动指定Logstash进程在特定机器上的实例")
    public ApiResponse<LogstashProcessResponseDTO> startMachineProcess(
            @Parameter(description = "Logstash进程ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "机器ID", required = true) @PathVariable("machineId") Long machineId) {
        return ApiResponse.success(logstashProcessService.startMachineProcess(id, machineId));
    }

    /**
     * 停止单台机器上的Logstash进程
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 停止后的Logstash进程信息
     */
    @PostMapping("/{id}/machines/{machineId}/stop")
    @Operation(summary = "停止单台机器上的Logstash进程", description = "停止指定Logstash进程在特定机器上的实例")
    public ApiResponse<LogstashProcessResponseDTO> stopMachineProcess(
            @Parameter(description = "Logstash进程ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "机器ID", required = true) @PathVariable("machineId") Long machineId) {
        return ApiResponse.success(logstashProcessService.stopMachineProcess(id, machineId));
    }
}