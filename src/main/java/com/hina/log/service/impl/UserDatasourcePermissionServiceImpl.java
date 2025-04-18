package com.hina.log.service.impl;

import com.hina.log.entity.User;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserDatasourcePermissionMapper;
import com.hina.log.mapper.UserMapper;
import com.hina.log.service.UserDatasourcePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户数据源权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDatasourcePermissionServiceImpl implements UserDatasourcePermissionService {

    private final UserDatasourcePermissionMapper permissionMapper;
    private final UserMapper userMapper;

    @Override
    public List<UserDatasourcePermission> getUserDatasourcePermissions(Long userId, Long datasourceId) {
        return permissionMapper.selectByUserAndDatasource(userId, datasourceId);
    }

    @Override
    public boolean hasTablePermission(Long userId, Long datasourceId, String tableName) {
        // 先查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 超级管理员和管理员拥有所有表的权限
        String role = user.getRole();
        if (UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role)) {
            return true;
        }

        // 检查用户是否拥有此表的权限
        UserDatasourcePermission permission = permissionMapper.selectByUserDatasourceAndTable(
                userId, datasourceId, tableName);
        if (permission != null) {
            return true;
        }

        // 检查用户是否拥有此数据源下所有表的权限
        UserDatasourcePermission wildcard = permissionMapper.selectAllTablesPermission(userId, datasourceId);
        return wildcard != null;
    }

    @Override
    @Transactional
    public UserDatasourcePermission grantTablePermission(Long userId, Long datasourceId, String tableName) {
        // 检查是否已存在相同权限
        UserDatasourcePermission existPermission = permissionMapper.selectByUserDatasourceAndTable(
                userId, datasourceId, tableName);

        if (existPermission != null) {
            return existPermission;
        }

        // 创建新权限
        UserDatasourcePermission permission = new UserDatasourcePermission();
        permission.setUserId(userId);
        permission.setDatasourceId(datasourceId);
        permission.setTableName(tableName);

        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public void revokeTablePermission(Long permissionId) {
        permissionMapper.deleteById(permissionId);
    }

    @Override
    @Transactional
    public void revokeTablePermission(Long userId, Long datasourceId, String tableName) {
        UserDatasourcePermission permission = permissionMapper.selectByUserDatasourceAndTable(
                userId, datasourceId, tableName);

        if (permission != null) {
            permissionMapper.deleteById(permission.getId());
        }
    }
}