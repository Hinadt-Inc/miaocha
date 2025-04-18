package com.hina.log.service;

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
     * @return 权限列表
     */
    List<UserDatasourcePermission> getUserDatasourcePermissions(Long userId, Long datasourceId);

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
     * @return 创建的权限
     */
    UserDatasourcePermission grantTablePermission(Long userId, Long datasourceId, String tableName);

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
}