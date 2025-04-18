package com.hina.log.service.impl;

import com.hina.log.entity.User;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.enums.UserRole;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserDatasourcePermissionMapper;
import com.hina.log.service.UserDatasourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询权限检查器
 */
@Component
@RequiredArgsConstructor
public class QueryPermissionChecker {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?",
            Pattern.CASE_INSENSITIVE);

    private final UserDatasourcePermissionService permissionService;

    /**
     * 检查用户是否有权限执行指定的查询
     *
     * @param user         用户信息
     * @param datasourceId 数据源ID
     * @param sql          SQL查询语句
     */
    public void checkQueryPermission(User user, Long datasourceId, String sql) {
        // 超级管理员和管理员有所有权限
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole()) ||
                UserRole.ADMIN.name().equals(user.getRole())) {
            return;
        }

        // 普通用户只能执行SELECT查询
        checkSelectOnly(sql);

        // 检查表权限
        String tableName = extractTableName(sql);
        if (tableName != null && !permissionService.hasTablePermission(user.getId(), datasourceId, tableName)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "没有访问表的权限: " + tableName);
        }
    }

    /**
     * 获取用户有权限访问的所有表
     *
     * @param userId       用户ID
     * @param datasourceId 数据源ID
     * @param conn         数据库连接
     * @return 有权限的表列表
     */
    public List<String> getPermittedTables(Long userId, Long datasourceId, Connection conn) throws SQLException {
        List<String> permittedTables = new ArrayList<>();

        // 查询用户有权限的表
        List<UserDatasourcePermission> permissions = permissionService.getUserDatasourcePermissions(userId,
                datasourceId);

        // 如果有通配符权限，可以查看所有表
        boolean hasWildcardPermission = permissions.stream()
                .anyMatch(p -> "*".equals(p.getTableName()));

        if (hasWildcardPermission) {
            // 获取所有表
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[] { "TABLE" })) {
                while (rs.next()) {
                    permittedTables.add(rs.getString("TABLE_NAME"));
                }
            }
        } else {
            // 只返回有权限的表
            for (UserDatasourcePermission permission : permissions) {
                if (!permission.getTableName().equals("*")) {
                    permittedTables.add(permission.getTableName());
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
        return SELECT_PATTERN.matcher(sql.trim()).find();
    }

    /**
     * 从SQL查询中提取表名
     *
     * @param sql SQL查询语句
     * @return 表名
     */
    private String extractTableName(String sql) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}