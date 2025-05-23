package com.hina.log.application.service.database;

import com.hina.log.domain.dto.SchemaInfoDTO;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL数据库元数据服务实现
 */
@Component
public class PostgreSqlMetadataService implements DatabaseMetadataService {

    private static final String GET_TABLE_COMMENT_SQL = "SELECT obj_description(to_regclass(?)::oid) as table_comment";

    @Override
    public List<String> getAllTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet rs = metaData.getTables(connection.getCatalog(), "public", "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        return tables;
    }

    @Override
    public String getTableComment(Connection connection, String tableName) {
        try (PreparedStatement pstmt = connection.prepareStatement(GET_TABLE_COMMENT_SQL)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String comment = rs.getString(1);
                    return comment != null ? comment : "";
                }
            }
        } catch (SQLException e) {
            // 获取注释失败时不抛出异常，返回空字符串
            return "";
        }
        return "";
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getColumnInfo(Connection connection, String tableName)
            throws SQLException {
        List<SchemaInfoDTO.ColumnInfoDTO> columns = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        // 获取主键信息
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(connection.getCatalog(), "public", tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        // 获取列信息 - 在PostgreSQL中，列注释需要特殊处理
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), "public", tableName, "%")) {
            while (rs.next()) {
                SchemaInfoDTO.ColumnInfoDTO column = new SchemaInfoDTO.ColumnInfoDTO();
                String columnName = rs.getString("COLUMN_NAME");
                column.setColumnName(columnName);
                column.setDataType(rs.getString("TYPE_NAME"));

                // 由于JDBC驱动可能无法正确获取列注释，这里通过额外的SQL获取
                column.setColumnComment(getColumnComment(connection, tableName, columnName));

                column.setIsPrimaryKey(primaryKeys.contains(columnName));
                column.setIsNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                columns.add(column);
            }
        }

        return columns;
    }

    /**
     * 获取PostgreSQL列注释
     */
    private String getColumnComment(Connection connection, String tableName, String columnName) {
        String sql = "SELECT col_description(a.attrelid, a.attnum) " +
                "FROM pg_catalog.pg_attribute a " +
                "WHERE a.attrelid = to_regclass(?)::oid " +
                "AND a.attname = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, columnName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String comment = rs.getString(1);
                    return comment != null ? comment : "";
                }
            }
        } catch (SQLException e) {
            return "";
        }
        return "";
    }

    @Override
    public String getSupportedDatabaseType() {
        return "postgresql";
    }
}