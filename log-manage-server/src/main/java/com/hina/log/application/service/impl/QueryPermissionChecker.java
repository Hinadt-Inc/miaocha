package com.hina.log.application.service.impl;

import com.hina.log.domain.entity.User;
import com.hina.log.domain.entity.enums.UserRole;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.application.service.ModulePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 查询权限检查器
 */
@Component
@RequiredArgsConstructor
public class QueryPermissionChecker {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);

    // 修改表名提取正则表达式，处理多种SQL语法情况
    private static final Pattern FROM_PATTERN = Pattern.compile(
            "\\bFROM\\s+((\"[^\"]+\"|'[^']+'|`[^`]+`|\\[[^\\]]+\\]|[\\w\\d_\\.]+)\\s*(,\\s*(\"[^\"]+\"|'[^']+'|`[^`]+`|\\[[^\\]]+\\]|[\\w\\d_\\.]+))*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "\\b(JOIN)\\s+(\"[^\"]+\"|'[^']+'|`[^`]+`|\\[[^\\]]+\\]|[\\w\\d_\\.]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "(\"([^\"]+)\"|'([^']+)'|`([^`]+)`|\\[([^\\]]+)\\]|([\\w\\d_\\.]+))",
            Pattern.CASE_INSENSITIVE);

    private final ModulePermissionService modulePermissionService;

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

        // 提取所有表名并检查权限
        Set<String> tableNames = extractTableNames(sql);
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
     * @param conn   数据库连接
     * @return 有权限的表列表
     */
    public List<String> getPermittedTables(Long userId, Connection conn) throws SQLException {
        List<String> permittedTables = new ArrayList<>();

        // 获取用户可访问的所有模块
        List<String> permittedModules = modulePermissionService.getUserAccessibleModules(userId).stream()
                .flatMap(structure -> structure.getModules().stream())
                .map(moduleInfo -> moduleInfo.getModuleName())
                .collect(Collectors.toList());

        // 获取所有表
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
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
        return SELECT_PATTERN.matcher(sql.trim()).find();
    }

    /**
     * 从SQL查询中提取所有表名
     *
     * @param sql SQL查询语句
     * @return 表名集合
     */
    private Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();

        // 提取FROM子句中的表名
        Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            String fromClause = fromMatcher.group(1);
            extractTablesFromClause(fromClause, tableNames);
        }

        // 提取JOIN子句中的表名
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            String tablePart = joinMatcher.group(2);
            addTableName(tablePart, tableNames);
        }

        return tableNames;
    }

    /**
     * 从子句中提取表名
     *
     * @param clause     子句
     * @param tableNames 表名集合
     */
    private void extractTablesFromClause(String clause, Set<String> tableNames) {
        String[] parts = clause.split(",");
        for (String part : parts) {
            // 处理表名部分，移除可能的别名
            String tablePart = part.trim().split("\\s+")[0];
            addTableName(tablePart, tableNames);
        }
    }

    /**
     * 将表名添加到集合
     *
     * @param tablePart  表名部分
     * @param tableNames 表名集合
     */
    private void addTableName(String tablePart, Set<String> tableNames) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(tablePart);
        if (matcher.find()) {
            // 尝试获取各种引号包裹的表名
            String tableName = matcher.group(2); // 双引号
            if (tableName == null)
                tableName = matcher.group(3); // 单引号
            if (tableName == null)
                tableName = matcher.group(4); // 反引号
            if (tableName == null)
                tableName = matcher.group(5); // 方括号
            if (tableName == null)
                tableName = matcher.group(6); // 无引号

            if (tableName != null) {
                // 处理模式名称和表名
                String[] schemaAndTable = tableName.split("\\.");
                tableName = schemaAndTable.length > 1 ? schemaAndTable[1] : schemaAndTable[0];
                tableNames.add(tableName);
            }
        }
    }
}