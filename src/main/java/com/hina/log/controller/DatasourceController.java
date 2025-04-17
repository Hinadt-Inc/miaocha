package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.DatasourceCreateDTO;
import com.hina.log.dto.DatasourceDTO;
import com.hina.log.service.DatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理控制器
 */
@RestController
@RequestMapping("/api/datasources")
@Tag(name = "数据源管理", description = "提供数据源的增删改查和连接测试等功能")
public class DatasourceController {

    @Autowired
    private DatasourceService datasourceService;

    @PostMapping
    @Operation(summary = "创建数据源", description = "创建一个新的数据源连接")
    public ApiResponse<DatasourceDTO> createDatasource(
            @Parameter(description = "数据源创建信息", required = true) @Valid @RequestBody DatasourceCreateDTO dto) {
        return ApiResponse.success(datasourceService.createDatasource(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数据源", description = "根据ID更新已有数据源的配置信息")
    public ApiResponse<DatasourceDTO> updateDatasource(
            @Parameter(description = "数据源ID", required = true) @PathVariable Long id,
            @Parameter(description = "数据源更新信息", required = true) @Valid @RequestBody DatasourceCreateDTO dto) {
        return ApiResponse.success(datasourceService.updateDatasource(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据源", description = "根据ID删除数据源")
    public ApiResponse<Void> deleteDatasource(
            @Parameter(description = "数据源ID", required = true) @PathVariable Long id) {
        datasourceService.deleteDatasource(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取数据源详情", description = "根据ID获取数据源的详细信息")
    public ApiResponse<DatasourceDTO> getDatasource(
            @Parameter(description = "数据源ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(datasourceService.getDatasource(id));
    }

    @GetMapping
    @Operation(summary = "获取所有数据源", description = "获取系统中所有的数据源列表")
    public ApiResponse<List<DatasourceDTO>> getAllDatasources() {
        return ApiResponse.success(datasourceService.getAllDatasources());
    }

    @PostMapping("/test-connection")
    @Operation(summary = "测试数据源连接", description = "测试新的数据源连接是否可用，但不保存")
    public ApiResponse<Boolean> testConnection(
            @Parameter(description = "数据源连接信息", required = true) @Valid @RequestBody DatasourceCreateDTO dto) {
        return ApiResponse.success(datasourceService.testConnection(dto));
    }

    @PostMapping("/{id}/test-connection")
    @Operation(summary = "测试现有的数据源连接", description = "测试已保存的数据源连接是否可用")
    public ApiResponse<Boolean> testConnection(
            @Parameter(description = "数据源ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(datasourceService.testExistingConnection(id));
    }
}