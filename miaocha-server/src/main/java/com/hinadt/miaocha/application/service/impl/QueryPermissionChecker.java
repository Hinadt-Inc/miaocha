package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.ModulePermissionService;
import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.permission.UserModulePermissionDTO;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.entity.enums.UserRole;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 查询权限检查器 */
@Component
public class QueryPermissionChecker {

    private final ModulePermissionService modulePermissionService;
    private final TableValidationService tableValidationService;

    public QueryPermissionChecker(
            ModulePermissionService modulePermissionService,
            TableValidationService tableValidationService) {
        this.modulePermissionService = modulePermissionService;
        this.tableValidationService = tableValidationService;
    }

    /**
     * 检查用户是否有权限执行指定的查询
     *
     * @param user 用户信息
     * @param sql SQL查询语句
     */
    public void checkQueryPermission(User user, String sql) {
        // 超级管理员和管理员有所有权限
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole())
                || UserRole.ADMIN.name().equals(user.getRole())) {
            return;
        }

        // 普通用户只能执行SELECT查询
        checkSelectOnly(sql);

        // 使用 TableValidationService 提取所有表名并检查权限
        Set<String> tableNames = tableValidationService.extractTableNames(sql);
        for (String tableName : tableNames) {
            // 检查模块权限
            if (!modulePermissionService.hasModulePermission(user.getId(), tableName)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "没有访问模块的权限: " + tableName);
            }
        }
    }

    /**
     * 获取用户有权限访问的所有表
     *
     * @param userId 用户ID
     * @param conn 数据库连接
     * @return 有权限的表列表
     */
    public List<String> getPermittedTables(Long userId, Connection conn) throws SQLException {
        List<String> permittedTables = new ArrayList<>();

        // 获取用户可访问的所有模块
        List<String> permittedModules =
                modulePermissionService.getUserAccessibleModules(userId).stream()
                        .map(UserModulePermissionDTO::getModule)
                        .toList();

        // 获取所有表
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs =
                metaData.getTables(conn.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // 如果表名与模块名称匹配，或者用户是管理员，则添加到有权限的表列表中
                if (permittedModules.contains(tableName)) {
                    permittedTables.add(tableName);
                }
            }
        }

        return permittedTables;
    }

    /**
     * 检查是否是SELECT查询
     *
     * @param sql SQL查询语句
     */
    private void checkSelectOnly(String sql) {
        if (!isSelectQuery(sql)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "非管理员用户仅允许执行SELECT查询");
        }
    }

    /**
     * 判断是否是SELECT查询
     *
     * @param sql SQL查询语句
     * @return 是否是SELECT查询
     */
    private boolean isSelectQuery(String sql) {
        return tableValidationService.isSelectStatement(sql);
    }
}
