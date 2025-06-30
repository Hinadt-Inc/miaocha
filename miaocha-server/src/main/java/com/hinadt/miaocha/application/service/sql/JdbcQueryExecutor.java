package com.hinadt.miaocha.application.service.sql;

import com.hinadt.miaocha.application.service.datasource.HikariDatasourceManager;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.sql.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JdbcQueryExecutor {

    @Autowired private HikariDatasourceManager hikariDatasourceManager;

    /**
     * 执行SQL查询或更新操作
     *
     * @param datasourceInfo 数据源信息
     * @param sql SQL语句
     * @return SQL执行结果
     */
    public SqlQueryResultDTO executeQuery(DatasourceInfo datasourceInfo, String sql) {
        SqlQueryResultDTO result = new SqlQueryResultDTO();

        try (Connection conn = hikariDatasourceManager.getConnection(datasourceInfo);
                Statement stmt = conn.createStatement()) {

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
     * 获取数据库连接（供需要直接操作连接的场景使用）
     *
     * @param datasourceInfo 数据源信息
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     */
    public Connection getConnection(DatasourceInfo datasourceInfo) throws SQLException {
        return hikariDatasourceManager.getConnection(datasourceInfo);
    }

    /**
     * 执行SQL查询或更新操作（供需要直接传入连接的场景使用）
     *
     * @param conn 数据库连接
     * @param sql SQL语句
     * @return SQL执行结果
     * @throws SQLException 如果SQL执行出错
     */
    public SqlQueryResultDTO executeQuery(Connection conn, String sql) throws SQLException {
        SqlQueryResultDTO result = new SqlQueryResultDTO();

        try (Statement stmt = conn.createStatement()) {
            boolean isResultSet = stmt.execute(sql);

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    processResultSet(rs, result);
                }
            } else {
                result.setAffectedRows(stmt.getUpdateCount());
            }
        }

        return result;
    }

    /**
     * 执行结构化查询并返回结构化结果
     *
     * @param conn 数据库连接
     * @param sql SQL语句
     * @return 结构化查询结果
     * @throws SQLException 如果SQL执行出错
     */
    public QueryResult executeStructuredQuery(Connection conn, String sql) throws SQLException {
        QueryResult result = new QueryResult();
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

        result.setColumns(columns);
        result.setRows(rows);
        return result;
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
