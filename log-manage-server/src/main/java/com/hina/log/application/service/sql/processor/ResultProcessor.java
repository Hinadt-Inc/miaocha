package com.hina.log.application.service.sql.processor;

import com.hina.log.domain.dto.LogDetailResultDTO;
import com.hina.log.domain.dto.LogHistogramResultDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 查询结果处理器 专注于处理数据库查询返回的原始结果，将其转换为应用所需的数据结构 */
@Component
public class ResultProcessor {

    /**
     * 处理日志分布统计查询结果 注意：保持从数据库查询结果中获取的时间倒序排序（即从最新日期到最旧日期）
     *
     * @param queryResult 查询返回的原始结果
     * @param result 日志时间分布结果DTO，用于填充分布数据
     */
    public void processDistributionResult(
            Map<String, Object> queryResult, LogHistogramResultDTO result) {
        List<LogHistogramResultDTO.LogDistributionData> distributionData = new ArrayList<>();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");

        if (rows != null) {
            // 直接使用数据库返回的顺序，因为SQL已经按log_time_升序排序
            for (Map<String, Object> row : rows) {
                LogHistogramResultDTO.LogDistributionData data =
                        new LogHistogramResultDTO.LogDistributionData();
                if (row.containsKey("log_time_")) {
                    data.setTimePoint(row.get("log_time_").toString());
                }
                if (row.containsKey("count")) {
                    Object countObj = row.get("count");
                    data.setCount(
                            countObj instanceof Number ? ((Number) countObj).longValue() : 0L);
                }
                distributionData.add(data);
            }
        }

        result.setDistributionData(distributionData);
    }

    /**
     * 处理详细日志查询结果
     *
     * @param queryResult 查询返回的原始结果
     * @param result 日志明细结果DTO，用于填充列名和数据行
     */
    public void processDetailResult(Map<String, Object> queryResult, LogDetailResultDTO result) {
        if (queryResult != null) {
            result.setColumns((List<String>) queryResult.get("columns"));
            result.setRows((List<Map<String, Object>>) queryResult.get("rows"));
        }
    }

    /**
     * 处理总数查询结果
     *
     * @param queryResult 查询返回的原始结果
     * @return 总数值
     */
    public int processTotalCountResult(Map<String, Object> queryResult) {
        int totalCount = 0;

        if (queryResult != null) {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");

            if (rows != null && !rows.isEmpty() && rows.get(0).containsKey("total")) {
                Object totalObj = rows.get(0).get("total");
                if (totalObj instanceof Number) {
                    totalCount = ((Number) totalObj).intValue();
                }
            }
        }

        return totalCount;
    }
}
