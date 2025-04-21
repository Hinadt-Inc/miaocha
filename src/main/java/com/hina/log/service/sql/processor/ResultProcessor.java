package com.hina.log.service.sql.processor;

import com.hina.log.dto.LogSearchResultDTO;
import com.hina.log.service.sql.JdbcQueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询结果处理器
 */
@Component
public class ResultProcessor {

    @Autowired
    private JdbcQueryExecutor jdbcQueryExecutor;

    /**
     * 处理日志分布统计查询结果
     */
    public void processDistributionResult(Connection conn, String sql, LogSearchResultDTO result) throws SQLException {
        List<LogSearchResultDTO.LogDistributionData> distributionData = new ArrayList<>();

        Map<String, Object> queryResult = jdbcQueryExecutor.executeRawQuery(conn, sql);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");

        for (Map<String, Object> row : rows) {
            LogSearchResultDTO.LogDistributionData data = new LogSearchResultDTO.LogDistributionData();
            data.setTimePoint(row.get("log_time_").toString());
            data.setCount(Long.valueOf(row.get("count").toString()));
            distributionData.add(data);
        }

        result.setDistributionData(distributionData);
    }

    /**
     * 处理详细日志查询结果
     */
    public void processDetailResult(Connection conn, String sql, LogSearchResultDTO result) throws SQLException {
        Map<String, Object> queryResult = jdbcQueryExecutor.executeRawQuery(conn, sql);
        result.setColumns((List<String>) queryResult.get("columns"));
        result.setRows((List<Map<String, Object>>) queryResult.get("rows"));
    }
}