package com.hinadt.miaocha.application.service.sql.processor;

import static com.hinadt.miaocha.application.service.sql.builder.SqlFragment.TIME_ALIAS;

import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogHistogramResultDTO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 查询结果处理器 专注于处理数据库查询返回的原始结果，将其转换为应用所需的数据结构 */
@Component
public class ResultProcessor {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 处理日志分布统计查询结果 注意：保持从数据库查询结果中获取的时间倒序排序（即从最新日期到最旧日期）
     *
     * @param queryResult 结构化查询结果
     * @param result 日志时间分布结果DTO，用于填充分布数据
     */
    public void processDistributionResult(QueryResult queryResult, LogHistogramResultDTO result) {
        List<LogHistogramResultDTO.LogDistributionData> distributionData = new ArrayList<>();

        if (queryResult.hasData()) {
            // 直接使用数据库返回的顺序，因为SQL已经按TIME_ALIAS升序排序
            for (Map<String, Object> row : queryResult.getRows()) {
                LogHistogramResultDTO.LogDistributionData data =
                        new LogHistogramResultDTO.LogDistributionData();

                if (row.containsKey(TIME_ALIAS)) {
                    String timePoint = formatTimePoint(row.get(TIME_ALIAS));
                    data.setTimePoint(timePoint);
                }

                data.setCount(getCountValue(row));
                distributionData.add(data);
            }
        }

        result.setDistributionData(distributionData);
    }

    /**
     * 处理详细日志查询结果
     *
     * @param queryResult 结构化查询结果
     * @param result 日志明细结果DTO，用于填充列名和数据行
     */
    public void processDetailResult(QueryResult queryResult, LogDetailResultDTO result) {
        if (queryResult != null) {
            result.setColumns(queryResult.getColumns());
            result.setRows(queryResult.getRows());
        }
    }

    /**
     * 处理总数查询结果
     *
     * @param queryResult 结构化查询结果
     * @return 总数值
     */
    public int processTotalCountResult(QueryResult queryResult) {
        int totalCount = 0;

        if (queryResult != null && queryResult.hasData()) {
            Map<String, Object> firstRow = queryResult.getFirstRow();
            if (firstRow.containsKey("total")) {
                Object totalObj = firstRow.get("total");
                if (totalObj instanceof Number) {
                    totalCount = ((Number) totalObj).intValue();
                }
            }
        }

        return totalCount;
    }

    /**
     * 格式化时间点，支持毫秒时间戳和标准时间字符串
     *
     * @param timePointObj 时间点对象，可能是毫秒时间戳(Long)或时间字符串
     * @return 格式化后的时间字符串
     */
    private String formatTimePoint(Object timePointObj) {
        if (timePointObj == null) {
            return "";
        }

        // 如果是Number类型，直接转换
        if (timePointObj instanceof Number) {
            long millisTimestamp = ((Number) timePointObj).longValue();
            Instant instant = Instant.ofEpochMilli(millisTimestamp);
            return instant.atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER);
        }

        String timeStr = timePointObj.toString();

        // 检查是否为数字格式（包括科学计数法）
        if (timeStr.matches("\\d+") || timeStr.matches("\\d+\\.\\d+E[+-]?\\d+")) {
            try {
                // 处理科学计数法和普通数字
                double doubleValue = Double.parseDouble(timeStr);
                long millisTimestamp = (long) doubleValue;
                // 将毫秒时间戳转换为可读时间格式
                Instant instant = Instant.ofEpochMilli(millisTimestamp);
                return instant.atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER);
            } catch (NumberFormatException e) {
                // 如果转换失败，直接返回原字符串
                return timeStr;
            }
        }

        // 非数字字符串，直接返回（已经是时间格式）
        return timeStr;
    }

    private long getCountValue(Map<String, Object> row) {
        if (row.containsKey("count")) {
            Object countObj = row.get("count");
            if (countObj instanceof Number) {
                return ((Number) countObj).longValue();
            }
        }
        return 0L;
    }
}
