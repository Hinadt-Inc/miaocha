package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.logstash.LogstashMachineDetailDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessMetadataUpdateDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Logstash进程管理控制器 - 重构支持多实例，基于logstashMachineId */
@RestController
@RequestMapping("/api/logstash/processes")
@Tag(name = "Logstash进程管理", description = "提供Logstash进程的创建、启停和配置更新等功能，支持多实例部署，需要管理员权限")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LogstashProcessEndpoint {

    private final LogstashProcessService logstashProcessService;
    private final TaskService taskService;

    /**
     * 创建Logstash进程 创建后会自动初始化对应的LogstashMachine实例
     *
     * @param dto Logstash进程创建DTO
     * @return 创建的Logstash进程响应
     */
    @PostMapping
    @Operation(
            summary = "创建Logstash进程",
            description = "创建一个新的Logstash进程，包含配置信息和机器列表。如果jvmOptions或logstashYml为空，将在初始化后异步同步这些配置。")
    public ApiResponse<LogstashProcessResponseDTO> createLogstashProcess(
            @Parameter(description = "Logstash进程创建信息", required = true) @Valid @RequestBody
                    LogstashProcessCreateDTO dto) {
        return ApiResponse.success(logstashProcessService.createLogstashProcess(dto));
    }

    /**
     * 获取Logstash进程详情
     *
     * @param id Logstash进程数据库ID
     * @return Logstash进程详情
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "获取Logstash进程详情",
            description = "根据ID获取Logstash进程的详细信息，包括各个LogstashMachine实例的状态")
    public ApiResponse<LogstashProcessResponseDTO> getLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id) {
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
     * 更新Logstash进程配置 支持同时更新主配置、JVM配置、系统配置中的任意组合
     *
     * @param id Logstash进程数据库ID
     * @param dto 配置更新请求DTO
     * @return 更新后的Logstash进程
     */
    @PutMapping("/{id}/config")
    @Operation(
            summary = "更新Logstash配置",
            description =
                    "更新Logstash进程的配置信息。可以同时更新任意组合的：主配置文件、JVM配置、Logstash系统配置。可以针对全部LogstashMachine实例或指定LogstashMachine实例。")
    public ApiResponse<LogstashProcessResponseDTO> updateLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id,
            @Parameter(description = "Logstash配置更新请求", required = true) @Valid @RequestBody
                    LogstashProcessConfigUpdateRequestDTO dto) {
        return ApiResponse.success(logstashProcessService.updateLogstashConfig(id, dto));
    }

    /**
     * 手动刷新Logstash配置 将数据库中的配置刷新到目标机器（不更改配置内容）
     *
     * @param id Logstash进程数据库ID
     * @param dto 配置刷新请求DTO，可指定要刷新的机器
     * @return 操作结果
     */
    @PostMapping("/{id}/config/refresh")
    @Operation(
            summary = "刷新Logstash配置",
            description =
                    "手动将数据库中的配置刷新到目标机器（不更改配置内容）。可以指定要刷新的LogstashMachine实例ID，若不指定则刷新所有LogstashMachine实例。")
    public ApiResponse<LogstashProcessResponseDTO> refreshLogstashConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id,
            @Parameter(description = "配置刷新请求", required = false) @RequestBody(required = false)
                    LogstashProcessConfigUpdateRequestDTO dto) {
        // 如果dto为null，创建一个空的dto，表示刷新所有LogstashMachine实例
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
    @Operation(summary = "删除Logstash进程", description = "根据ID删除Logstash进程及其相关联的所有LogstashMachine实例")
    public ApiResponse<Void> deleteLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id) {
        logstashProcessService.deleteLogstashProcess(id);
        return ApiResponse.success();
    }

    /**
     * 启动Logstash进程 - 全局操作（所有实例）
     *
     * @param id Logstash进程数据库ID
     * @return 启动后的Logstash进程
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "全局启动Logstash进程", description = "启动指定Logstash进程下所有关联的LogstashMachine实例")
    public ApiResponse<LogstashProcessResponseDTO> startLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id) {
        return ApiResponse.success(logstashProcessService.startLogstashProcess(id));
    }

    /**
     * 停止Logstash进程 - 全局操作（所有实例）
     *
     * @param id Logstash进程数据库ID
     * @return 停止后的Logstash进程
     */
    @PostMapping("/{id}/stop")
    @Operation(summary = "全局停止Logstash进程", description = "停止指定Logstash进程下所有关联的LogstashMachine实例")
    public ApiResponse<LogstashProcessResponseDTO> stopLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id) {
        return ApiResponse.success(logstashProcessService.stopLogstashProcess(id));
    }

    /**
     * 全局强制停止Logstash进程 应急停止功能：执行原有的停止逻辑，但无论命令成功与否，都强制将状态更改为未启动
     *
     * @param id Logstash进程数据库ID
     * @return 强制停止后的Logstash进程
     */
    @PostMapping("/{id}/force-stop")
    @Operation(
            summary = "全局强制停止Logstash进程",
            description = "强制停止指定Logstash进程关联的所有LogstashMachine实例，用于应急情况")
    public ApiResponse<LogstashProcessResponseDTO> forceStopLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id) {
        return ApiResponse.success(logstashProcessService.forceStopLogstashProcess(id));
    }

    /**
     * 获取LogstashMachine实例的所有任务
     *
     * @param instanceId LogstashMachine实例ID
     * @return 任务详情列表
     */
    @GetMapping("/instances/{instanceId}/tasks")
    @Operation(summary = "获取Logstash实例任务", description = "获取指定LogstashMachine实例的所有任务信息，包括步骤执行状态和进度")
    public ApiResponse<List<TaskDetailDTO>> getInstanceTasks(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);
        List<TaskDetailDTO> taskDetails =
                taskIds.stream()
                        .map(taskId -> taskService.getTaskDetail(taskId).orElse(null))
                        .filter(task -> task != null)
                        .collect(Collectors.toList());

        return ApiResponse.success(taskDetails);
    }

    /**
     * 获取LogstashMachine实例的最新任务
     *
     * @param instanceId LogstashMachine实例ID
     * @return 最新任务详情
     */
    @GetMapping("/instances/{instanceId}/tasks/latest")
    @Operation(summary = "获取Logstash实例最新任务", description = "获取指定LogstashMachine实例的最新任务信息")
    public ApiResponse<TaskDetailDTO> getLatestInstanceTask(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);
        if (taskIds.isEmpty()) {
            return ApiResponse.error(ErrorCode.TASK_NOT_FOUND, "找不到任务信息");
        }

        Optional<TaskDetailDTO> latestTask = taskService.getTaskDetail(taskIds.get(0));
        return latestTask
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(ErrorCode.TASK_NOT_FOUND, "找不到任务信息"));
    }

    /**
     * 获取进程任务信息
     *
     * @param id Logstash进程ID
     * @return 任务详情列表
     */
    @GetMapping("/{id}/tasks")
    @Operation(summary = "获取进程任务信息", description = "获取指定Logstash进程的所有任务信息，包括步骤执行状态和进度")
    public ApiResponse<List<TaskDetailDTO>> getProcessTasks(
            @Parameter(description = "Logstash进程ID", required = true) @PathVariable("id") Long id) {
        List<String> taskIds = taskService.getAllProcessTaskIds(id);
        List<TaskDetailDTO> taskDetails =
                taskIds.stream()
                        .map(taskId -> taskService.getTaskDetail(taskId).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        return ApiResponse.success(taskDetails);
    }

    /**
     * 更新Logstash进程完整元信息（包括基础信息和配置信息） 只更新数据库，不同步到实例，不执行部署操作
     *
     * @param id Logstash进程数据库ID
     * @param dto 完整元信息更新请求DTO
     * @return 更新后的Logstash进程
     */
    @PutMapping("/{id}/metadata-and-config")
    @Operation(
            summary = "更新Logstash进程完整元信息",
            description = "更新Logstash进程的完整元信息，包括进程名称、模块ID和所有配置信息（主配置、JVM配置、系统配置）。只更新数据库，不同步到实例。")
    public ApiResponse<LogstashProcessResponseDTO> updateLogstashProcessMetadataAndConfig(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id,
            @Parameter(description = "完整元信息更新请求", required = true) @Valid @RequestBody
                    LogstashProcessMetadataUpdateDTO dto) {
        return ApiResponse.success(
                logstashProcessService.updateLogstashProcessMetadataAndConfig(id, dto));
    }

    /**
     * Logstash进程扩容/缩容操作
     *
     * @param id Logstash进程数据库ID
     * @param dto 扩容/缩容请求DTO
     * @return 扩容/缩容后的Logstash进程
     */
    @PostMapping("/{id}/scale")
    @Operation(
            summary = "Logstash进程扩容/缩容",
            description = "对Logstash进程进行扩容或缩容操作。扩容：添加新机器并初始化；缩容：移除指定LogstashMachine实例并清理资源。")
    public ApiResponse<LogstashProcessResponseDTO> scaleLogstashProcess(
            @Parameter(description = "Logstash进程数据库ID", required = true) @PathVariable("id")
                    Long id,
            @Parameter(description = "扩容/缩容请求", required = true) @Valid @RequestBody
                    LogstashProcessScaleRequestDTO dto) {
        return ApiResponse.success(logstashProcessService.scaleLogstashProcess(id, dto));
    }

    // ==================== 单个LogstashMachine实例操作接口 ====================

    /**
     * 启动单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     * @return 操作结果
     */
    @PostMapping("/instances/{instanceId}/start")
    @Operation(summary = "启动单个LogstashMachine实例", description = "启动指定的LogstashMachine实例")
    public ApiResponse<Void> startLogstashInstance(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.startLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /**
     * 停止单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     * @return 操作结果
     */
    @PostMapping("/instances/{instanceId}/stop")
    @Operation(summary = "停止单个LogstashMachine实例", description = "停止指定的LogstashMachine实例")
    public ApiResponse<Void> stopLogstashInstance(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.stopLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /**
     * 强制停止单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     * @return 操作结果
     */
    @PostMapping("/instances/{instanceId}/force-stop")
    @Operation(summary = "强制停止单个LogstashMachine实例", description = "强制停止指定的LogstashMachine实例，用于应急情况")
    public ApiResponse<Void> forceStopLogstashInstance(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.forceStopLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /**
     * 重新初始化单个LogstashMachine实例
     *
     * @param instanceId LogstashMachine实例ID
     * @return 操作结果
     */
    @PostMapping("/instances/{instanceId}/reinitialize")
    @Operation(summary = "重新初始化LogstashMachine实例", description = "重新初始化指定的LogstashMachine实例环境")
    public ApiResponse<Void> reinitializeLogstashInstance(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.reinitializeLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /**
     * 获取LogstashMachine实例详细信息
     *
     * @param instanceId LogstashMachine实例ID
     * @return LogstashMachine实例详细信息
     */
    @GetMapping("/instances/{instanceId}")
    @Operation(
            summary = "获取LogstashMachine实例详情",
            description = "根据LogstashMachine实例ID获取详细的实例信息，包括关联的进程和机器信息")
    public ApiResponse<LogstashMachineDetailDTO> getLogstashMachineDetail(
            @Parameter(description = "LogstashMachine实例ID", required = true)
                    @PathVariable("instanceId")
                    Long instanceId) {
        return ApiResponse.success(logstashProcessService.getLogstashMachineDetail(instanceId));
    }
}
