package com.hina.log.controller;

import com.hina.log.dto.ApiResponse;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.service.UserDatasourcePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户数据源权限控制器
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "用户数据源权限管理", description = "提供用户数据源权限的分配与管理功能")
public class UserDatasourcePermissionController {

    private final UserDatasourcePermissionService permissionService;

    /**
     * 获取用户在数据源的所有权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @return 权限列表
     */
    @GetMapping("/user/{userId}/datasource/{datasourceId}")
    @Operation(summary = "获取用户数据源权限", description = "获取指定用户在指定数据源上的所有表权限")
    public ApiResponse<List<UserDatasourcePermission>> getUserDatasourcePermissions(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "数据源ID", required = true) @PathVariable Long datasourceId) {
        return ApiResponse.success(permissionService.getUserDatasourcePermissions(userId, datasourceId));
    }

    /**
     * 授予用户对数据源表的权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 创建的权限
     */
    @PostMapping("/user/{userId}/datasource/{datasourceId}/table/{tableName}")
    @Operation(summary = "授予表权限", description = "授予用户对指定数据源表的访问权限")
    public ApiResponse<UserDatasourcePermission> grantTablePermission(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "数据源ID", required = true) @PathVariable Long datasourceId,
            @Parameter(description = "表名", required = true) @PathVariable String tableName) {
        return ApiResponse.success(permissionService.grantTablePermission(userId, datasourceId, tableName));
    }

    /**
     * 撤销用户对数据源表的权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 结果
     */
    @DeleteMapping("/user/{userId}/datasource/{datasourceId}/table/{tableName}")
    @Operation(summary = "撤销表权限", description = "撤销用户对指定数据源表的访问权限")
    public ApiResponse<Void> revokeTablePermission(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "数据源ID", required = true) @PathVariable Long datasourceId,
            @Parameter(description = "表名", required = true) @PathVariable String tableName) {
        permissionService.revokeTablePermission(userId, datasourceId, tableName);
        return ApiResponse.success();
    }

    /**
     * 撤销权限（通过权限ID）
     *
     * @param permissionId 权限ID
     * @return 结果
     */
    @DeleteMapping("/{permissionId}")
    @Operation(summary = "通过ID撤销权限", description = "通过权限ID撤销用户的数据源表访问权限")
    public ApiResponse<Void> revokePermissionById(
            @Parameter(description = "权限ID", required = true) @PathVariable Long permissionId) {
        permissionService.revokeTablePermission(permissionId);
        return ApiResponse.success();
    }
}