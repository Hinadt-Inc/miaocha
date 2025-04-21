package com.hina.log.service;

import com.hina.log.dto.permission.UserDatasourcePermissionDTO;
import com.hina.log.dto.permission.UserPermissionTableStructureDTO;
import com.hina.log.entity.UserDatasourcePermission;

import java.util.List;

/**
 * 用户数据源权限服务接口
 */
public interface UserDatasourcePermissionService {

    /**
     * 获取用户在指定数据源的所有权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @return 权限DTO列表
     */
    List<UserDatasourcePermissionDTO> getUserDatasourcePermissions(Long userId, Long datasourceId);

    /**
     * 检查用户对指定数据源表的访问权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 是否有权限
     */
    boolean hasTablePermission(Long userId, Long datasourceId, String tableName);

    /**
     * 授予用户对指定数据源表的访问权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 创建的权限DTO
     */
    UserDatasourcePermissionDTO grantTablePermission(Long userId, Long datasourceId, String tableName);

    /**
     * 撤销用户对指定数据源表的访问权限
     *
     * @param permissionId 权限ID
     */
    void revokeTablePermission(Long permissionId);

    /**
     * 撤销用户对指定数据源表的访问权限
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     */
    void revokeTablePermission(Long userId, Long datasourceId, String tableName);

    /**
     * 获取权限实体（内部使用）
     *
     * @param permissionId 权限ID
     * @return 权限实体
     */
    UserDatasourcePermission getPermissionEntityById(Long permissionId);

    /**
     * 获取权限实体（内部使用）
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 权限实体
     */
    UserDatasourcePermission getPermissionEntity(Long userId, Long datasourceId, String tableName);

    /**
     * 获取当前用户可访问的所有数据源及表信息
     *
     * @param userId 用户ID
     * @return 用户可访问的数据源及表信息列表
     */
    List<UserPermissionTableStructureDTO> getUserAccessibleTables(Long userId);
}