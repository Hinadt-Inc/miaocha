package com.hinadt.miaocha.application.service.impl.logsearch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.impl.logsearch.template.LogSearchTemplate.SearchExecutor;
import com.hinadt.miaocha.application.service.impl.logsearch.template.SearchContext;
import com.hinadt.miaocha.application.service.sql.JdbcQueryExecutor;
import com.hinadt.miaocha.application.service.sql.builder.FieldDistributionSqlBuilder;
import com.hinadt.miaocha.application.service.sql.builder.LogSqlBuilder;
import com.hinadt.miaocha.application.service.sql.converter.LogSearchDTOConverter;
import com.hinadt.miaocha.application.service.sql.processor.QueryResult;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.exception.LogQueryException;
import com.hinadt.miaocha.domain.dto.FieldDistributionDTO;
import com.hinadt.miaocha.domain.dto.LogFieldDistributionResultDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 字段分布搜索执行器
 *
 * <p>专门处理字段分布查询逻辑，统一通过LogSqlBuilder构建SQL，支持并行查询优化
 */
@Component
@Slf4j
public class FieldDistributionSearchExecutor extends BaseSearchExecutor
        implements SearchExecutor<LogFieldDistributionResultDTO> {

    private final LogSqlBuilder logSqlBuilder;
    private final LogSearchDTOConverter dtoConverter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FieldDistributionSearchExecutor(
            JdbcQueryExecutor jdbcQueryExecutor,
            LogSqlBuilder logSqlBuilder,
            LogSearchDTOConverter dtoConverter,
            @Qualifier("logQueryExecutor") Executor logQueryExecutor) {
        super(jdbcQueryExecutor, logQueryExecutor);
        this.logSqlBuilder = logSqlBuilder;
        this.dtoConverter = dtoConverter;
    }

    @Override
    public LogFieldDistributionResultDTO execute(SearchContext context) throws LogQueryException {

        LogSearchDTO dto = context.getDto();
        String tableName = context.getTableName();
        Connection conn = context.getConnection();

        LogFieldDistributionResultDTO result = new LogFieldDistributionResultDTO();

        // 1. 转换fields中的点语法为括号语法（用于TOPN函数）
        List<String> convertedTopnFields =
                dto.getFields().stream()
                        .map(dtoConverter::convertTopnField)
                        .collect(java.util.stream.Collectors.toList());

        // 2. 构建字段分布查询SQL - 统一通过LogSqlBuilder
        String fieldDistributionSql =
                logSqlBuilder.buildFieldDistributionSql(
                        dto, tableName, convertedTopnFields, dto.getFields(), 5);

        log.debug("字段分布SQL: {}", fieldDistributionSql);

        // 3. 异步执行查询
        CompletableFuture<QueryResult> fieldDistributionFuture =
                executeQueryAsync(
                        conn,
                        fieldDistributionSql,
                        ErrorCode.LOG_FIELD_DISTRIBUTION_QUERY_FAILED,
                        "FieldDistributionQuery");

        try {
            // 等待查询完成
            QueryResult fieldDistributionResult = fieldDistributionFuture.get();

            // 4. 设置采样信息
            result.setActualSampleCount(FieldDistributionSqlBuilder.SAMPLE_SIZE);

            // 5. 处理结果
            List<FieldDistributionDTO> fieldDistributions =
                    processTopnResult(
                            fieldDistributionResult,
                            dto.getFields(),
                            FieldDistributionSqlBuilder.SAMPLE_SIZE);
            result.setFieldDistributions(fieldDistributions);

            return result;
        } catch (ExecutionException | InterruptedException e) {
            // 其他异常（超时、中断等）让外层处理，转为BusinessException
            throw new RuntimeException(e);
        }
    }

    /** 处理TOPN查询结果，转换为FieldDistributionDTO列表 */
    private List<FieldDistributionDTO> processTopnResult(
            QueryResult queryResult, List<String> fields, Integer sampleSize) {
        List<FieldDistributionDTO> result = new ArrayList<>();

        if (queryResult.hasData()) {
            Map<String, Object> row = queryResult.getFirstRow(); // TOPN查询只返回一行数据

            for (String field : fields) {
                String jsonValue = getFieldValue(row, field);

                if (jsonValue != null || row.containsKey(field)) {
                    FieldDistributionDTO dto = new FieldDistributionDTO();
                    dto.setFieldName(field);

                    // 解析JSON格式的TOPN结果
                    List<FieldDistributionDTO.ValueDistribution> valueDistributions;
                    if (jsonValue != null) {
                        valueDistributions = parseTopnJson(jsonValue, sampleSize);
                    } else {
                        valueDistributions = new ArrayList<>();
                    }
                    dto.setValueDistributions(valueDistributions);

                    result.add(dto);
                }
            }
        }

        return result;
    }

    /** 获取字段值，支持多种列名格式 */
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

    /** 解析TOPN函数返回的JSON字符串 格式如：{"value1":count1,"value2":count2,...} */
    private List<FieldDistributionDTO.ValueDistribution> parseTopnJson(
            String jsonValue, Integer sampleSize) {
        List<FieldDistributionDTO.ValueDistribution> result = new ArrayList<>();

        if (jsonValue == null || jsonValue.trim().isEmpty()) {
            return result;
        }

        try {
            // 使用Jackson解析JSON
            Map<String, Integer> jsonMap =
                    objectMapper.readValue(jsonValue, new TypeReference<Map<String, Integer>>() {});

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
