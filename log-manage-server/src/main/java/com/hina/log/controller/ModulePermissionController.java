package com.hina.log.controller;

import com.hina.log.annotation.CurrentUser;
import com.hina.log.dto.ApiResponse;
import com.hina.log.dto.permission.UserModulePermissionDTO;
import com.hina.log.dto.permission.UserPermissionModuleStructureDTO;
import com.hina.log.dto.user.UserDTO;
import com.hina.log.service.ModulePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块权限控制器
 */
@RestController
@RequestMapping("/api/permissions/modules")
@Tag(name = "模块权限管理", description = "提供模块权限的授予和撤销功能")
@RequiredArgsConstructor
public class ModulePermissionController {

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
            @Parameter(description = "模块名称", required = true) @RequestParam("module") String module) {
        return ApiResponse.success(modulePermissionService.grantModulePermission(userId, module));
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
            @Parameter(description = "模块名称", required = true) @RequestParam("module") String module) {
        modulePermissionService.revokeModulePermission(userId, module);
        return ApiResponse.success();
    }

    /**
     * 检查用户是否有模块权限
     *
     * @param user   当前用户
     * @param module 模块名称
     * @return 是否有权限
     */
    @GetMapping("/check")
    @Operation(summary = "检查模块权限", description = "检查当前用户是否有指定模块的访问权限")
    public ApiResponse<Boolean> checkModulePermission(
            @CurrentUser UserDTO user,
            @Parameter(description = "模块名称", required = true) @RequestParam("module") String module) {
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
    public ApiResponse<List<UserPermissionModuleStructureDTO>> getMyAccessibleModules(@CurrentUser UserDTO user) {
        return ApiResponse.success(modulePermissionService.getUserAccessibleModules(user.getId()));
    }
}
