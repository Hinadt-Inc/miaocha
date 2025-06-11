package com.hinadt.miaocha.application.service.database;

import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** MySQL/MariaDB数据库元数据服务实现 */
@Component
public class MySqlMetadataService implements DatabaseMetadataService {

    private static final String GET_TABLE_COMMENT_SQL =
            "SELECT table_comment FROM information_schema.tables WHERE table_name = ?";

    @Override
    public List<String> getAllTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet rs =
                metaData.getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
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
                    return rs.getString(1);
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
        try (ResultSet rs = metaData.getPrimaryKeys(connection.getCatalog(), null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        // 获取列信息
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, "%")) {
            while (rs.next()) {
                SchemaInfoDTO.ColumnInfoDTO column = new SchemaInfoDTO.ColumnInfoDTO();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
                column.setIsNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                columns.add(column);
            }
        }

        return columns;
    }

    @Override
    public String getSupportedDatabaseType() {
        return DatasourceType.MYSQL.getType();
    }
}
