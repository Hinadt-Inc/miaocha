package com.hinadt.miaocha.application.service.database;

import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.entity.enums.DatasourceType;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Doris数据库元数据服务实现 专门处理Doris的特有功能，如variant类型的列展开 */
@Component
public class DorisMetadataService implements DatabaseMetadataService {

    private static final String GET_TABLE_COMMENT_SQL =
            "SELECT table_comment FROM information_schema.tables WHERE table_name = ?";

    // 直接查询Doris的DESC命令来获取真实的列信息，包括variant展开的列
    private static final String DESC_TABLE_SQL = "DESC `%s`";

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
            return "";
        }
        return "";
    }

    @Override
    public List<SchemaInfoDTO.ColumnInfoDTO> getColumnInfo(Connection connection, String tableName)
            throws SQLException {
        // 使用DESC命令获取Doris真实的列信息（包括variant展开的列）
        List<SchemaInfoDTO.ColumnInfoDTO> dorisColumns = getDorisDescColumns(connection, tableName);

        // 如果DESC命令失败，回退到标准JDBC方式
        if (dorisColumns.isEmpty()) {
            return getStandardColumnInfo(connection, tableName);
        }

        return dorisColumns;
    }

    /** 使用标准JDBC方式获取列信息 */
    private List<SchemaInfoDTO.ColumnInfoDTO> getStandardColumnInfo(
            Connection connection, String tableName) throws SQLException {
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

    /** 使用DESC命令获取Doris真实的列信息（包括variant展开的列） */
    private List<SchemaInfoDTO.ColumnInfoDTO> getDorisDescColumns(
            Connection connection, String tableName) {
        List<SchemaInfoDTO.ColumnInfoDTO> columns = new ArrayList<>();

        try {
            String sql = String.format(DESC_TABLE_SQL, tableName);

            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    SchemaInfoDTO.ColumnInfoDTO column = new SchemaInfoDTO.ColumnInfoDTO();

                    String field = rs.getString("Field");
                    String type = rs.getString("Type");
                    String nullFlag = rs.getString("Null");
                    String key = rs.getString("Key");

                    column.setColumnName(field);
                    column.setDataType(type);
                    column.setIsPrimaryKey("true".equals(key)); // Doris DESC返回"true"/"false"
                    column.setIsNullable("Yes".equalsIgnoreCase(nullFlag)); // "Yes"表示可空

                    columns.add(column);
                }
            }
        } catch (SQLException e) {
            // DESC命令执行失败时返回空列表，会回退到标准JDBC方式
        }

        return columns;
    }

    /** 获取主键信息 */
    private Set<String> getPrimaryKeys(Connection connection, String tableName) {
        Set<String> primaryKeys = new HashSet<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(connection.getCatalog(), null, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            System.err.println("获取主键信息失败: " + e.getMessage());
        }
        return primaryKeys;
    }

    @Override
    public String getSupportedDatabaseType() {
        return DatasourceType.DORIS.getType();
    }
}
