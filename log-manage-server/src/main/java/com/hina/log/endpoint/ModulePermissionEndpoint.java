package com.hina.log.endpoint;

import com.hina.log.application.service.ModulePermissionService;
import com.hina.log.common.annotation.CurrentUser;
import com.hina.log.domain.dto.ApiResponse;
import com.hina.log.domain.dto.permission.ModulePermissionBatchRequestDTO;
import com.hina.log.domain.dto.permission.ModuleUsersPermissionDTO;
import com.hina.log.domain.dto.permission.UserModulePermissionDTO;
import com.hina.log.domain.dto.permission.UserPermissionModuleStructureDTO;
import com.hina.log.domain.dto.user.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 模块权限控制器 */
@RestController
@RequestMapping("/api/permissions/modules")
@Tag(name = "模块权限管理", description = "提供模块权限的授予和撤销功能")
@RequiredArgsConstructor
public class ModulePermissionEndpoint {

    private final ModulePermissionService modulePermissionService;

    /**
     * 授予用户对模块的权限
     *
     * @param userId 用户ID
     * @param module 模块名称
     * @return 创建的权限
     */
    @PostMapping("/user/{userId}/grant")
    @Operation(summary = "授予模块权限", description = "授予用户对指定模块的访问权限")
    public ApiResponse<UserModulePermissionDTO> grantModulePermission(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Parameter(description = "模块名称", required = true) @RequestParam("module")
                    String module) {
        return ApiResponse.success(modulePermissionService.grantModulePermission(userId, module));
    }

    /**
     * 批量授予用户对多个模块的权限
     *
     * @param userId 用户ID
     * @param request 模块权限批量请求DTO
     * @return 创建的权限列表
     */
    @PostMapping("/user/{userId}/batch-grant")
    @Operation(summary = "批量授予模块权限", description = "批量授予用户对多个模块的访问权限")
    public ApiResponse<List<UserModulePermissionDTO>> batchGrantModulePermissions(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Parameter(description = "模块权限批量请求", required = true) @Valid @RequestBody
                    ModulePermissionBatchRequestDTO request) {
        // 确保请求中的用户ID与路径中的用户ID一致
        request.setUserId(userId);
        return ApiResponse.success(
                modulePermissionService.batchGrantModulePermissions(userId, request.getModules()));
    }

    /**
     * 撤销用户对模块的权限
     *
     * @param userId 用户ID
     * @param module 模块名称
     * @return 结果
     */
    @DeleteMapping("/user/{userId}/revoke")
    @Operation(summary = "撤销模块权限", description = "撤销用户对指定模块的访问权限")
    public ApiResponse<Void> revokeModulePermission(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Parameter(description = "模块名称", required = true) @RequestParam("module")
                    String module) {
        modulePermissionService.revokeModulePermission(userId, module);
        return ApiResponse.success();
    }

    /**
     * 批量撤销用户对多个模块的权限
     *
     * @param userId 用户ID
     * @param request 模块权限批量请求DTO
     * @return 结果
     */
    @DeleteMapping("/user/{userId}/batch-revoke")
    @Operation(summary = "批量撤销模块权限", description = "批量撤销用户对多个模块的访问权限")
    public ApiResponse<Void> batchRevokeModulePermissions(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Parameter(description = "模块权限批量请求", required = true) @Valid @RequestBody
                    ModulePermissionBatchRequestDTO request) {
        // 确保请求中的用户ID与路径中的用户ID一致
        request.setUserId(userId);
        modulePermissionService.batchRevokeModulePermissions(userId, request.getModules());
        return ApiResponse.success();
    }

    /**
     * 检查用户是否有模块权限
     *
     * @param user 当前用户
     * @param module 模块名称
     * @return 是否有权限
     */
    @GetMapping("/check")
    @Operation(summary = "检查模块权限", description = "检查当前用户是否有指定模块的访问权限")
    public ApiResponse<Boolean> checkModulePermission(
            @CurrentUser UserDTO user,
            @Parameter(description = "模块名称", required = true) @RequestParam("module")
                    String module) {
        boolean hasPermission = modulePermissionService.hasModulePermission(user.getId(), module);
        return ApiResponse.success(hasPermission);
    }

    /**
     * 获取用户的所有模块权限
     *
     * @param userId 用户ID
     * @return 模块权限列表
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户模块权限", description = "获取用户的所有模块权限")
    public ApiResponse<List<UserModulePermissionDTO>> getUserModulePermissions(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
        return ApiResponse.success(modulePermissionService.getUserModulePermissions(userId));
    }

    /**
     * 获取当前用户可访问的所有模块
     *
     * @param user 当前用户
     * @return 用户可访问的模块结构列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的模块权限", description = "获取当前用户可访问的所有模块")
    public ApiResponse<List<UserPermissionModuleStructureDTO>> getMyAccessibleModules(
            @CurrentUser UserDTO user) {
        return ApiResponse.success(modulePermissionService.getUserAccessibleModules(user.getId()));
    }

    /**
     * 获取所有用户的模块权限
     *
     * @return 按模块聚合的用户权限列表
     */
    @GetMapping("/users/all")
    @Operation(summary = "获取所有用户的模块权限", description = "获取系统中所有模块的用户权限信息，按模块聚合显示，包含用户姓名等详细信息")
    public ApiResponse<List<ModuleUsersPermissionDTO>> getAllUsersModulePermissions() {
        return ApiResponse.success(modulePermissionService.getAllUsersModulePermissions());
    }

    /**
     * 获取用户没有权限的模块列表
     *
     * @param userId 用户ID
     * @return 用户没有权限的模块列表
     */
    @GetMapping("/user/{userId}/unauthorized")
    @Operation(summary = "获取用户未授权的模块", description = "获取用户没有权限的所有模块列表，方便前端授权")
    public ApiResponse<List<String>> getUserUnauthorizedModules(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
        return ApiResponse.success(modulePermissionService.getUserUnauthorizedModules(userId));
    }
}
