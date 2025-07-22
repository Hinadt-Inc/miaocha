package com.hinadt.miaocha.application.service.sql.processor;

import static com.hinadt.miaocha.application.service.sql.expression.SqlFragment.COUNT_ALIAS;
import static com.hinadt.miaocha.application.service.sql.expression.SqlFragment.TIME_ALIAS;
import static com.hinadt.miaocha.application.service.sql.expression.SqlFragment.TOTAL_ALIAS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.domain.dto.logsearch.FieldDistributionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogDetailResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogHistogramResultDTO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 查询结果处理器 专注于处理数据库查询返回的原始结果，将其转换为应用所需的数据结构 */
@Component
@Slf4j
public class ResultProcessor {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================================
    // 公共接口方法
    // ============================================================================

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
    public long processTotalCountResult(QueryResult queryResult) {
        long totalCount = 0L;

        if (queryResult != null && queryResult.hasData()) {
            Map<String, Object> firstRow = queryResult.getFirstRow();
            if (firstRow.containsKey(TOTAL_ALIAS)) {
                Object totalObj = firstRow.get(TOTAL_ALIAS);
                if (totalObj instanceof Number) {
                    totalCount = ((Number) totalObj).longValue();
                }
            }
        }

        return totalCount;
    }

    /**
     * 处理字段分布查询结果
     *
     * @param queryResult 结构化查询结果
     * @param result 字段分布结果DTO，用于填充分布数据和采样信息
     * @param originalFields 原始字段名列表
     */
    public void processFieldDistributionResult(
            QueryResult queryResult,
            LogFieldDistributionResultDTO result,
            List<String> originalFields) {

        // 1. 获取真实的采样总数
        long actualSampleCount = processTotalCountResult(queryResult);
        result.setActualSampleCount(actualSampleCount);

        // 2. 处理字段分布数据
        List<FieldDistributionDTO> fieldDistributions = new ArrayList<>();

        if (queryResult.hasData()) {
            Map<String, Object> row = queryResult.getFirstRow(); // TOPN查询只返回一行数据

            for (String field : originalFields) {
                String jsonValue = getFieldValue(row, field);

                if (jsonValue != null || row.containsKey(field)) {
                    FieldDistributionDTO dto = new FieldDistributionDTO();
                    dto.setFieldName(field);

                    // 解析JSON格式的TOPN结果
                    List<FieldDistributionDTO.ValueDistribution> valueDistributions;
                    if (jsonValue != null) {
                        valueDistributions = parseTopnJson(jsonValue, actualSampleCount);
                    } else {
                        valueDistributions = new ArrayList<>();
                    }
                    dto.setValueDistributions(valueDistributions);

                    fieldDistributions.add(dto);
                }
            }
        }

        result.setFieldDistributions(fieldDistributions);
    }

    // ============================================================================
    // 私有辅助方法
    // ============================================================================

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

    /**
     * 从行数据中获取count字段值
     *
     * @param row 行数据
     * @return count值
     */
    private long getCountValue(Map<String, Object> row) {
        if (row.containsKey(COUNT_ALIAS)) {
            Object countObj = row.get(COUNT_ALIAS);
            if (countObj instanceof Number) {
                return ((Number) countObj).longValue();
            }
        }
        return 0L;
    }

    /**
     * 获取字段值，支持多种列名格式
     *
     * @param row 行数据
     * @param field 字段名
     * @return 字段值
     */
    private String getFieldValue(Map<String, Object> row, String field) {
        // 优先尝试AS别名（直接使用原字段名）
        if (row.containsKey(field)) {
            return (String) row.get(field);
        }

        // 兼容旧格式：尝试TOPN函数格式的列名
        String topnColumnNameLower = "topn(" + field + ", 5)";
        String topnColumnNameUpper = "TOPN(" + field + ", 5)";

        if (row.containsKey(topnColumnNameLower)) {
            return (String) row.get(topnColumnNameLower);
        } else if (row.containsKey(topnColumnNameUpper)) {
            return (String) row.get(topnColumnNameUpper);
        }

        return null;
    }

    /**
     * 解析TOPN函数返回的JSON字符串 格式如：{"value1":count1,"value2":count2,...}
     *
     * @param jsonValue JSON字符串
     * @param sampleSize 采样总数，用于计算百分比
     * @return 值分布列表
     */
    private List<FieldDistributionDTO.ValueDistribution> parseTopnJson(
            String jsonValue, Long sampleSize) {
        List<FieldDistributionDTO.ValueDistribution> result = new ArrayList<>();

        if (jsonValue == null || jsonValue.trim().isEmpty()) {
            return result;
        }

        try {
            // 使用Jackson解析JSON
            Map<String, Integer> jsonMap =
                    objectMapper.readValue(jsonValue, new TypeReference<>() {});

            // 解析每个键值对
            for (Map.Entry<String, Integer> entry : jsonMap.entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();

                FieldDistributionDTO.ValueDistribution vd =
                        new FieldDistributionDTO.ValueDistribution();
                vd.setValue(key);
                vd.setCount(count);

                // 计算基于采样总数的百分比，保留2位小数
                double percentage = sampleSize > 0 ? (double) count / sampleSize * 100 : 0.0;
                vd.setPercentage(Math.round(percentage * 100) / 100.0);

                result.add(vd);
            }

        } catch (Exception e) {
            log.error("解析TOPN JSON失败: {}", jsonValue, e);
            // 返回空结果而不是抛出异常，保证系统稳定性
        }

        return result;
    }
}
