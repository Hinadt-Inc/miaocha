package com.hina.log.service.sql;

import com.hina.log.dto.SqlQueryResultDTO;
import com.hina.log.entity.Datasource;
import com.hina.log.entity.enums.DatasourceType;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
public class JdbcQueryExecutor {

    public SqlQueryResultDTO executeQuery(Datasource datasource, String sql) {
        SqlQueryResultDTO result = new SqlQueryResultDTO();

        try (Connection conn = getConnection(datasource)) {
            Statement stmt = conn.createStatement();
            boolean isResultSet = stmt.execute(sql);

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    processResultSet(rs, result);
                }
            } else {
                result.setAffectedRows(stmt.getUpdateCount());
            }
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SQL执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行原始SQL查询并返回结果
     *
     * @param conn 数据库连接
     * @param sql  SQL语句
     * @return 查询结果，包含列名和行数据
     * @throws SQLException 如果SQL执行出错
     */
    public Map<String, Object> executeRawQuery(Connection conn, String sql) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取列名
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            // 获取行数据
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        }

        result.put("columns", columns);
        result.put("rows", rows);
        return result;
    }

    public Connection getConnection(Datasource datasource) throws SQLException {
        DatasourceType datasourceType = DatasourceType.fromType(datasource.getType());
        if (datasourceType == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_TYPE_NOT_SUPPORTED);
        }

        String url = datasourceType.buildJdbcUrl(
                datasource.getIp(),
                datasource.getPort(),
                datasource.getDatabase(),
                datasource.getJdbcParams());

        return DriverManager.getConnection(
                url,
                datasource.getUsername(),
                datasource.getPassword());
    }

    private void processResultSet(ResultSet rs, SqlQueryResultDTO result) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            rows.add(row);
        }

        result.setColumns(columns);
        result.setRows(rows);
    }
}