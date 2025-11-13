package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.logstash.task.TaskService;
import com.hinadt.miaocha.application.service.LogstashAlertRecipientsService;
import com.hinadt.miaocha.application.service.LogstashProcessService;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.logstash.AlertRecipientsUpdateRequestDTO;
import com.hinadt.miaocha.domain.dto.logstash.LogstashInstanceBatchOperationRequestDTO;
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

/** Logstash process management controller - supports multiple instances per process. */
@RestController
@RequestMapping("/api/logstash/processes")
@Tag(
        name = "Logstash Process Management",
        description =
                "Provide create/start/stop/config update for Logstash processes with multi-instance"
                        + " support. Admin required.")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LogstashProcessEndpoint {

    private final LogstashProcessService logstashProcessService;
    private final LogstashAlertRecipientsService alertRecipientsService;
    private final TaskService taskService;

    /** Create a Logstash process and initialize LogstashMachine instances. */
    @PostMapping
    @Operation(
            summary = "Create Logstash process",
            description =
                    "Create a Logstash process with configs and machines. If jvmOptions or"
                            + " logstashYml is empty, they will be synced asynchronously after"
                            + " initialization.")
    public ApiResponse<LogstashProcessResponseDTO> createLogstashProcess(
            @Parameter(description = "Create DTO", required = true) @Valid @RequestBody
                    LogstashProcessCreateDTO dto) {
        return ApiResponse.success(logstashProcessService.createLogstashProcess(dto));
    }

    /** Get Logstash process detail. */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get Logstash process detail",
            description =
                    "Get process by ID including statuses of related LogstashMachine instances.")
    public ApiResponse<LogstashProcessResponseDTO> getLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.getLogstashProcess(id));
    }

    /** List all Logstash processes. */
    @GetMapping
    @Operation(summary = "List all processes", description = "List all Logstash processes")
    public ApiResponse<List<LogstashProcessResponseDTO>> getAllLogstashProcesses() {
        return ApiResponse.success(logstashProcessService.getAllLogstashProcesses());
    }

    /** Update configs for a process (main/JVM/YML). */
    @PutMapping("/{id}/config")
    @Operation(
            summary = "Update Logstash config",
            description =
                    "Update main config, JVM options, and/or logstash.yml for all or specified"
                            + " instances.")
    public ApiResponse<LogstashProcessResponseDTO> updateLogstashConfig(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Config update request", required = true) @Valid @RequestBody
                    LogstashProcessConfigUpdateRequestDTO dto) {
        return ApiResponse.success(logstashProcessService.updateLogstashConfig(id, dto));
    }

    /** Refresh stored configs to machines without changing contents. */
    @PostMapping("/{id}/config/refresh")
    @Operation(
            summary = "Refresh Logstash config",
            description =
                    "Sync stored configs to target machines. If instance IDs omitted, refresh all"
                            + " instances.")
    public ApiResponse<LogstashProcessResponseDTO> refreshLogstashConfig(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Refresh request", required = false)
                    @RequestBody(required = false)
                    LogstashProcessConfigUpdateRequestDTO dto) {
        if (dto == null) {
            dto = new LogstashProcessConfigUpdateRequestDTO();
        }
        return ApiResponse.success(logstashProcessService.refreshLogstashConfig(id, dto));
    }

    /** Delete a process and its related instances. */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete Logstash process",
            description = "Delete a process by ID and all related LogstashMachine instances.")
    public ApiResponse<Void> deleteLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        logstashProcessService.deleteLogstashProcess(id);
        return ApiResponse.success();
    }

    /** Start all instances under a process. */
    @PostMapping("/{id}/start")
    @Operation(summary = "Start process", description = "Start all instances under the process")
    public ApiResponse<LogstashProcessResponseDTO> startLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.startLogstashProcess(id));
    }

    /** Stop all instances under a process. */
    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop process", description = "Stop all instances under the process")
    public ApiResponse<LogstashProcessResponseDTO> stopLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.stopLogstashProcess(id));
    }

    /** Force stop all instances under a process. */
    @PostMapping("/{id}/force-stop")
    @Operation(
            summary = "Force stop process",
            description = "Force stop all instances for emergency handling")
    public ApiResponse<LogstashProcessResponseDTO> forceStopLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(logstashProcessService.forceStopLogstashProcess(id));
    }

    /** Update alert recipients (emails) for the process. */
    @PutMapping("/{id}/alert-recipients")
    @Operation(
            summary = "Update alert recipients",
            description =
                    "Configure alert recipients at Logstash process level. Accepts a list of"
                            + " emails; stored as JSON string.")
    public ApiResponse<LogstashProcessResponseDTO> updateAlertRecipients(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Recipients update request", required = true)
                    @Valid
                    @RequestBody
                    AlertRecipientsUpdateRequestDTO dto) {
        alertRecipientsService.updateAlertRecipients(id, dto.getAlertRecipients());
        return ApiResponse.success(logstashProcessService.getLogstashProcess(id));
    }

    // ==================== Instance tasks endpoints ====================

    /** Get all tasks of a LogstashMachine instance. */
    @GetMapping("/instances/{instanceId}/tasks")
    @Operation(
            summary = "Get instance tasks",
            description =
                    "Get all tasks of a LogstashMachine instance including step statuses and"
                            + " progress")
    public ApiResponse<List<TaskDetailDTO>> getInstanceTasks(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);
        List<TaskDetailDTO> taskDetails =
                taskIds.stream()
                        .map(taskId -> taskService.getTaskDetail(taskId).orElse(null))
                        .filter(task -> task != null)
                        .collect(Collectors.toList());

        return ApiResponse.success(taskDetails);
    }

    /** Get the latest task of a LogstashMachine instance. */
    @GetMapping("/instances/{instanceId}/tasks/latest")
    @Operation(
            summary = "Get latest instance task",
            description = "Get the latest task of the instance")
    public ApiResponse<TaskDetailDTO> getLatestInstanceTask(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        List<String> taskIds = taskService.getAllInstanceTaskIds(instanceId);
        if (taskIds.isEmpty()) {
            return ApiResponse.error(ErrorCode.TASK_NOT_FOUND, "No task found");
        }

        Optional<TaskDetailDTO> latestTask = taskService.getTaskDetail(taskIds.get(0));
        return latestTask
                .map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(ErrorCode.TASK_NOT_FOUND, "No task found"));
    }

    /** Get process tasks by process ID. */
    @GetMapping("/{id}/tasks")
    @Operation(
            summary = "Get process tasks",
            description = "Get all tasks of the process including step statuses and progress")
    public ApiResponse<List<TaskDetailDTO>> getProcessTasks(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id) {
        List<String> taskIds = taskService.getAllProcessTaskIds(id);
        List<TaskDetailDTO> taskDetails =
                taskIds.stream()
                        .map(taskId -> taskService.getTaskDetail(taskId).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        return ApiResponse.success(taskDetails);
    }

    /** Update process metadata and configs (DB only, not syncing to instances). */
    @PutMapping("/{id}/metadata-and-config")
    @Operation(
            summary = "Update metadata and config",
            description =
                    "Update process name, module, and configs (main/JVM/YML) in DB only without"
                            + " syncing to instances.")
    public ApiResponse<LogstashProcessResponseDTO> updateLogstashProcessMetadataAndConfig(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Update DTO", required = true) @Valid @RequestBody
                    LogstashProcessMetadataUpdateDTO dto) {
        return ApiResponse.success(
                logstashProcessService.updateLogstashProcessMetadataAndConfig(id, dto));
    }

    /** Scale a process: add machines (expand) or remove instances (shrink). */
    @PostMapping("/{id}/scale")
    @Operation(
            summary = "Scale process",
            description =
                    "Expand: add machines and initialize; Shrink: remove specified instances and"
                            + " cleanup.")
    public ApiResponse<LogstashProcessResponseDTO> scaleLogstashProcess(
            @Parameter(description = "Process ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Scale request", required = true) @Valid @RequestBody
                    LogstashProcessScaleRequestDTO dto) {
        return ApiResponse.success(logstashProcessService.scaleLogstashProcess(id, dto));
    }

    // ==================== Single instance operations ====================

    /** Start a single instance. */
    @PostMapping("/instances/{instanceId}/start")
    @Operation(
            summary = "Start instance",
            description = "Start a specific LogstashMachine instance")
    public ApiResponse<Void> startLogstashInstance(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.startLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /** Stop a single instance. */
    @PostMapping("/instances/{instanceId}/stop")
    @Operation(summary = "Stop instance", description = "Stop a specific LogstashMachine instance")
    public ApiResponse<Void> stopLogstashInstance(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.stopLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /** Force stop a single instance. */
    @PostMapping("/instances/{instanceId}/force-stop")
    @Operation(
            summary = "Force stop instance",
            description = "Force stop a specific LogstashMachine instance for emergency")
    public ApiResponse<Void> forceStopLogstashInstance(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.forceStopLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /** Reinitialize a single instance. */
    @PostMapping("/instances/{instanceId}/reinitialize")
    @Operation(
            summary = "Reinitialize instance",
            description = "Reinitialize the environment for a specific instance")
    public ApiResponse<Void> reinitializeLogstashInstance(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        logstashProcessService.reinitializeLogstashInstance(instanceId);
        return ApiResponse.success();
    }

    /** Get a single instance detail. */
    @GetMapping("/instances/{instanceId}")
    @Operation(
            summary = "Get instance detail",
            description =
                    "Get detailed info of a LogstashMachine instance including related process and"
                            + " machine info")
    public ApiResponse<LogstashMachineDetailDTO> getLogstashMachineDetail(
            @Parameter(description = "Instance ID", required = true) @PathVariable("instanceId")
                    Long instanceId) {
        return ApiResponse.success(logstashProcessService.getLogstashMachineDetail(instanceId));
    }

    // ==================== Batch instance operations ====================

    /** Batch start multiple instances. */
    @PostMapping("/instances/batch-start")
    @Operation(
            summary = "Batch start instances",
            description = "Start multiple LogstashMachine instances in batch")
    public ApiResponse<Void> batchStartLogstashInstances(
            @Parameter(description = "Batch operation request", required = true) @Valid @RequestBody
                    LogstashInstanceBatchOperationRequestDTO dto) {
        logstashProcessService.batchStartLogstashInstances(dto.getInstanceIds());
        return ApiResponse.success();
    }

    /** Batch stop multiple instances. */
    @PostMapping("/instances/batch-stop")
    @Operation(
            summary = "Batch stop instances",
            description = "Stop multiple LogstashMachine instances in batch")
    public ApiResponse<Void> batchStopLogstashInstances(
            @Parameter(description = "Batch operation request", required = true) @Valid @RequestBody
                    LogstashInstanceBatchOperationRequestDTO dto) {
        logstashProcessService.batchStopLogstashInstances(dto.getInstanceIds());
        return ApiResponse.success();
    }
}
