package com.hinadt.miaocha.endpoint;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.domain.dto.ApiResponse;
import com.hinadt.miaocha.domain.dto.module.*;
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

    @Autowired private TableValidationService tableValidationService;

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

    @GetMapping("/query-config")
    @Operation(summary = "根据模块名获取模块查询配置", description = "根据模块名获取模块的查询配置，包括时间字段和关键词检索字段")
    public ApiResponse<QueryConfigDTO> getModuleQueryConfig(
            @Parameter(description = "模块ID", required = true) @RequestParam("name") String name) {
        QueryConfigDTO response = moduleInfoService.getQueryConfigByModule(name);
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
    @Operation(summary = "删除模块", description = "根据模块ID删除模块，可选择是否同时删除底层Doris表数据")
    public ApiResponse<Void> deleteModule(
            @Parameter(description = "模块ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否删除底层Doris表数据", required = false)
                    @RequestParam(defaultValue = "false")
                    Boolean deleteDorisTable) {
        moduleInfoService.deleteModule(id, deleteDorisTable);
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

    @PutMapping("/query-config")
    @Operation(summary = "配置模块查询配置", description = "配置模块的查询相关设置，包括时间字段和关键词检索字段。需要先完成建表操作。")
    public ApiResponse<ModuleInfoDTO> configureQueryConfig(
            @Valid @RequestBody ModuleQueryConfigDTO queryConfigDTO) {
        ModuleInfoDTO response =
                moduleInfoService.configureQueryConfig(
                        queryConfigDTO.getModuleId(), queryConfigDTO.getQueryConfig());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}/field-names")
    @Operation(
            summary = "获取模块表字段名列表",
            description = "获取指定模块的表字段名列表，用于查询配置时的字段提示。优先从建表SQL解析，如解析失败则从数据库元数据获取。")
    public ApiResponse<List<String>> getModuleFieldNames(
            @Parameter(description = "模块ID", required = true) @PathVariable Long id) {
        List<String> fieldNames = tableValidationService.getTableFieldNames(id);
        return ApiResponse.success(fieldNames);
    }
}
