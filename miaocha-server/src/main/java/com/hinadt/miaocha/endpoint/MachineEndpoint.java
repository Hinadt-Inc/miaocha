package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.MachineService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.MachineConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.MachineCreateDTO;
import com.hinadt.miaocha.domain.dto.MachineDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 机器管理控制器 */
@RestController
@RequestMapping("/api/machines")
@Tag(name = "机器管理", description = "提供机器元信息的增删改查和连接测试等功能")
public class MachineEndpoint {

    private final MachineService machineService;

    public MachineEndpoint(MachineService machineService) {
        this.machineService = machineService;
    }

    /**
     * 创建机器
     *
     * @param dto 机器创建DTO
     * @return 创建的机器
     */
    @PostMapping
    @Operation(summary = "创建机器", description = "创建一个新的机器连接")
    public ApiResponse<MachineDTO> createMachine(
            @Parameter(description = "机器创建信息", required = true) @Valid @RequestBody
                    MachineCreateDTO dto) {
        return ApiResponse.success(machineService.createMachine(dto));
    }

    /**
     * 更新机器
     *
     * @param id 机器ID
     * @param dto 机器更新DTO
     * @return 更新后的机器
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新机器", description = "根据ID更新已有机器的配置信息")
    public ApiResponse<MachineDTO> updateMachine(
            @Parameter(description = "机器ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "机器更新信息", required = true) @Valid @RequestBody
                    MachineCreateDTO dto) {
        return ApiResponse.success(machineService.updateMachine(id, dto));
    }

    /**
     * 删除机器
     *
     * @param id 机器ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除机器", description = "根据ID删除机器")
    public ApiResponse<Void> deleteMachine(
            @Parameter(description = "机器ID", required = true) @PathVariable("id") Long id) {
        machineService.deleteMachine(id);
        return ApiResponse.success();
    }

    /**
     * 获取机器详情
     *
     * @param id 机器ID
     * @return 机器详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取机器详情", description = "根据ID获取机器的详细信息")
    public ApiResponse<MachineDTO> getMachine(
            @Parameter(description = "机器ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(machineService.getMachine(id));
    }

    /**
     * 获取所有机器
     *
     * @return 机器列表
     */
    @GetMapping
    @Operation(summary = "获取所有机器", description = "获取系统中所有的机器列表")
    public ApiResponse<List<MachineDTO>> getAllMachines() {
        return ApiResponse.success(machineService.getAllMachines());
    }

    /**
     * 测试现有机器连接
     *
     * @param id 机器ID
     * @return 连接测试结果
     */
    @PostMapping("/{id}/test-connection")
    @Operation(summary = "测试现有机器连接", description = "测试已保存机器的SSH连接是否可用")
    public ApiResponse<MachineConnectionTestResultDTO> testConnection(
            @Parameter(description = "机器ID", required = true) @PathVariable("id") Long id) {
        return ApiResponse.success(machineService.testConnection(id));
    }

    /**
     * 测试机器连接 (使用参数)
     *
     * @param dto 机器连接参数
     * @return 连接测试结果
     */
    @PostMapping("/test-connection")
    @Operation(summary = "测试机器连接", description = "使用机器连接参数测试SSH连接，无需保存机器")
    public ApiResponse<MachineConnectionTestResultDTO> testConnectionWithParams(
            @Parameter(description = "机器连接参数", required = true) @Valid @RequestBody
                    MachineCreateDTO dto) {
        return ApiResponse.success(machineService.testConnectionWithParams(dto));
    }
}
