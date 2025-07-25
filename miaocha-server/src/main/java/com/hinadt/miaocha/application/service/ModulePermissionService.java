package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.permission.ModuleUsersPermissionDTO;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import java.util.List;

/** 模块权限服务接口 用于处理基于模块的权限管理 */
public interface ModulePermissionService {

    /**
     * 检查用户对指定模块的访问权限
     *
     * @param userId 用户ID
     * @param module 模块名称
     * @return 是否有权限
     */
    boolean hasModulePermission(Long userId, String module);

    /**
     * 授予用户对指定模块的访问权限
     *
     * @param userId 用户ID
     * @param module 模块名称
     * @return 创建的权限DTO
     */
    UserModulePermissionDTO grantModulePermission(Long userId, String module);

    /**
     * 撤销用户对指定模块的访问权限
     *
     * @param userId 用户ID
     * @param module 模块名称
     */
    void revokeModulePermission(Long userId, String module);

    /**
     * 获取用户的所有模块权限
     *
     * @param userId 用户ID
     * @return 模块权限列表
     */
    List<UserModulePermissionDTO> getUserModulePermissions(Long userId);

    /**
     * 获取当前用户可访问的所有模块
     *
     * @param userId 用户ID
     * @return 用户可访问的模块权限列表（扁平化结构）
     */
    List<UserModulePermissionDTO> getUserAccessibleModules(Long userId);

    /**
     * 获取所有用户的模块权限（按模块聚合，每个模块下显示有权限的用户列表）
     *
     * @return 模块用户权限聚合列表
     */
    List<ModuleUsersPermissionDTO> getAllUsersModulePermissions();

    /**
     * 批量授予用户对多个模块的访问权限
     *
     * @param userId 用户ID
     * @param modules 模块名称列表
     * @return 创建的权限DTO列表
     */
    List<UserModulePermissionDTO> batchGrantModulePermissions(Long userId, List<String> modules);

    /**
     * 批量撤销用户对多个模块的访问权限
     *
     * @param userId 用户ID
     * @param modules 模块名称列表
     */
    void batchRevokeModulePermissions(Long userId, List<String> modules);

    /**
     * 获取用户没有权限的所有模块
     *
     * @param userId 用户ID
     * @return 用户没有权限的模块列表
     */
    List<String> getUserUnauthorizedModules(Long userId);
}
