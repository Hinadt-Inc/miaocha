package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.module.ModuleExecuteDorisSqlDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoCreateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoUpdateDTO;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoWithPermissionsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 日志模块管理Endpoint */
@RestController
@RequestMapping("/api/modules")
@Tag(name = "日志模块管理", description = "日志模块管理相关API")
public class ModuleEndpoint {

    @Autowired private ModuleInfoService moduleInfoService;

    @PostMapping
    @Operation(summary = "创建模块", description = "创建一个新的日志模块")
    public ApiResponse<ModuleInfoDTO> createModule(
            @Valid @RequestBody ModuleInfoCreateDTO createDTO) {
        ModuleInfoDTO response = moduleInfoService.createModule(createDTO);
        return ApiResponse.success(response);
    }

    @PutMapping
    @Operation(summary = "更新模块", description = "更新模块信息")
    public ApiResponse<ModuleInfoDTO> updateModule(
            @Valid @RequestBody ModuleInfoUpdateDTO updateDTO) {
        ModuleInfoDTO response = moduleInfoService.updateModule(updateDTO);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取模块", description = "根据模块ID获取模块详细信息")
    public ApiResponse<ModuleInfoDTO> getModuleById(
            @Parameter(description = "模块ID", required = true) @PathVariable Long id) {
        ModuleInfoDTO response = moduleInfoService.getModuleById(id);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "获取所有模块（包含权限信息）", description = "获取所有模块列表，包含每个模块对应的用户权限信息")
    public ApiResponse<List<ModuleInfoWithPermissionsDTO>> getAllModules() {
        List<ModuleInfoWithPermissionsDTO> response =
                moduleInfoService.getAllModulesWithPermissions();
        return ApiResponse.success(response);
    }

    @GetMapping("/basic")
    @Operation(summary = "获取所有模块基本信息", description = "获取所有模块基本信息列表，不包含权限信息")
    public ApiResponse<List<ModuleInfoDTO>> getAllModulesBasic() {
        List<ModuleInfoDTO> response = moduleInfoService.getAllModules();
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模块", description = "根据模块ID删除模块")
    public ApiResponse<Void> deleteModule(
            @Parameter(description = "模块ID", required = true) @PathVariable Long id) {
        moduleInfoService.deleteModule(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/execute-doris-sql")
    @Operation(summary = "执行模块的Doris SQL", description = "为指定模块执行Doris SQL创建表")
    public ApiResponse<ModuleInfoDTO> executeDorisSql(
            @Parameter(description = "模块ID", required = true) @PathVariable Long id,
            @Valid @RequestBody ModuleExecuteDorisSqlDTO sqlDTO) {
        ModuleInfoDTO response = moduleInfoService.executeDorisSql(id, sqlDTO.getSql());
        return ApiResponse.success(response);
    }
}
