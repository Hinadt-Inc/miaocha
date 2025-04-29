package com.hina.log.service.sql.processor;

import com.hina.log.dto.FieldDistributionDTO;
import com.hina.log.dto.LogSearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * 字段分布统计处理器
 * 负责计算日志字段的数据分布统计信息
 */
@Component
public class FieldDistributionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FieldDistributionProcessor.class);

    // 默认统计前5个最多的值
    private static final int TOP_N = 5;

    @Autowired(required = false)
    @Qualifier("fieldStatisticsExecutor")
    private Executor fieldStatisticsExecutor;

    /**
     * 获取线程执行器，如果注入的执行器为空（如在测试环境中），则使用公共线程池
     */
    private Executor getExecutor() {
        return fieldStatisticsExecutor != null ? fieldStatisticsExecutor : ForkJoinPool.commonPool();
    }

    /**
     * 异步计算字段分布统计信息
     *
     * @param result 日志搜索结果DTO
     * @return 完成的Future
     */
    public CompletableFuture<Void> processFieldDistributionsAsync(LogSearchResultDTO result) {
        if (result == null || !result.getSuccess() || result.getRows() == null || result.getRows().isEmpty()
                || result.getColumns() == null || result.getColumns().isEmpty()) {
            // 如果结果为空或无效，直接返回空的分布统计
            result.setFieldDistributions(Collections.emptyList());
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                List<FieldDistributionDTO> fieldDistributions = calculateFieldDistributions(result.getColumns(), result.getRows());
                result.setFieldDistributions(fieldDistributions);
            } catch (Exception e) {
                logger.error("计算字段分布统计信息时发生错误", e);
                // 出错时设置为空列表，不影响主要结果返回
                result.setFieldDistributions(Collections.emptyList());
            }
        }, getExecutor());
    }

    /**
     * 计算字段分布统计信息
     *
     * @param columns 列名列表
     * @param rows 数据行列表
     * @return 字段分布统计信息列表
     */
    private List<FieldDistributionDTO> calculateFieldDistributions(List<String> columns, List<Map<String, Object>> rows) {
        // 使用并发HashMap来统计每个字段的值分布
        Map<String, Map<Object, Integer>> fieldValueCounts = new ConcurrentHashMap<>();

        // 初始化每个字段的值计数映射
        for (String column : columns) {
            fieldValueCounts.put(column, new ConcurrentHashMap<>());
        }

        // 统计每个字段的值分布
        for (Map<String, Object> row : rows) {
            for (String column : columns) {
                Object value = row.get(column);
                Map<Object, Integer> valueCounts = fieldValueCounts.get(column);

                // 处理null值
                if (value == null) {
                    value = "NULL";
                } else if (value instanceof String && ((String) value).isEmpty()) {
                    value = "(空字符串)";
                }

                // 更新计数
                valueCounts.compute(value, (k, v) -> (v == null) ? 1 : v + 1);
            }
        }

        // 转换为DTO对象
        List<FieldDistributionDTO> result = new ArrayList<>();
        int totalRows = rows.size();

        for (String column : columns) {
            FieldDistributionDTO dto = new FieldDistributionDTO();
            dto.setFieldName(column);

            Map<Object, Integer> valueCounts = fieldValueCounts.get(column);
            int nullCount = valueCounts.getOrDefault("NULL", 0);
            int nonNullCount = totalRows - nullCount;

            dto.setTotalCount(totalRows);
            dto.setNullCount(nullCount);
            dto.setNonNullCount(nonNullCount);
            dto.setUniqueValueCount(valueCounts.size());

            // 计算Top N的值分布
            List<FieldDistributionDTO.ValueDistribution> valueDistributions = valueCounts.entrySet().stream()
                    .sorted(Map.Entry.<Object, Integer>comparingByValue().reversed())
                    .limit(TOP_N)
                    .map(entry -> {
                        FieldDistributionDTO.ValueDistribution vd = new FieldDistributionDTO.ValueDistribution();
                        vd.setValue(entry.getKey());
                        vd.setCount(entry.getValue());
                        // 计算百分比，保留2位小数
                        double percentage = (double) entry.getValue() / totalRows * 100;
                        vd.setPercentage(Math.round(percentage * 100) / 100.0);
                        return vd;
                    })
                    .collect(Collectors.toList());

            dto.setValueDistributions(valueDistributions);
            result.add(dto);
        }

        return result;
    }
}
