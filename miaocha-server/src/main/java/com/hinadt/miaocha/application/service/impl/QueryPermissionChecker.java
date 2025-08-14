package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.TableValidationService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.enums.UserRole;
import com.hinadt.miaocha.infrastructure.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserModulePermissionMapper;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 查询权限检查器 */
@Component
public class QueryPermissionChecker {

    private final TableValidationService tableValidationService;
    private final UserModulePermissionMapper userModulePermissionMapper;
    private final UserMapper userMapper;
    private final ModuleInfoMapper moduleInfoMapper;

    public QueryPermissionChecker(
            TableValidationService tableValidationService,
            UserModulePermissionMapper userModulePermissionMapper,
            UserMapper userMapper,
            ModuleInfoMapper moduleInfoMapper) {
        this.tableValidationService = tableValidationService;
        this.userModulePermissionMapper = userModulePermissionMapper;
        this.userMapper = userMapper;
        this.moduleInfoMapper = moduleInfoMapper;
    }

    /**
     * 检查用户是否有权限执行指定的查询
     *
     * @param user 用户信息
     * @param sql SQL查询语句
     * @param datasourceId 数据源ID
     */
    public void checkQueryPermission(User user, String sql, Long datasourceId) {
        // 超级管理员和管理员有所有权限
        if (UserRole.SUPER_ADMIN.name().equals(user.getRole())
                || UserRole.ADMIN.name().equals(user.getRole())) {
            return;
        }

        // 普通用户只能执行SELECT查询
        checkSelectOnly(sql);

        // 提取SQL中涉及的所有表名
        Set<String> sqlTableNames = tableValidationService.extractTableNames(sql);
        if (sqlTableNames.isEmpty()) {
            return; // 如果没有提取到表名，直接通过
        }

        // 获取用户有权限访问的表名集合
        List<String> permittedTableNames =
                userModulePermissionMapper.selectPermittedTableNames(user.getId(), datasourceId);
        Set<String> permittedTableSet = new HashSet<>(permittedTableNames);

        // 检查SQL中的每个表名是否都在用户有权限的表名集合中
        for (String tableName : sqlTableNames) {
            if (!permittedTableSet.contains(tableName)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "没有访问表的权限: " + tableName);
            }
        }
    }

    /**
     * 获取用户有权限访问的所有表
     *
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @param conn 数据库连接
     * @return 有权限的表列表
     */
    public List<String> getPermittedTables(Long userId, Long datasourceId, Connection conn)
            throws SQLException {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取数据库中真实存在的表名
        Set<String> realTables = getRealTablesFromDatabase(conn);

        // 根据用户角色获取有权限的表名
        Set<String> allowedTables = getAllowedTablesByRole(user, userId, datasourceId);

        // 取交集：只返回既有权限又真实存在的表
        return allowedTables.stream()
                .filter(realTables::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取数据库中真实存在的表名
     *
     * @param conn 数据库连接
     * @return 真实表名集合
     */
    private Set<String> getRealTablesFromDatabase(Connection conn) throws SQLException {
        Set<String> realTables = new HashSet<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs =
                metaData.getTables(conn.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                realTables.add(rs.getString("TABLE_NAME"));
            }
        }
        return realTables;
    }

    /**
     * 根据用户角色获取有权限的表名
     *
     * @param user 用户信息
     * @param userId 用户ID
     * @param datasourceId 数据源ID
     * @return 有权限的表名集合
     */
    private Set<String> getAllowedTablesByRole(User user, Long userId, Long datasourceId) {
        String role = user.getRole();

        if (UserRole.SUPER_ADMIN.name().equals(role) || UserRole.ADMIN.name().equals(role)) {
            // 管理员和超级管理员：获取所有启用模块的表名
            List<String> enabledModuleTables =
                    moduleInfoMapper.selectEnabledModuleTableNames(datasourceId);
            return new HashSet<>(enabledModuleTables);
        } else {
            // 普通用户：通过联查用户模块权限表和模块信息表获取有权限的表名（已经过滤了禁用的模块）
            List<String> userPermittedTables =
                    userModulePermissionMapper.selectPermittedTableNames(userId, datasourceId);
            return new HashSet<>(userPermittedTables);
        }
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
