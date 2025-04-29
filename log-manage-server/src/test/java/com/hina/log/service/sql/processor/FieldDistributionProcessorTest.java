package com.hina.log.service.sql.processor;

import com.hina.log.dto.FieldDistributionDTO;
import com.hina.log.dto.LogSearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

class FieldDistributionProcessorTest {

    private FieldDistributionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FieldDistributionProcessor();
        ReflectionTestUtils.setField(processor, "fieldStatisticsExecutor", ForkJoinPool.commonPool());
    }

    @Test
    void processFieldDistributionsAsync_withValidData_shouldCalculateCorrectly() throws ExecutionException, InterruptedException {
        // 准备测试数据
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setSuccess(true);

        List<String> columns = Arrays.asList("level", "service", "message");
        result.setColumns(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        // 添加测试数据行
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("level", i < 5 ? "ERROR" : (i < 8 ? "WARN" : "INFO"));
            row.put("service", i < 3 ? "user-service" : (i < 7 ? "order-service" : "payment-service"));
            row.put("message", "Message " + i);
            rows.add(row);
        }
        result.setRows(rows);

        // 执行测试
        CompletableFuture<Void> future = processor.processFieldDistributionsAsync(result);
        future.get(); // 等待异步处理完成

        // 验证结果
        assertNotNull(result.getFieldDistributions());
        assertEquals(3, result.getFieldDistributions().size());

        // 验证level字段的分布
        FieldDistributionDTO levelDistribution = result.getFieldDistributions().stream()
                .filter(d -> "level".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(levelDistribution);
        assertEquals(10, levelDistribution.getTotalCount());
        assertEquals(0, levelDistribution.getNullCount());
        assertEquals(10, levelDistribution.getNonNullCount());
        assertEquals(3, levelDistribution.getUniqueValueCount());

        List<FieldDistributionDTO.ValueDistribution> levelValues = levelDistribution.getValueDistributions();
        assertEquals(3, levelValues.size());

        // 验证ERROR是最多的
        FieldDistributionDTO.ValueDistribution errorValue = levelValues.get(0);
        assertEquals("ERROR", errorValue.getValue());
        assertEquals(5, errorValue.getCount());
        assertEquals(50.0, errorValue.getPercentage());

        // 验证service字段的分布
        FieldDistributionDTO serviceDistribution = result.getFieldDistributions().stream()
                .filter(d -> "service".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(serviceDistribution);
        assertEquals(3, serviceDistribution.getUniqueValueCount());

        // 验证order-service是最多的
        FieldDistributionDTO.ValueDistribution orderServiceValue = serviceDistribution.getValueDistributions().get(0);
        assertEquals("order-service", orderServiceValue.getValue());
        assertEquals(4, orderServiceValue.getCount());
        assertEquals(40.0, orderServiceValue.getPercentage());
    }

    @Test
    void processFieldDistributionsAsync_withNullValues_shouldHandleCorrectly() throws ExecutionException, InterruptedException {
        // 准备测试数据
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setSuccess(true);

        List<String> columns = Arrays.asList("level", "service");
        result.setColumns(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        // 添加测试数据行，包含null值
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("level", i < 5 ? "ERROR" : null);
            row.put("service", i < 3 ? "user-service" : (i < 7 ? null : "payment-service"));
            rows.add(row);
        }
        result.setRows(rows);

        // 执行测试
        CompletableFuture<Void> future = processor.processFieldDistributionsAsync(result);
        future.get(); // 等待异步处理完成

        // 验证结果
        assertNotNull(result.getFieldDistributions());

        // 验证level字段的分布
        FieldDistributionDTO levelDistribution = result.getFieldDistributions().stream()
                .filter(d -> "level".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(levelDistribution);
        assertEquals(10, levelDistribution.getTotalCount());
        assertEquals(5, levelDistribution.getNullCount());
        assertEquals(5, levelDistribution.getNonNullCount());

        // 验证service字段的分布
        FieldDistributionDTO serviceDistribution = result.getFieldDistributions().stream()
                .filter(d -> "service".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(serviceDistribution);
        assertEquals(10, serviceDistribution.getTotalCount());
        assertEquals(4, serviceDistribution.getNullCount());
        assertEquals(6, serviceDistribution.getNonNullCount());
    }

    @Test
    void processFieldDistributionsAsync_withEmptyResult_shouldReturnEmptyList() throws ExecutionException, InterruptedException {
        // 准备空结果
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setSuccess(true);
        result.setColumns(Collections.emptyList());
        result.setRows(Collections.emptyList());

        // 执行测试
        CompletableFuture<Void> future = processor.processFieldDistributionsAsync(result);
        future.get(); // 等待异步处理完成

        // 验证结果
        assertNotNull(result.getFieldDistributions());
        assertTrue(result.getFieldDistributions().isEmpty());
    }

    @Test
    void processFieldDistributionsAsync_withFailedResult_shouldReturnEmptyList() throws ExecutionException, InterruptedException {
        // 准备失败结果
        LogSearchResultDTO result = new LogSearchResultDTO();
        result.setSuccess(false);

        // 执行测试
        CompletableFuture<Void> future = processor.processFieldDistributionsAsync(result);
        future.get(); // 等待异步处理完成

        // 验证结果
        assertNotNull(result.getFieldDistributions());
        assertTrue(result.getFieldDistributions().isEmpty());
    }
}
