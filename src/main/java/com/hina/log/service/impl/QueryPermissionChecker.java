package com.hina.log.service.impl;

import com.hina.log.entity.User;
import com.hina.log.entity.UserDatasourcePermission;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.UserDatasourcePermissionMapper;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class QueryPermissionChecker {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?",
            Pattern.CASE_INSENSITIVE);

    private final UserDatasourcePermissionMapper permissionMapper;

    public QueryPermissionChecker(UserDatasourcePermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public void checkQueryPermission(User user, Long datasourceId, String sql) {
        if (!user.getIsAdmin()) {
            checkSelectOnly(sql);
            String tableName = extractTableName(sql);
            checkTablePermission(user.getId(), datasourceId, tableName);
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
        List<UserDatasourcePermission> permissions = permissionMapper
                .selectByUserAndDatasource(userId, datasourceId);

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

    private void checkSelectOnly(String sql) {
        if (!isSelectQuery(sql)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "非管理员用户仅允许执行SELECT查询");
        }
    }

    private void checkTablePermission(Long userId, Long datasourceId, String tableName) {
        if (!hasTablePermission(userId, datasourceId, tableName)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "没有访问表的权限");
        }
    }

    private boolean hasTablePermission(Long userId, Long datasourceId, String tableName) {
        // 检查通配符权限
        UserDatasourcePermission wildcardPermission = permissionMapper.selectAllTablesPermission(userId, datasourceId);
        if (wildcardPermission != null) {
            return true;
        }

        // 检查特定表权限
        if (tableName != null) {
            UserDatasourcePermission tablePermission = permissionMapper
                    .selectByUserDatasourceAndTable(userId, datasourceId, tableName);
            return tablePermission != null;
        }

        return false;
    }

    private boolean isSelectQuery(String sql) {
        return SELECT_PATTERN.matcher(sql).find();
    }

    private String extractTableName(String sql) {
        java.util.regex.Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}